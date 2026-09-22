package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import lombok.Getter;

/**
 * Read-only aggregation view that collapses a multi-index item handler into one
 * entry per distinct item key. Used by the Forge-facing bridges so external
 * automation sees typed content instead of raw physical slots.
 */
public class ItemStorageView {

    @Getter
    private final BigItemStack snapshot;
    @Getter
    private final long capacity;
    private final boolean voidsOverflow;

    private ItemStorageView(BigItemStack snapshot, long capacity, boolean voidsOverflow) {
        this.snapshot = snapshot;
        this.capacity = Math.max(0L, capacity);
        this.voidsOverflow = voidsOverflow;
    }

    /**
     * Collapses all populated indices into one view per exact item key.
     *
     * <p>
     * Walks every index of the handler. Callers that ask repeatedly for the same
     * unchanged handler should go through {@link #storages(IBigItemHandler)},
     * which reuses a handler's own memo when it has one.
     * </p>
     *
     * @param handler handler to read
     * @return immutable ordered views
     */
    @Nonnull
    public static List<ItemStorageView> aggregate(@Nonnull IBigItemHandler handler) {
        Map<ItemStorageKey, ItemStorageView> byKey = new LinkedHashMap<>();
        int count = Math.max(0, handler.getStorageCount());
        for (int index = 0; index < count; index++) {
            BigItemStack snapshot = handler.getSnapshot(index);
            if (!snapshot.hasTemplate() || snapshot.isEmpty()) {
                continue;
            }
            ItemStorageKey key = snapshot.getKey();
            ItemStorageView previous = byKey.get(key);
            long capacity = handler.getCapacity(index);
            boolean voids = handler.voidsOverflow(index);
            if (previous == null) {
                byKey.put(key, new ItemStorageView(snapshot, capacity, voids));
            } else {
                byKey.put(
                    key,
                    new ItemStorageView(
                        previous.snapshot.withAmount(saturatedAdd(previous.snapshot.getAmount(), snapshot.getAmount())),
                        saturatedAdd(previous.capacity, capacity),
                        previous.voidsOverflow || voids));
            }
        }
        // Unmodifiable because a memoized result is handed to several readers.
        return Collections.unmodifiableList(new ArrayList<>(byKey.values()));
    }

    /**
     * Aggregates every populated index into one view per exact item key, reusing
     * the handler's memo when it keeps one.
     *
     * @param handler handler to read
     * @return immutable ordered views
     */
    @Nonnull
    public static List<ItemStorageView> storages(@Nonnull IBigItemHandler handler) {
        StorageViewCache cache = handler.getStorageViewCache();
        return cache == null ? aggregate(handler) : cache.views(handler);
    }

    public static boolean hasEmptyStorage(@Nonnull IBigItemHandler handler) {
        StorageViewCache cache = handler.getStorageViewCache();
        if (cache != null) {
            return cache.hasEmptyStorage(handler);
        }
        int count = Math.max(0, handler.getStorageCount());
        for (int index = 0; index < count; index++) {
            if (handler.isEmptyStorageAvailable(index)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param handler handler to inspect
     * @return summed capacity of every unconfigured insertion-capable index
     */
    public static long emptyStorageCapacity(@Nonnull IBigItemHandler handler) {
        StorageViewCache cache = handler.getStorageViewCache();
        if (cache != null) {
            return cache.emptyCapacity(handler);
        }
        long capacity = 0L;
        int count = Math.max(0, handler.getStorageCount());
        for (int index = 0; index < count; index++) {
            if (handler.isEmptyStorageAvailable(index)) {
                capacity = saturatedAdd(capacity, handler.getCapacity(index));
            }
        }
        return capacity;
    }

    /**
     * Reports how many virtual slots a handler exposes, counting the leading
     * empty insertion slot when the handler offers one.
     *
     * @param handler handler to inspect
     * @return the virtual slot count
     */
    public static int virtualSlots(@Nonnull IBigItemHandler handler) {
        StorageViewCache cache = handler.getStorageViewCache();
        if (cache != null) {
            return cache.virtualSlots(handler);
        }
        return storages(handler).size() + (hasEmptyStorage(handler) ? 1 : 0);
    }

    /**
     * Reports the writable amount of one virtual slot.
     *
     * @param handler handler to read
     * @param slot    virtual slot index
     * @return the slot limit, or zero when the slot does not exist
     */
    public static int slotLimit(@Nonnull IBigItemHandler handler, int slot) {
        boolean hasEmpty = hasEmptyStorage(handler);
        if (hasEmpty && slot == 0) {
            return toForgeLimit(emptyStorageCapacity(handler));
        }
        ItemStorageView storage = storageAt(slot, hasEmpty, storages(handler));
        if (storage == null) {
            return 0;
        }
        return storage.voidsOverflow() ? Integer.MAX_VALUE : toForgeLimit(storage.getCapacity());
    }

    /**
     * Resolves one aggregated view from a virtual slot index.
     *
     * @param slot     virtual slot reported by the bridge
     * @param hasEmpty whether the bridge reserves slot zero for insertions
     * @param storages aggregated views
     * @return the matching view, or {@code null}
     */
    public static ItemStorageView storageAt(int slot, boolean hasEmpty, @Nonnull List<ItemStorageView> storages) {
        int index = slot - (hasEmpty ? 1 : 0);
        return index < 0 || index >= storages.size() ? null : storages.get(index);
    }

    public static int toForgeLimit(long value) {
        long capacity = Math.max(0L, value);
        return capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    private static long saturatedAdd(long left, long right) {
        long safeLeft = Math.max(0L, left);
        long safeRight = Math.max(0L, right);
        return safeLeft > Long.MAX_VALUE - safeRight ? Long.MAX_VALUE : safeLeft + safeRight;
    }

    public boolean voidsOverflow() {
        return voidsOverflow;
    }

    public ItemStack toItemStack() {
        return snapshot.toItemStack();
    }
}
