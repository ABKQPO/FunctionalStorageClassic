package com.hfstudio.functionalstorage.common.inventory.adapter;

import java.util.BitSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

/** Presents stable physical slots and commits vanilla's mutable stack edits as storage deltas. */
public class DrawerItemInventory implements ISidedInventory {

    private static final int STACK_UNIT = 64;

    private final IBigItemHandler handler;
    private final String name;
    private final Runnable changeListener;
    private final ItemStack[] exposed;
    private final ItemStack[] baseline;
    private final ItemStack[] rollbackStacks;
    private final long[] rollbackAmounts;
    private final BitSet handedOut = new BitSet();
    private final int inputSlot;
    private final int[] accessibleSlots;
    private boolean synchronizing;
    private int stackLimit = Integer.MIN_VALUE;

    public DrawerItemInventory(@Nonnull IBigItemHandler handler, @Nonnull String name,
        @Nonnull Runnable changeListener) {
        this.handler = handler;
        this.name = name;
        this.changeListener = changeListener;
        exposed = new ItemStack[handler.getStorageCount()];
        baseline = new ItemStack[exposed.length];
        rollbackStacks = new ItemStack[exposed.length];
        rollbackAmounts = new long[exposed.length];
        inputSlot = exposed.length;
        accessibleSlots = new int[inputSlot + 1];
        for (int index = 0; index < accessibleSlots.length; index++) accessibleSlots[index] = index;
    }

