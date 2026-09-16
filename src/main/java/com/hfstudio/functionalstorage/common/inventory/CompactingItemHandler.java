package com.hfstudio.functionalstorage.common.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageChangeDispatcher;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.storage.CompactingTier;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;
import com.hfstudio.functionalstorage.util.ItemUtil;

import lombok.Getter;

/**
 * Long-capacity item storage whose visible slots are lossless views of one
 * shared amount counted in the lowest configured tier. Storing a block of iron
 * therefore also shows the equivalent ingots and nuggets, and extracting from
 * any tier consumes the same shared amount.
 *
 * <p>
 * Tier definitions and public reads are immutable snapshots; simulations
 * never configure or mutate storage.
 * </p>
 */
public abstract class CompactingItemHandler implements IBigItemHandler {

    private static final String ENTRIES = "Entries";
    private static final String INDEX = "Index";
    private static final String BASE_UNITS = "BaseUnits";
    private static final String TEMPLATE = "Template";
    private static final String BASE_AMOUNT = "BaseAmount";
    private static final String TIERS = "Tiers";

    private final CompactingTier[] tiers;
    private final StorageChangeDispatcher<BigItemStack, ItemStorageKey> changeDispatcher = new StorageChangeDispatcher<>();

    private long baseAmount;
    @Getter
    private boolean configured;

    public CompactingItemHandler(int slots) {
        this.tiers = new CompactingTier[Math.max(0, slots)];
        clearConfiguration();
    }

    private static TransferResult<BigItemStack, ItemStorageKey> emptyResult(long requested, StorageAction action) {
        return new TransferResult<>(Math.max(0L, requested), BigItemStack.empty(), action);
    }

    private static TransferResult<BigItemStack, ItemStorageKey> processedResult(BigItemStack request, long processed,
        StorageAction action) {
        return new TransferResult<>(
            request.getAmount(),
            processed == 0L ? BigItemStack.empty() : request.withAmount(processed),
            action);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static long saturatedMultiply(long value, long factor) {
        if (value <= 0L || factor <= 0L) {
            return 0L;
        }
        return value > Long.MAX_VALUE / factor ? Long.MAX_VALUE : value * factor;
    }

    @Override
    public int getStorageCount() {
        return tiers.length;
    }

    @Nonnull
    @Override
    public BigItemStack getSnapshot(int index) {
        if (!isValidIndex(index) || !tiers[index].hasTemplate()) {
            return BigItemStack.empty();
        }
        long amount = isCreative() ? Long.MAX_VALUE : baseAmount / tiers[index].getBaseUnits();
        return new BigItemStack(tiers[index].getTemplate(), amount);
    }

    @Override
    public long getCapacity(int index) {
        if (!isValidIndex(index) || !tiers[index].hasTemplate()) {
            return 0L;
        }
        if (hasMaxStorage() || isCreative()) {
            return Long.MAX_VALUE;
        }
        return getTotalBaseCapacity() / tiers[index].getBaseUnits();
    }

    @Nonnull
    @Override
    public TransferResult<BigItemStack, ItemStorageKey> insert(int index, @Nonnull BigItemStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L || !isValidIndex(index) || !isOperationEnabled()) {
            return emptyResult(requested, action);
        }

        CompactingTier tier = tiers[index];
        if (!tier.hasTemplate() || !isCompatible(tier, request)) {
            return emptyResult(requested, action);
        }

        if (isCreative()) {
            return processedResult(request, requested, action);
        }

        long capacity = getTotalBaseCapacity();
        long unit = tier.getBaseUnits();
        long freeUnits = baseAmount >= capacity ? 0L : capacity - baseAmount;
        long inserted = Math.min(requested, freeUnits / unit);
        long processed = voidsOverflow() ? requested : inserted;

        if (action == StorageAction.EXECUTE && inserted > 0L) {
            BigItemStack[] before = snapshots();
            baseAmount = saturatedAdd(baseAmount, saturatedMultiply(inserted, unit));
            publishVisibleDelta(before);
        }
        return processedResult(request, processed, action);
    }

    @Nonnull
    @Override
    public TransferResult<BigItemStack, ItemStorageKey> extract(int index, long amount, @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = Math.max(0L, amount);
        if (requested == 0L || !isValidIndex(index) || !tiers[index].hasTemplate() || !isOperationEnabled()) {
            return emptyResult(requested, action);
        }

        CompactingTier tier = tiers[index];
        long unit = tier.getBaseUnits();
        long available = isCreative() ? requested : baseAmount / unit;
        long extracted = Math.min(requested, available);
        if (extracted == 0L) {
            return emptyResult(requested, action);
        }

        if (action == StorageAction.EXECUTE && !isCreative()) {
            BigItemStack[] before = snapshots();
            baseAmount -= extracted * unit;
            if (baseAmount <= 0L) {
                baseAmount = 0L;
                if (!isLocked()) {
                    clearConfiguration();
                }
            }
            publishVisibleDelta(before);
        }
        return new TransferResult<>(requested, new BigItemStack(tier.getTemplate(), extracted), action);
    }

