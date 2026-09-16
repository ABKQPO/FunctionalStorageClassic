package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

/**
 * Read-only aggregation view that collapses a multi-index item handler into one
 * entry per distinct item key. Used by the Forge-facing bridges so external
 * automation sees typed content instead of raw physical slots.
 */
public class ItemStorageView {

    private final BigItemStack snapshot;
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
     * @param handler handler to read
     * @return immutable ordered views
     */
    @Nonnull
    public static List<ItemStorageView> storages(@Nonnull IBigItemHandler handler) {
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
        return new ArrayList<>(byKey.values());
    }

    /**
     * @param handler handler to inspect
     * @return whether any unconfigured index can accept an insertion
     */
    public static boolean hasEmptyStorage(@Nonnull IBigItemHandler handler) {
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

    /**
     * @param value arbitrary capacity
     * @return saturated Forge slot limit
     */
    public static int toForgeLimit(long value) {
        long capacity = Math.max(0L, value);
        return capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    private static long saturatedAdd(long left, long right) {
        long safeLeft = Math.max(0L, left);
        long safeRight = Math.max(0L, right);
        return safeLeft > Long.MAX_VALUE - safeRight ? Long.MAX_VALUE : safeLeft + safeRight;
    }

    /**
     * @return the aggregated stored amount
     */
    public BigItemStack getSnapshot() {
        return snapshot;
    }

    /**
     * @return the aggregated capacity
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * @return whether any contributing index voids compatible overflow
     */
    public boolean voidsOverflow() {
        return voidsOverflow;
    }

    /**
     * @return a fresh count-saturated stack for external display, or {@code null}
     */
    public ItemStack toItemStack() {
        return snapshot.toItemStack();
    }
}
