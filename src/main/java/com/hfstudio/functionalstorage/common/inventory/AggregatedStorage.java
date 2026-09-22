package com.hfstudio.functionalstorage.common.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageChangeDispatcher;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.StorageViewCache;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

public class AggregatedStorage<S extends StorageSnapshot<S, K>, K extends StorageKey> implements IStorageHandler<S, K> {

    private final S empty;
    private final List<StorageSubscription> subscriptions = new ArrayList<>();
    private final StorageChangeDispatcher<S, K> changes = new StorageChangeDispatcher<>();
    private List<IStorageHandler<S, K>> children = List.of();
    private int[] ends = new int[0];
    private int count;
    private final List<IStorageHandler<S, K>> scratch = new ArrayList<>();
    private final Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());

    protected AggregatedStorage(S empty) {
        this.empty = empty;
    }

    public boolean rebuild(List<? extends IStorageHandler<S, K>> candidates) {
        List<IStorageHandler<S, K>> unique = scratch;
        unique.clear();
        identities.clear();
        for (IStorageHandler<S, K> child : candidates) {
            if (child != null && child != this && identities.add(child.getStorageIdentity())) unique.add(child);
        }
        boolean changed = !unique.equals(children);
        if (!changed) {
            int total = 0;
            for (int index = 0; index < unique.size(); index++) {
                total += unique.get(index)
                    .getStorageCount();
                if (total != ends[index]) changed = true;
            }
        }
        identities.clear();
        if (!changed) {
            unique.clear();
            return false;
        }
        for (StorageSubscription subscription : subscriptions) subscription.close();
        subscriptions.clear();
        children = new ArrayList<>(unique);
        ends = new int[unique.size()];
        count = 0;
        for (int index = 0; index < unique.size(); index++) {
            int offset = count;
            count += unique.get(index)
                .getStorageCount();
            ends[index] = count;
            subscriptions.add(
                unique.get(index)
                    .subscribe(change -> forwardChange(offset, change)));
        }
        unique.clear();
        if (changes.hasSubscribers()) changes.dispatch(StorageChange.reset());
        onChildChanged();
        return true;
    }

    /**
     * Drops derived state whenever the aggregated contents or membership change.
     *
     * <p>
     * Called for every child change and for every rebuild, including while nothing
     * is subscribed, because a memoized read must never outlive the state it
     * describes.
     * </p>
     */
    protected void onChildChanged() {}

    private void forwardChange(int offset, StorageChange<S, K> change) {
        // Runs before the subscriber check: a memo must drop even while nobody is
        // listening, or the first reader after a quiet change would see stale state.
        onChildChanged();
        if (!changes.hasSubscribers()) return;
        if (change.isReset()) {
            changes.dispatch(StorageChange.reset());
            return;
        }
        List<StorageChange.Entry<S, K>> entries = new ArrayList<>(
            change.getEntries()
                .size());
        for (StorageChange.Entry<S, K> entry : change.getEntries()) {
            entries.add(new StorageChange.Entry<>(offset + entry.getIndex(), entry.getBefore(), entry.getAfter()));
        }
        changes.dispatch(StorageChange.delta(entries));
    }

    @Override
    public StorageSubscription subscribe(Consumer<? super StorageChange<S, K>> listener) {
        return changes.subscribe(listener);
    }

    @Override
    public int getStorageCount() {
        return count;
    }

    @Override
    public S getSnapshot(int index) {
        int child = childIndex(index);
        return child < 0 ? empty
            : children.get(child)
                .getSnapshot(localIndex(index, child));
    }

    @Override
    public long getCapacity(int index) {
        int child = childIndex(index);
        return child < 0 ? 0
            : children.get(child)
                .getCapacity(localIndex(index, child));
    }

    @Override
    public TransferResult<S, K> insert(int index, S request, StorageAction action) {
        int child = childIndex(index);
        return child < 0 ? new TransferResult<>(request.getAmount(), empty, action)
            : children.get(child)
                .insert(localIndex(index, child), request, action);
    }

    @Override
    public TransferResult<S, K> extract(int index, long amount, StorageAction action) {
        int child = childIndex(index);
        return child < 0 ? new TransferResult<>(Math.max(0, amount), empty, action)
            : children.get(child)
                .extract(localIndex(index, child), amount, action);
    }

    /**
     * Reports whether one spanned index retains and enforces a filter.
     *
     * <p>
     * The spanned drawers each carry their own lock, so asking this storage as a whole
     * would answer with the interface default and claim nothing is locked. A caller
     * deciding whether an index invites a resource must get that index's own answer,
     * or the storage advertises room that the drawer behind it will refuse.
     * </p>
     *
     * @param index aggregate index
     * @return whether the storage behind that index is locked
     */
    @Override
    public boolean isLocked(int index) {
        int child = childIndex(index);
        return child >= 0 && children.get(child)
            .isLocked(localIndex(index, child));
    }

    @Override
    public boolean voidsOverflow(int index) {
        int child = childIndex(index);
        return child >= 0 && children.get(child)
            .voidsOverflow(localIndex(index, child));
    }

    /**
     * Reports equivalence support for the aggregate.
     *
     * <p>
     * Any spanned storage that widens matching forces a real compatibility probe,
     * because the probe may succeed in that storage even though it cannot in the
     * others. Reporting false while a child widens matching would silently stop such
     * resources from sharing a slot.
     * </p>
     *
     * @return whether any spanned storage accepts equivalent resources
     */
    @Override
    public boolean allowsEquivalentResources() {
        for (IStorageHandler<S, K> child : children) {
            if (child.allowsEquivalentResources()) {
                return true;
            }
        }
        return false;
    }

    protected IStorageHandler<S, K> childAt(int index) {
        int child = childIndex(index);
        return child < 0 ? null : children.get(child);
    }

    protected int childSlot(int index) {
        int child = childIndex(index);
        return child < 0 ? -1 : localIndex(index, child);
    }

    private int childIndex(int index) {
        if (index < 0 || index >= count) return -1;
        int low = 0;
        int high = ends.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (index < ends[middle]) high = middle;
            else low = middle + 1;
        }
        return low;
    }

    private int localIndex(int index, int child) {
        return index - (child == 0 ? 0 : ends[child - 1]);
    }

    public static class Items extends AggregatedStorage<BigItemStack, ItemStorageKey> implements IBigItemHandler {

        private final StorageViewCache viewCache = new StorageViewCache();

        public Items() {
            super(BigItemStack.empty());
        }

        @Override
        public StorageViewCache getStorageViewCache() {
            return viewCache;
        }

        @Override
        protected void onChildChanged() {
            viewCache.invalidate();
        }

        @Override
        public boolean isEmptyStorageAvailable(int index) {
            IStorageHandler<BigItemStack, ItemStorageKey> child = childAt(index);
            return child instanceof IBigItemHandler items && items.isEmptyStorageAvailable(childSlot(index));
        }
    }

    public static class Fluids extends AggregatedStorage<BigFluidStack, FluidStorageKey> implements IBigFluidHandler {

        // Null means unknown, so the first request computes a real answer. Cached
        // because an untyped drain asks for the first populated index once per tank a
        // caller was told about, which is how AE2's storage bus polls.
        private Integer firstPopulated;

        public Fluids() {
            super(BigFluidStack.empty());
        }

        @Override
        public int firstPopulatedIndex() {
            Integer cached = firstPopulated;
            if (cached == null) {
                cached = IBigFluidHandler.super.firstPopulatedIndex();
                firstPopulated = cached;
            }
            return cached;
        }

        @Override
        protected void onChildChanged() {
            firstPopulated = null;
        }

        /**
         * Drops the memo without computing a replacement.
         *
         * <p>
         * The owning network calls this when a linked drawer changes capacity, which
         * alters what this storage holds without any amount moving.
         * </p>
         */
        public void invalidateFirstPopulated() {
            firstPopulated = null;
        }
    }

    public static class Aspects extends AggregatedStorage<BigAspectStack, AspectStorageKey>
        implements IBigAspectHandler {

        // Starts null so the first request computes a real summary. A pre-filled
        // value would be returned as-is and report an empty storage forever.
        private AspectSummary summary;

        public Aspects() {
            super(BigAspectStack.empty());
        }

        @Override
        @Nonnull
        public AspectSummary getSummary() {
            AspectSummary cached = summary;
            if (cached == null) {
                cached = AspectSummary.of(this);
                summary = cached;
            }
            return cached;
        }

        @Override
        protected void onChildChanged() {
            summary = null;
        }

        /**
         * Drops the memo without computing a replacement.
         *
         * <p>
         * A lock transition arrives as a child change, and an aggregate answers
         * {@code isLocked} from the interface default, so contents are the only input
         * this memo depends on. The owning network also calls this when a linked
         * drawer changes capacity, which alters what the storage invites without
         * moving any essentia.
         * </p>
         */
        public void invalidateSummary() {
            summary = null;
        }
    }
}