    @Override
    public int getSizeInventory() {
        return accessibleSlots.length;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        if (isInputSlot(index)) {
            return null;
        }
        commitSlot(index);
        if (!valid(index)) {
            return null;
        }
        clearRollback(index);
        refresh(index);
        handedOut.set(index);
        return exposed[index];
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        if (isInputSlot(index)) {
            insertInput(stack);
            return;
        }
        if (!valid(index)) {
            return;
        }
        boolean returnedStack = stack != null && stack == exposed[index]
            && baseline[index] != null
            && handedOut.get(index);
        if (restoreExtraction(index, stack)) {
            return;
        }
        clearRollback(index);
        commitSlot(index);
        if (returnedStack) {
            return;
        }
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            removeDisplayedStack(index);
            return;
        }
        if (baseline[index] == null && exposed[index] == null) {
            refresh(index);
        }
        exposed[index] = stack.copy();
        handedOut.set(index);
        commitSlot(index);
    }

    private void insertInput(@Nullable ItemStack stack) {
        if (!acceptsInput(stack)) {
            return;
        }
        handler.insertRouted(new BigItemStack(stack, stack.stackSize), StorageAction.EXECUTE);
    }

    private void removeDisplayedStack(int index) {
        clearRollback(index);
        if (baseline[index] == null && exposed[index] == null) {
            refresh(index);
        }
        ItemStack displayed = baseline[index];
        long amount = count(displayed);
        if (displayed != null && amount > 0L) {
            extractExactly(index, amount);
        }
        refresh(index);
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        commitSlot(index);
        if (!valid(index) || count <= 0) {
            return null;
        }
        clearRollback(index);
        BigItemStack snapshot = handler.getSnapshot(index);
        if (!snapshot.hasTemplate() || snapshot.getAmount() <= 0L) {
            refresh(index);
            return null;
        }
        int requested = count;
        BigItemStack extracted = handler.extract(index, requested, StorageAction.EXECUTE)
            .getProcessed();
        ItemStack rollback = extracted.toItemStack();
        if (extracted.getAmount() > 0L && rollback != null) {
            rollbackStacks[index] = rollback.copy();
            rollbackAmounts[index] = extracted.getAmount();
        }
        refresh(index);
        return extracted.toItemStack();
    }

    @Nullable
    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public String getInventoryName() {
        return name;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        if (stackLimit == Integer.MIN_VALUE) {
            stackLimit = calculateStackLimit();
        }
        return stackLimit;
    }

    private int calculateStackLimit() {
        long smallestUnit = Long.MAX_VALUE;
        int count = Math.max(0, handler.getStorageCount());
        boolean measured = false;
        for (int index = 0; index < count; index++) {
            long capacity = Math.max(0L, handler.getCapacity(index));
            if (capacity <= 0L) {
                continue;
            }
            int stackSize = Math.max(
                1,
                handler.getSnapshot(index)
                    .getTemplateStackSize());
            long normalizedCapacity = capacity > Long.MAX_VALUE / STACK_UNIT ? Long.MAX_VALUE
                : capacity * STACK_UNIT / stackSize;
            smallestUnit = Math.min(smallestUnit, normalizedCapacity);
            measured = true;
        }
        if (!measured) {
            return STACK_UNIT;
        }
        long limit = smallestUnit > Integer.MAX_VALUE / (long) STACK_UNIT ? Integer.MAX_VALUE
            : smallestUnit * STACK_UNIT;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, limit));
    }

    @Override
    public void markDirty() {
        flushChanges();
        clearRollbacks();
        changeListener.run();
    }

    public void invalidateLimit() {
        stackLimit = Integer.MIN_VALUE;
    }

    public void flushChanges() {
        if (synchronizing || handedOut.isEmpty()) {
            return;
        }
        synchronizing = true;
        try {
            for (int index = handedOut.nextSetBit(0); index >= 0; index = handedOut.nextSetBit(index + 1)) {
                commit(index);
            }
        } finally {
            synchronizing = false;
        }
    }

    private void commitSlot(int index) {
        if (synchronizing || !valid(index) || !handedOut.get(index)) {
            return;
        }
        synchronizing = true;
        try {
            commit(index);
        } finally {
            synchronizing = false;
        }
    }

    /**
     * Applies one slot's pending edit and resynchronizes the exposed stack.
     *
     * @param index slot to commit
     */
    private void commit(int index) {
        ItemStack before = baseline[index];
        ItemStack after = exposed[index];
        if (!sameStack(before, after)) {
            int oldCount = count(before);
            int newCount = count(after);
            if (sameType(before, after)) {
                int delta = newCount - oldCount;
                if (delta > 0) {
                    insertExactly(index, after, delta);
                } else if (delta < 0) {
                    extractExactly(index, -delta);
                }
            } else if (newCount == 0 && oldCount > 0) {
                extractExactly(index, oldCount);
            } else if (oldCount == 0 && newCount > 0) {
                insertExactly(index, after, newCount);
            }
        }
        refresh(index);
    }

    @Override
    public boolean isUseableByPlayer(@Nonnull EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, @Nonnull ItemStack stack) {
        flushChanges();
        if (isInputSlot(index)) {
            return acceptsInput(stack);
        }
        if (!valid(index) || stack.getItem() == null || stack.stackSize <= 0) {
            return false;
        }
        if (valid(index)) {
            handedOut.set(index);
        }
        ItemStack current = valid(index) ? handler.getSnapshot(index)
            .toItemStack() : null;
        if (current != null && !sameType(current, stack)) {
            return false;
        }
        long requested = current == null ? stack.stackSize : 1L;
        return handler.insert(index, new BigItemStack(stack, requested), StorageAction.SIMULATE)
            .getProcessedAmount() == requested;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return accessibleSlots;
    }

    @Override
    public boolean canInsertItem(int index, @Nonnull ItemStack stack, int side) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, @Nonnull ItemStack stack, int side) {
        return valid(index);
    }

    @Nonnull
    public List<ItemStorageView> getViews() {
        flushChanges();
        return ItemStorageView.storages(handler);
    }

    private void refresh(int index) {
        BigItemStack snapshot = handler.getSnapshot(index);
        ItemStack stack = toInventoryStack(snapshot);
        if (!sameStack(stack, baseline[index])) {
            exposed[index] = stack;
            baseline[index] = stack == null ? null : stack.copy();
        } else if (!sameStack(exposed[index], baseline[index])) {
            exposed[index] = stack;
        }
        // The exposed stack now agrees with the committed amount, so there is
        // nothing left to reconcile for this slot.
        handedOut.clear(index);
    }

    private boolean restoreExtraction(int index, @Nullable ItemStack stack) {
        long amount = rollbackAmounts[index];
        ItemStack original = rollbackStacks[index];
        if (amount <= 0L || original == null
            || stack == null
            || !sameType(original, stack)
            || stack.stackSize > original.stackSize) {
            return false;
        }
        long restored = stack.stackSize == original.stackSize ? amount : Math.min(amount, Math.max(0, stack.stackSize));
        if (restored > 0L) {
            handler.insert(index, new BigItemStack(stack, restored), StorageAction.EXECUTE);
        }
        clearRollback(index);
        refresh(index);
        return true;
    }

    private void clearRollbacks() {
        for (int index = 0; index < rollbackAmounts.length; index++) {
            clearRollback(index);
        }
    }

    private void clearRollback(int index) {
        rollbackAmounts[index] = 0L;
        rollbackStacks[index] = null;
    }

    @Nullable
    private ItemStack toInventoryStack(@Nonnull BigItemStack snapshot) {
        ItemStack stack = snapshot.toItemStack();
        if (stack == null) {
            return null;
        }
        int limit = Math.min(stack.getMaxStackSize(), getInventoryStackLimit());
        int visibleLimit = Math.max(1, limit);
        stack.stackSize = (int) Math.min(snapshot.getAmount(), visibleLimit);
        return stack;
    }

    private boolean valid(int index) {
        return index >= 0 && index < exposed.length;
    }

    private static int count(ItemStack stack) {
        return stack == null ? 0 : Math.max(0, stack.stackSize);
    }

    private boolean acceptsInput(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return false;
        }
        long requested = stack.stackSize;
        return handler.insertRouted(new BigItemStack(stack, requested), StorageAction.SIMULATE)
            .getProcessedAmount() == requested;
    }

    private boolean insertExactly(int index, @Nonnull ItemStack stack, long amount) {
        if (amount <= 0L) {
            return true;
        }
        BigItemStack request = new BigItemStack(stack, amount);
        if (handler.insert(index, request, StorageAction.SIMULATE)
            .getProcessedAmount() != amount) {
            return false;
        }
        return handler.insert(index, request, StorageAction.EXECUTE)
            .getProcessedAmount() == amount;
    }

    private boolean extractExactly(int index, long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (handler.extract(index, amount, StorageAction.SIMULATE)
            .getProcessedAmount() != amount) {
            return false;
        }
        return handler.extract(index, amount, StorageAction.EXECUTE)
            .getProcessedAmount() == amount;
    }

    private boolean isInputSlot(int index) {
        return index == inputSlot;
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        int firstCount = count(first);
        if (firstCount != count(second)) {
            return false;
        }
        return firstCount == 0 || sameType(first, second);
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.isItemEqual(second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }
}