    /**
     * Replaces the compression tiers with detached immutable definitions.
     * Missing entries are padded with empty tiers and extra entries ignored.
     * A refresh that keeps the same item chain preserves the stored amount.
     *
     * @param newTiers replacement tier definitions
     */
    public void configureTiers(@Nonnull List<CompactingTier> newTiers) {
        Objects.requireNonNull(newTiers, "newTiers");
        BigItemStack[] before = snapshots();
        CompactingTier[] previous = tiers.clone();
        boolean changed = false;
        boolean hasTemplate = false;

        for (int index = 0; index < tiers.length; index++) {
            CompactingTier tier = index < newTiers.size() && newTiers.get(index) != null ? newTiers.get(index)
                : CompactingTier.empty();
            CompactingTier replacement = tier.copy();
            changed |= !tiers[index].sameDefinition(replacement);
            tiers[index] = replacement;
            hasTemplate |= replacement.hasTemplate();
        }

        changed |= configured != hasTemplate;
        configured = hasTemplate;
        if (!configured && baseAmount != 0L) {
            baseAmount = 0L;
            changed = true;
        }
        if (changed) {
            publishDefinitionDelta(before, previous);
        }
    }

    /**
     * @return an unmodifiable list of detached tier definitions
     */
    @Nonnull
    public List<CompactingTier> getTiers() {
        List<CompactingTier> copies = new ArrayList<>(tiers.length);
        for (CompactingTier tier : tiers) {
            copies.add(tier.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    /**
     * @return the exact stored amount counted in lowest-tier units
     */
    public long getStoredBaseAmount() {
        return baseAmount;
    }

    /**
     * @return the maximum shared amount in lowest-tier units
     */
    public long getTotalBaseCapacity() {
        return getTotalBaseCapacity(getMultiplier());
    }

    /**
     * Calculates the shared capacity for a prospective multiplier without
     * mutating upgrades or contents, so an upgrade can be validated before it
     * is installed.
     *
     * @param multiplier prospective storage multiplier
     * @return capacity in lowest-tier units
     */
    public long getTotalBaseCapacity(double multiplier) {
        if (!configured) {
            return 0L;
        }
        CompactingTier base = null;
        long largestUnits = 1L;
        for (CompactingTier tier : tiers) {
            if (!tier.hasTemplate()) {
                continue;
            }
            largestUnits = Math.max(largestUnits, tier.getBaseUnits());
            if (base == null || tier.getBaseUnits() < base.getBaseUnits()) {
                base = tier;
            }
        }
        if (base == null || Double.isNaN(multiplier) || multiplier <= 0D) {
            return 0L;
        }
        int maxStackSize = Math.max(
            0,
            base.getTemplate()
                .getMaxStackSize());
        double capacity = multiplier * maxStackSize * (double) largestUnits;
        if (Double.isInfinite(capacity) || capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return capacity <= 0D ? 0L : (long) Math.floor(capacity);
    }

    /**
     * @param index candidate index
     * @return whether the index participates in insertion
     */
    public boolean canDoubleClickSlot(int index) {
        return isValidIndex(index) && (isLocked() || tiers[index].hasTemplate());
    }

    /**
     * Applies tier retention for a lock transition and emits exactly one
     * resynchronization event.
     *
     * @param locked new lock state
     */
    public void applyLockConfiguration(boolean locked) {
        if (!locked && !isCreative() && baseAmount == 0L && configured) {
            clearConfiguration();
        }
        changeDispatcher.dispatch(StorageChange.reset());
    }

    /**
     * @return a fresh tag holding the shared amount and every tier definition
     */
    @Nonnull
    public NBTTagCompound serializeNBT() {
        NBTTagCompound root = new NBTTagCompound();
        root.setLong(BASE_AMOUNT, baseAmount);
        NBTTagList tierList = new NBTTagList();
        for (int index = 0; index < tiers.length; index++) {
            CompactingTier tier = tiers[index];
            if (!tier.hasTemplate()) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(INDEX, index);
            entry.setLong(BASE_UNITS, tier.getBaseUnits());
            entry
                .setTag(TEMPLATE, ItemStorageResource.INSTANCE.writeTemplate(new BigItemStack(tier.getTemplate(), 1L)));
            tierList.appendTag(entry);
        }
        root.setTag(TIERS, tierList);
        return root;
    }

    /**
     * Replaces tiers and contents from persisted data.
     *
     * @param tag previously produced by {@link #serializeNBT()}
     */
    public void deserializeNBT(@Nullable NBTTagCompound tag) {
        CompactingTier[] beforeTiers = tiers.clone();
        long beforeAmount = baseAmount;
        boolean beforeConfigured = configured;
        clearConfiguration();

        if (tag != null) {
            NBTTagList tierList = tag.getTagList(TIERS, 10);
            for (int entryIndex = 0; entryIndex < tierList.tagCount(); entryIndex++) {
                NBTTagCompound entry = tierList.getCompoundTagAt(entryIndex);
                int index = entry.getInteger(INDEX);
                if (!isValidIndex(index) || !entry.hasKey(TEMPLATE)) {
                    continue;
                }
                BigItemStack restored = ItemStorageResource.INSTANCE.readSnapshot(entry.getCompoundTag(TEMPLATE), 1L);
                if (!restored.hasTemplate()) {
                    continue;
                }
                tiers[index] = new CompactingTier(restored.getTemplate(), Math.max(1L, entry.getLong(BASE_UNITS)));
                configured = true;
            }
            baseAmount = configured ? Math.max(0L, tag.getLong(BASE_AMOUNT)) : 0L;
            if (baseAmount == 0L && !isLocked() && !isCreative()) {
                clearConfiguration();
            }
        }

        if (changeDispatcher.hasSubscribers() && !sameState(beforeTiers, beforeAmount, beforeConfigured)) {
            changeDispatcher.dispatch(StorageChange.reset());
        }
    }

    @Override
    public void onChange(@Nonnull StorageChange<BigItemStack, ItemStorageKey> change) {
        changeDispatcher.dispatch(change);
    }

    @Nonnull
    @Override
    public StorageSubscription subscribe(
        @Nonnull Consumer<? super StorageChange<BigItemStack, ItemStorageKey>> listener) {
        return changeDispatcher.subscribe(listener);
    }

    /**
     * @return the compacting storage multiplier
     */
    public abstract double getMultiplier();

    /**
     * @return whether ore-dictionary equivalents may share a tier
     */
    protected boolean allowsEquivalentItems() {
        return false;
    }

    /**
     * @return whether finite capacity is replaced with {@link Long#MAX_VALUE}
     */
    protected boolean hasMaxStorage() {
        return false;
    }

    /**
     * @return whether this handler's owning container currently allows transactions
     */
    protected boolean isOperationEnabled() {
        return true;
    }

    private boolean isCompatible(@Nonnull CompactingTier tier, @Nonnull BigItemStack request) {
        ItemStack template = tier.getTemplate();
        ItemStack incoming = request.getTemplate();
        return ItemUtil.areItemStacksCompatible(template, incoming, allowsEquivalentItems());
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < tiers.length;
    }

    private void clearConfiguration() {
        for (int index = 0; index < tiers.length; index++) {
            tiers[index] = CompactingTier.empty();
        }
        baseAmount = 0L;
        configured = false;
    }

    private BigItemStack[] snapshots() {
        BigItemStack[] snapshots = new BigItemStack[tiers.length];
        for (int index = 0; index < tiers.length; index++) {
            snapshots[index] = getSnapshot(index);
        }
        return snapshots;
    }

    private void publishVisibleDelta(BigItemStack[] before) {
        List<StorageChange.Entry<BigItemStack, ItemStorageKey>> entries = new ArrayList<>();
        for (int index = 0; index < tiers.length; index++) {
            BigItemStack after = getSnapshot(index);
            if (before[index].hasTemplate() || after.hasTemplate()) {
                entries.add(new StorageChange.Entry<>(index, before[index], after));
            }
        }
        if (!entries.isEmpty()) {
            changeDispatcher.dispatch(StorageChange.delta(entries));
        }
    }

    private void publishDefinitionDelta(BigItemStack[] before, CompactingTier[] previousTiers) {
        List<StorageChange.Entry<BigItemStack, ItemStorageKey>> entries = new ArrayList<>();
        for (int index = 0; index < tiers.length; index++) {
            BigItemStack after = getSnapshot(index);
            boolean definitionChanged = !previousTiers[index].sameDefinition(tiers[index]);
            boolean contentChanged = before[index].getAmount() != after.getAmount();
            if (definitionChanged || contentChanged) {
                entries.add(new StorageChange.Entry<>(index, before[index], after));
            }
        }
        if (!entries.isEmpty()) {
            changeDispatcher.dispatch(StorageChange.delta(entries));
        }
    }

    private boolean sameState(CompactingTier[] previousTiers, long previousAmount, boolean previousConfigured) {
        if (previousAmount != baseAmount || previousConfigured != configured || previousTiers.length != tiers.length) {
            return false;
        }
        for (int index = 0; index < tiers.length; index++) {
            if (!previousTiers[index].sameDefinition(tiers[index])) {
                return false;
            }
        }
        return true;
    }
}
