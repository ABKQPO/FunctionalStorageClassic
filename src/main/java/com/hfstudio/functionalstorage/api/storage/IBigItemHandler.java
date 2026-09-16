package com.hfstudio.functionalstorage.api.storage;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

/**
 * Forge item capability bridge for a generic long-capacity storage handler.
 * Business state is exposed only through {@link IStorageHandler}; the methods
 * below adapt that state to Forge's int-count API and retain item routing
 * semantics needed by the capability.
 */
public interface IBigItemHandler extends IStorageHandler<BigItemStack, ItemStorageKey> {

    /**
     * Exposes one virtual slot per stored item key and, when available, one
     * leading empty insertion slot. Physical storage positions stay internal.
     *
     * @return the number of virtual slots
     */
    default int getSlots() {
        return ItemStorageView.storages(this)
            .size() + (ItemStorageView.hasEmptyStorage(this) ? 1 : 0);
    }

    /**
     * Returns the virtual empty slot or one aggregated item-key view. Empty
     * physical slots are not shown as typed content; a zero-amount retained
     * filter is exposed through the leading empty slot so automation can insert
     * the matching type into a configured empty drawer.
     *
     * @param slot virtual slot index
     * @return the visible stack, or {@code null}
     */
    default ItemStack getStackInSlot(int slot) {
        boolean hasEmpty = ItemStorageView.hasEmptyStorage(this);
        if (hasEmpty && slot == 0) {
            return null;
        }
        ItemStorageView storage = ItemStorageView.storageAt(slot, hasEmpty, ItemStorageView.storages(this));
        return storage == null ? null : storage.toItemStack();
    }

    /**
     * Checks Forge's slot validity for a virtual slot.
     *
     * @param slot virtual slot index
     * @return whether the slot exists
     */
    default boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    /**
     * Computes the Forge slot limit for a virtual slot.
     *
     * @param slot virtual slot index
     * @return the saturated capacity
     */
    default int getSlotLimit(int slot) {
        boolean hasEmpty = ItemStorageView.hasEmptyStorage(this);
        List<ItemStorageView> storages = ItemStorageView.storages(this);
        if (hasEmpty && slot == 0) {
            return ItemStorageView.toForgeLimit(ItemStorageView.emptyStorageCapacity(this));
        }
        ItemStorageView storage = ItemStorageView.storageAt(slot, hasEmpty, storages);
        if (storage == null) {
            return 0;
        }
        return storage.voidsOverflow() ? Integer.MAX_VALUE : ItemStorageView.toForgeLimit(storage.getCapacity());
    }

    /**
     * Bridges Forge insertion directly to routed storage. The supplied slot
     * is only a Forge compatibility argument and never selects a physical
     * drawer.
     *
     * @param slot     virtual slot index
     * @param stack    stack to insert
     * @param simulate whether to only report the result
     * @return the leftover stack, or {@code null} when everything fit
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
     * Bridges Forge extraction directly to the routed key represented by the
     * virtual slot. The physical drawer selected by the route is internal.
     *
     * @param slot     virtual slot index
     * @param amount   requested amount
     * @param simulate whether to only report the result
     * @return the extracted stack, or {@code null}
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

    /**
     * Checks insertion validity through a side-effect-free generic simulation.
     *
     * @param slot  virtual slot index
     * @param stack candidate stack
     * @return whether at least one item would be accepted
     */
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
     * Reports whether one internal index can represent the virtual empty
     * insertion slot. Aggregate handlers may override this to inspect the
     * owning child instead of their own aggregate lock state.
     *
     * @param index internal storage index
     * @return whether the index can accept an unconfigured insertion
     */
    default boolean isEmptyStorageAvailable(int index) {
        if (index < 0 || index >= Math.max(0, getStorageCount())) {
            return false;
        }
        BigItemStack snapshot = getSnapshot(index);
        return snapshot.isEmpty() && getCapacity(index) > 0L && (snapshot.hasTemplate() || !isLocked());
    }

    /**
     * Routes insertion through matching configured indices and then empty
     * indices. The generic index methods are the only state operations used.
     *
     * @param request requested resource and amount
     * @param action  execute or simulate
     * @return the routed result
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
        for (int pass = 0; pass < 3 && processedTotal < requested; pass++) {
            for (int index = 0; index < count && processedTotal < requested; index++) {
                BigItemStack current = getSnapshot(index);
                boolean hasTemplate = current.hasTemplate();
                boolean exact = hasTemplate && current.isSameType(request);
                if (pass == 0 && !exact) {
                    continue;
                }
                if (pass == 1) {
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
                if (pass == 2 && hasTemplate) {
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
     *
     * @param request requested resource and amount
     * @param action  execute or simulate
     * @return the routed result
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
