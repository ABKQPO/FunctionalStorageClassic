package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

/**
 * Forge item capability bridge for a generic long-capacity storage handler.
 * Business state is exposed only through {@link IStorageHandler}; the methods below
 * adapt that state to Forge's int-count API and retain item routing semantics needed by
 * the capability.
 */
public interface IBigItemHandler extends IStorageHandler<BigItemStack, ItemStorageKey> {

    /**
     * A memo the bridge may reuse across reads, or {@code null} for a handler that is
     * cheap enough to walk on every question.
     *
     * <p>
     * These methods are asked once per virtual slot by Forge-style callers, so a handler
     * spanning many indices should return a cache it invalidates on every change.
     * Handlers that do not return one are aggregated on each call, which is correct and
     * merely slower.
     * </p>
     */
    @Nullable
    default StorageViewCache getStorageViewCache() {
        return null;
    }

    /**
     * Exposes one virtual slot per stored item key and, when available, one leading
     * empty insertion slot. Physical storage positions stay internal.
     */
    default int getSlots() {
        return ItemStorageView.virtualSlots(this);
    }

    /**
     * Returns the virtual empty slot or one aggregated item-key view. Empty physical
     * slots are not shown as typed content; a zero-amount retained filter is exposed
     * through the leading empty slot so automation can insert the matching type into a
     * configured empty drawer.
     */
    default ItemStack getStackInSlot(int slot) {
        boolean hasEmpty = ItemStorageView.hasEmptyStorage(this);
        if (hasEmpty && slot == 0) {
            return null;
        }
        ItemStorageView storage = ItemStorageView.storageAt(slot, hasEmpty, ItemStorageView.storages(this));
        return storage == null ? null : storage.toItemStack();
    }

    default boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    default int getSlotLimit(int slot) {
        return ItemStorageView.slotLimit(this, slot);
    }

    /**
     * The supplied slot is only a Forge compatibility argument and never selects a
     * physical drawer.
     */
    default ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        Objects.requireNonNull(stack, "stack");
        if (stack.getItem() == null || stack.stackSize <= 0) {
            return stack.getItem() == null ? null : stack.copy();
        }
        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        TransferResult<BigItemStack, ItemStorageKey> result = insertRouted(
            request,
            StorageAction.fromSimulation(simulate));
        long remaining = result.getRemainingAmount();
        if (remaining == 0L) {
            return null;
        }
        ItemStack remainder = stack.copy();
        remainder.stackSize = (int) Math.min(remaining, stack.stackSize);
        return remainder;
    }

    /**
     * The physical drawer selected by the route is internal.
     */
    default ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return null;
        }
        boolean hasEmpty = ItemStorageView.hasEmptyStorage(this);
        ItemStorageView storage = ItemStorageView.storageAt(slot, hasEmpty, ItemStorageView.storages(this));
        if (storage == null) {
            return null;
        }
        ItemStack template = storage.getSnapshot()
            .getTemplate();
        if (template == null) {
            return null;
        }
        long requested = Math.min(amount, Math.max(0, template.getMaxStackSize()));
        if (requested <= 0L) {
            return null;
        }
        TransferResult<BigItemStack, ItemStorageKey> result = extractRouted(
            new BigItemStack(template, requested),
            StorageAction.fromSimulation(simulate));
        long processed = Math.min(requested, Math.max(0L, result.getProcessedAmount()));
        return processed == 0L ? null
            : result.getProcessed()
                .withAmount(processed)
                .toItemStack();
    }

    default boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if (stack.getItem() == null || !isValidSlot(slot)) {
            return false;
        }
        TransferResult<BigItemStack, ItemStorageKey> result = insertRouted(
            new BigItemStack(stack, 1L),
            StorageAction.SIMULATE);
        return result.getProcessedAmount() > 0L;
    }

    /**
     * Whether one internal index can represent the virtual empty insertion slot.
     * Aggregate handlers may override this to inspect the owning child instead of their
     * own aggregate lock state.
     */
    default boolean isEmptyStorageAvailable(int index) {
        if (index < 0 || index >= Math.max(0, getStorageCount())) {
            return false;
        }
        BigItemStack snapshot = getSnapshot(index);
        // The lock is read per index: a storage spanning several drawers carries one
        // lock per drawer, so the storage-wide answer would misdescribe this index.
        return snapshot.isEmpty() && getCapacity(index) > 0L && (snapshot.hasTemplate() || !isLocked(index));
    }

    /**
     * Routes insertion through matching configured indices and then empty indices. The
     * generic index methods are the only state operations used.
     */
    @Nonnull
    default TransferResult<BigItemStack, ItemStorageKey> insertRouted(@Nonnull BigItemStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigItemStack.empty(), action);
        }
        long processedTotal = 0L;
        BigItemStack compatibilityProbe = request.withAmount(1L);
        int count = Math.max(0, getStorageCount());
        // A storage that never treats different resources as interchangeable can only
        // accept into a slot already holding the exact type, so the middle pass could
        // only discover that. Skipping it removes a full walk plus a probe per occupied
        // index, which is most of the cost of storing a new type.
        int passes = allowsEquivalentResources() ? 3 : 2;
        for (int pass = 0; pass < passes && processedTotal < requested; pass++) {
            for (int index = 0; index < count && processedTotal < requested; index++) {
                BigItemStack current = getSnapshot(index);
                boolean hasTemplate = current.hasTemplate();
                boolean exact = hasTemplate && current.isSameType(request);
                if (pass == 0 && !exact) {
                    continue;
                }
                if (passes == 3 && pass == 1) {
                    if (!hasTemplate || exact) {
                        continue;
                    }
                    TransferResult<BigItemStack, ItemStorageKey> probe = insert(
                        index,
                        compatibilityProbe,
                        StorageAction.SIMULATE);
                    if (probe.getProcessedAmount() <= 0L) {
                        continue;
                    }
                }
                if (pass == passes - 1 && hasTemplate) {
                    continue;
                }
                long remaining = requested - processedTotal;
                TransferResult<BigItemStack, ItemStorageKey> result = insert(
                    index,
                    request.withAmount(remaining),
                    action);
                long processed = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
                processedTotal = saturatedAdd(processedTotal, processed);
            }
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    /**
     * Routes type-sensitive extraction through matching generic indices.
     */
    @Nonnull
    default TransferResult<BigItemStack, ItemStorageKey> extractRouted(@Nonnull BigItemStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigItemStack.empty(), action);
        }
        long processedTotal = 0L;
        int count = Math.max(0, getStorageCount());
        for (int index = 0; index < count && processedTotal < requested; index++) {
            BigItemStack current = getSnapshot(index);
            if (current.isEmpty() || !current.isSameType(request)) {
                continue;
            }
            long remaining = requested - processedTotal;
            TransferResult<BigItemStack, ItemStorageKey> result = extract(index, remaining, action);
            long processed = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
            processedTotal = saturatedAdd(processedTotal, processed);
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
