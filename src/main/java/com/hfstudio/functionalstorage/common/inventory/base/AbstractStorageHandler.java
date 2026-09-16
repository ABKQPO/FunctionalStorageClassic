package com.hfstudio.functionalstorage.common.inventory.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageChangeDispatcher;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/**
 * Generic fixed-size long-capacity storage over one {@link StorageResource}
 * kind. Mutable game objects never escape this class: every state transition
 * replaces an immutable internal slot value and every public read returns a
 * detached snapshot.
 *
 * @param <S> concrete snapshot type
 * @param <K> immutable resource key type
 */
public abstract class AbstractStorageHandler<S extends StorageSnapshot<S, K>, K extends StorageKey>
    implements IStorageHandler<S, K> {

    private static final String ENTRIES = "Entries";
    private static final String INDEX = "Index";
    private static final String AMOUNT = "Amount";
    private static final String TEMPLATE = "Template";

    private final StorageResource<S, K> resource;
    private final S[] templates;
    private final long[] amounts;
    private final StorageChangeDispatcher<S, K> changeDispatcher = new StorageChangeDispatcher<>();

    @SuppressWarnings("unchecked")
    protected AbstractStorageHandler(@Nonnull StorageResource<S, K> resource, int slots) {
        this.resource = Objects.requireNonNull(resource, "resource");
        int count = Math.max(0, slots);
        this.templates = (S[]) new StorageSnapshot[count];
        this.amounts = new long[count];
        Arrays.fill(this.templates, resource.empty());
    }

    @Nonnull
    protected final StorageResource<S, K> getResource() {
        return resource;
    }

    @Override
    public final int getStorageCount() {
        return templates.length;
    }

    @Nonnull
    @Override
    public final S getSnapshot(int index) {
        if (!isValidIndex(index)) {
            return resource.empty();
        }
        return configuredAt(index) ? templates[index].withAmount(isCreative() ? Long.MAX_VALUE : amounts[index])
            : resource.empty();
    }

    @Override
    public final long getCapacity(int index) {
        if (!isValidIndex(index)) {
            return 0L;
        }
        return capacityOf(configuredAt(index) ? templates[index] : resource.empty());
    }

    @Nonnull
    @Override
    public final TransferResult<S, K> insert(int index, @Nonnull S request, @Nonnull StorageAction action) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(action, "action");

        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L || !isValidIndex(index) || !isOperationEnabled()) {
            return emptyResult(requested, action);
        }

        S incoming = resource.templateOf(request);
        if (!resource.hasTemplate(incoming)) {
            return emptyResult(requested, action);
        }

        boolean configured = configuredAt(index);
        if (!configured) {
            if (isLocked()) {
                return emptyResult(requested, action);
            }
        } else if (!isCompatible(templates[index], incoming)) {
            return emptyResult(requested, action);
        }

        if (isCreative()) {
            if (action == StorageAction.EXECUTE && !configured) {
                setSlot(index, incoming, Long.MAX_VALUE);
            }
            return processedResult(request, requested, action);
        }

        S capacityTemplate = configured ? templates[index] : incoming;
        long capacity = capacityOf(capacityTemplate);
        long insertable = amounts[index] >= capacity ? 0L : capacity - amounts[index];
        long inserted = Math.min(requested, insertable);
        long processed = voidsOverflow() ? requested : inserted;

        if (action == StorageAction.EXECUTE && inserted > 0L) {
            setSlot(index, capacityTemplate, saturatedAdd(amounts[index], inserted));
        }
        return processedResult(request, processed, action);
    }

    @Nonnull
    @Override
    public final TransferResult<S, K> extract(int index, long amount, @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");

        long requested = Math.max(0L, amount);
        if (requested == 0L || !isValidIndex(index) || !configuredAt(index) || !isOperationEnabled()) {
            return emptyResult(requested, action);
        }

        S template = templates[index];
        long extracted = isCreative() ? requested : Math.min(requested, amounts[index]);
        if (extracted == 0L) {
            return emptyResult(requested, action);
        }

        if (action == StorageAction.EXECUTE && !isCreative()) {
            long remaining = amounts[index] - extracted;
            setSlot(index, remaining == 0L && !isLocked() ? resource.empty() : template, remaining);
        }
        return new TransferResult<>(requested, template.withAmount(extracted), action);
    }

    /**
     * Installs or clears a retained slot filter. A populated slot can only keep
     * its existing compatible type.
     *
     * @param index  target index
     * @param filter desired template, or an empty snapshot to clear
     * @return whether the requested filter is valid for the current contents
     */
    public final boolean setSlotFilter(int index, @Nonnull S filter) {
        if (!isValidIndex(index)) {
            return false;
        }
        Objects.requireNonNull(filter, "filter");
        S normalized = resource.hasTemplate(filter) ? resource.templateOf(filter) : resource.empty();
        if (amounts[index] > 0L && (!resource.hasTemplate(normalized) || !isCompatible(templates[index], normalized))) {
            return false;
        }
        S replacement = amounts[index] > 0L ? templates[index] : normalized;
        if (sameSnapshot(templates[index], replacement)) {
            return true;
        }
        setSlot(index, replacement, amounts[index]);
        return true;
    }

    /**
     * Applies filter retention for a user-visible lock transition and emits
     * exactly one full-resynchronization event. Callers must invoke this only
     * after the external lock flag actually changed.
     *
     * @param locked new lock state
     */
    public final void applyLockConfiguration(boolean locked) {
        if (!locked) {
            List<StorageChange.Entry<S, K>> entries = new ArrayList<>();
            for (int index = 0; index < templates.length; index++) {
                if (amounts[index] == 0L && configuredAt(index)) {
                    S before = templates[index].withAmount(0L);
                    setSlotSilently(index, resource.empty(), 0L);
                    entries.add(new StorageChange.Entry<>(index, before, resource.empty()));
                }
            }
            if (!entries.isEmpty()) {
                changeDispatcher.dispatch(StorageChange.delta(entries));
            }
        }
        changeDispatcher.dispatch(StorageChange.reset());
    }

    @Nonnull
    public final NBTTagCompound serializeNBT() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();
        for (int index = 0; index < templates.length; index++) {
            if (!configuredAt(index)) {
                continue;
            }
            NBTTagCompound templateTag = resource.writeTemplate(templates[index]);
            if (templateTag == null) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(INDEX, index);
            entry.setLong(AMOUNT, amounts[index]);
            entry.setTag(TEMPLATE, templateTag);
            entries.appendTag(entry);
        }
        root.setTag(ENTRIES, entries);
        return root;
    }

    public final void deserializeNBT(@Nullable NBTTagCompound tag) {
        S[] previousTemplates = copyTemplates();
        long[] previousAmounts = amounts.clone();
        S[] restoredTemplates = copyTemplates();
        Arrays.fill(restoredTemplates, resource.empty());
        long[] restoredAmounts = new long[amounts.length];

        if (tag != null && tag.hasKey(ENTRIES)) {
            NBTTagList entries = tag.getTagList(ENTRIES, 10);
            for (int entryIndex = 0; entryIndex < entries.tagCount(); entryIndex++) {
                NBTTagCompound entry = entries.getCompoundTagAt(entryIndex);
                int index = entry.getInteger(INDEX);
                if (!isValidIndex(index) || !entry.hasKey(TEMPLATE)) {
                    continue;
                }
                long amount = Math.max(0L, entry.getLong(AMOUNT));
                S snapshot = resource.readSnapshot(entry.getCompoundTag(TEMPLATE), amount);
                if (!resource.hasTemplate(snapshot) || (amount == 0L && !isLocked())) {
                    continue;
                }
                restoredTemplates[index] = resource.templateOf(snapshot);
                restoredAmounts[index] = amount;
            }
        }

        boolean changed = false;
        for (int index = 0; index < templates.length; index++) {
            if (!sameSlot(
                previousTemplates[index],
                previousAmounts[index],
                restoredTemplates[index],
                restoredAmounts[index])) {
                changed = true;
            }
            templates[index] = restoredTemplates[index];
            amounts[index] = restoredAmounts[index];
        }
        if (changed && changeDispatcher.hasSubscribers()) {
            changeDispatcher.dispatch(StorageChange.reset());
        }
    }

    @Override
    public final void onChange(@Nonnull StorageChange<S, K> change) {
        changeDispatcher.dispatch(change);
    }

    @Nonnull
    @Override
    public final StorageSubscription subscribe(@Nonnull Consumer<? super StorageChange<S, K>> listener) {
        return changeDispatcher.subscribe(listener);
    }

    protected boolean isOperationEnabled() {
        return true;
    }

    protected boolean hasMaxStorage() {
        return false;
    }

    /**
     * Decides whether an incoming template may share a configured slot.
     * Subclasses enable ore-dictionary equivalence here.
     *
     * @param template  configured slot template
     * @param candidate incoming template
     * @return whether the candidate is accepted
     */
    protected boolean isCompatible(@Nonnull S template, @Nonnull S candidate) {
        return resource.accepts(template, candidate);
    }

    private long capacityOf(@Nonnull S template) {
        if (hasMaxStorage() || isCreative()) {
            return Long.MAX_VALUE;
        }
        double multiplier = getMultiplier();
        if (Double.isNaN(multiplier) || multiplier <= 0D) {
            return 0L;
        }
        double capacity = resource.capacityFor(template) * multiplier;
        if (Double.isInfinite(capacity) || capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return capacity <= 0D ? 0L : (long) Math.floor(capacity);
    }

    private void setSlot(int index, @Nonnull S template, long amount) {
        S before = getSnapshot(index);
        long beforeCapacity = getCapacity(index);
        setSlotSilently(index, template, amount);
        S after = getSnapshot(index);
        if (sameSlot(before, beforeCapacity, after, getCapacity(index))) {
            return;
        }
        changeDispatcher.dispatch(StorageChange.delta(index, before, after));
    }

    private void setSlotSilently(int index, @Nonnull S template, long amount) {
        templates[index] = resource.hasTemplate(template) ? resource.templateOf(template) : resource.empty();
        amounts[index] = Math.max(0L, amount);
    }

    private boolean configuredAt(int index) {
        return resource.hasTemplate(templates[index]);
    }

    private S[] copyTemplates() {
        return templates.clone();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < templates.length;
    }

    private boolean sameSlot(@Nonnull S left, long leftCapacity, @Nonnull S right, long rightCapacity) {
        return sameSnapshot(left, right) && leftCapacity == rightCapacity;
    }

    private boolean sameSnapshot(@Nonnull S left, @Nonnull S right) {
        if (!resource.hasTemplate(left) && !resource.hasTemplate(right)) {
            return true;
        }
        return resource.hasTemplate(left) && resource.hasTemplate(right) && resource.matches(left, right);
    }

    private TransferResult<S, K> emptyResult(long requested, @Nonnull StorageAction action) {
        return new TransferResult<>(Math.max(0L, requested), resource.empty(), action);
    }

    private TransferResult<S, K> processedResult(@Nonnull S request, long processed, @Nonnull StorageAction action) {
        return new TransferResult<>(
            request.getAmount(),
            processed == 0L ? resource.empty() : request.withAmount(processed),
            action);
    }

    private long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
