package com.hfstudio.functionalstorage.api.storage;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Memoizes the Forge-facing aggregation of one item handler.
 *
 * <p>
 * Aggregating a handler walks every index, and the virtual-slot bridge asks for
 * the aggregation once per slot. A handler that spans a whole controller network
 * would otherwise re-walk the network for each of its slots, and external
 * automation polls exactly like that. A handler that keeps one of these pays the
 * walk once per change instead of once per question.
 * </p>
 *
 * <p>
 * A handler owns its cache and must invalidate it whenever its slots, capacities,
 * or lock state change. The cache holds no clock of its own: it trusts the owner
 * to invalidate, which keeps it allocation-free on the read path.
 * </p>
 */
public class StorageViewCache {

    @Nullable
    private List<ItemStorageView> views;
    private boolean emptyMeasured;
    private boolean hasEmpty;
    private long emptyCapacity;
    private int virtualSlots = -1;

    /**
     * Discards every memoized figure. Call this from any path that changes a
     * slot, a capacity, or the lock state.
     */
    public void invalidate() {
        views = null;
        emptyMeasured = false;
        hasEmpty = false;
        emptyCapacity = 0L;
        virtualSlots = -1;
    }

    @Nonnull
    public List<ItemStorageView> views(@Nonnull IBigItemHandler handler) {
        List<ItemStorageView> cached = views;
        if (cached == null) {
            cached = ItemStorageView.aggregate(handler);
            views = cached;
        }
        return cached;
    }

    public boolean hasEmptyStorage(@Nonnull IBigItemHandler handler) {
        measureEmpty(handler);
        return hasEmpty;
    }

    public long emptyCapacity(@Nonnull IBigItemHandler handler) {
        measureEmpty(handler);
        return emptyCapacity;
    }

    public int virtualSlots(@Nonnull IBigItemHandler handler) {
        int cached = virtualSlots;
        if (cached < 0) {
            cached = views(handler).size() + (hasEmptyStorage(handler) ? 1 : 0);
            virtualSlots = cached;
        }
        return cached;
    }

    private void measureEmpty(@Nonnull IBigItemHandler handler) {
        if (emptyMeasured) {
            return;
        }
        emptyMeasured = true;
        int count = Math.max(0, handler.getStorageCount());
        long capacity = 0L;
        for (int index = 0; index < count; index++) {
            if (!handler.isEmptyStorageAvailable(index)) {
                continue;
            }
            hasEmpty = true;
            long slotCapacity = Math.max(0L, handler.getCapacity(index));
            capacity = capacity > Long.MAX_VALUE - slotCapacity ? Long.MAX_VALUE : capacity + slotCapacity;
        }
        emptyCapacity = capacity;
    }
}
