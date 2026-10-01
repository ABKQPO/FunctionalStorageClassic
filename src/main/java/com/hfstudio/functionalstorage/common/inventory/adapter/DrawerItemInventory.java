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

import cpw.mods.fml.common.Optional;
import tconstruct.api.IExtendedStackLimitProvider;

/** Presents stable physical slots and commits vanilla's mutable stack edits as storage deltas. */
@Optional.Interface(iface = "tconstruct.api.IExtendedStackLimitProvider", modid = "TConstruct", striprefs = true)
public class DrawerItemInventory implements ISidedInventory, IExtendedStackLimitProvider {

    private static final int STACK_UNIT = 64;

    private final IBigItemHandler handler;
    private final String name;
    private final Runnable changeListener;
    private ItemStack[] exposed;
    private ItemStack[] baseline;
    private ItemStack[] inputExposed;
    private ItemStack[] inputBaseline;
    private ItemStack[] rollbackStacks;
    private long[] rollbackAmounts;
    private final BitSet handedOut = new BitSet();
    private final BitSet inputHandedOut = new BitSet();
    private int[] accessibleSlots;
    private boolean synchronizing;
    private int stackLimit = Integer.MIN_VALUE;

    public DrawerItemInventory(@Nonnull IBigItemHandler handler, @Nonnull String name,
        @Nonnull Runnable changeListener) {
        this.handler = handler;
        this.name = name;
        this.changeListener = changeListener;
        exposed = new ItemStack[handler.getStorageCount()];
        baseline = new ItemStack[exposed.length];
        inputExposed = new ItemStack[exposed.length];
        inputBaseline = new ItemStack[exposed.length];
        rollbackStacks = new ItemStack[exposed.length];
        rollbackAmounts = new long[exposed.length];
        accessibleSlots = new int[exposed.length * 2];
        for (int index = 0; index < accessibleSlots.length; index++) accessibleSlots[index] = index;
    }

    @Override
    public int getSizeInventory() {
        syncSlots();
        return accessibleSlots.length;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        syncSlots();
        if (isInputSlot(index)) {
            commitInput(index);
            refreshInput(index);
            inputHandedOut.set(index);
            return inputExposed[index];
        }
        index -= exposed.length;
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
        syncSlots();
        if (isInputSlot(index)) {
            setInput(index, stack);
            return;
        }
        index -= exposed.length;
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

    private void setInput(int index, @Nullable ItemStack stack) {
        boolean returnedStack = stack != null && stack == inputExposed[index] && inputHandedOut.get(index);
        commitInput(index);
        if (returnedStack) {
            return;
        }
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            refreshInput(index);
            return;
        }
        ItemStack before = inputBaseline[index];
        int amount = sameType(before, stack) ? stack.stackSize - count(before) : stack.stackSize;
        if (amount > 0) {
            insertExactly(index, stack, amount);
        }
        refreshInput(index);
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
        syncSlots();
        if (isInputSlot(index)) {
            return null;
        }
        index -= exposed.length;
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
            long smallest = STACK_UNIT;
            for (int index = 0, count = handler.getStorageCount(); index < count; index++) {
                long capacity = handler.getCapacity(index);
                if (capacity > 0L) {
                    long safeLimit = capacity < STACK_UNIT && !handler.getSnapshot(index)
                        .hasTemplate() ? 1L : capacity;
                    smallest = Math.min(smallest, safeLimit);
                }
            }
            stackLimit = (int) smallest;
        }
        return stackLimit;
    }

    @Override
    @Optional.Method(modid = "TConstruct")
    public int getExtendedStackLimit(int slot, @Nullable ItemStack stack) {
        syncSlots();
        if (stack == null || stack.getItem() == null) {
            return 0;
        }

        int storageCount = handler.getStorageCount();
        int storageIndex = slot < storageCount ? slot : slot - storageCount;
        if (storageIndex < 0 || storageIndex >= storageCount) {
            return 0;
        }
        if (handler.voidsOverflow(storageIndex)) {
            return Integer.MAX_VALUE;
        }

        long capacity = Math.max(0L, handler.getCapacity(storageIndex));
        if (slot < storageCount) {
            BigItemStack current = handler.getSnapshot(storageIndex);
            if (current.hasTemplate() && current.getAmount() > 0L) {
                long remaining = Math.max(0L, capacity - current.getAmount());
                ItemStack template = current.getTemplate();
                long unit = Math.min(capacity, Math.max(1, template.getMaxStackSize()));
                if (remaining < unit) {
                    capacity = Math.min(capacity, unit);
                }
            }
        }
        return capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
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

    public void invalidateSlots() {
        resetSlots(Math.max(0, handler.getStorageCount()));
    }

    public void flushChanges() {
        if (synchronizing || (handedOut.isEmpty() && inputHandedOut.isEmpty())) {
            return;
        }
        synchronizing = true;
        try {
            for (int index = inputHandedOut.nextSetBit(0); index >= 0; index = inputHandedOut.nextSetBit(index + 1)) {
                commitInputEdit(index);
            }
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

    private void commitInput(int index) {
        if (synchronizing || !inputHandedOut.get(index)) {
            return;
        }
        synchronizing = true;
        try {
            commitInputEdit(index);
        } finally {
            synchronizing = false;
        }
    }

    private void commitInputEdit(int index) {
        ItemStack before = inputBaseline[index];
        ItemStack after = inputExposed[index];
        int amount = sameType(before, after) ? count(after) - count(before) : count(after);
        if (amount > 0 && after != null) {
            insertExactly(index, after, amount);
        }
        refreshInput(index);
    }

    private void refreshInput(int index) {
        BigItemStack snapshot = handler.getSnapshot(index);
        ItemStack template = snapshot.getTemplate();
        ItemStack stack = null;
        if (template != null) {
            long capacity = Math.max(0L, handler.getCapacity(index));
            long remaining = Math.max(0L, capacity - snapshot.getAmount());
            int unit = (int) Math.min(capacity, Math.max(1, template.getMaxStackSize()));
            int count = (int) Math.max(0L, unit - remaining);
            if (count > 0) {
                stack = template.copy();
                stack.stackSize = count;
            }
        }
        inputExposed[index] = stack;
        inputBaseline[index] = stack == null ? null : stack.copy();
        inputHandedOut.clear(index);
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
        syncSlots();
        flushChanges();
        if (!isInputSlot(index) || stack.getItem() == null || stack.stackSize <= 0) {
            return false;
        }
        ItemStack current = handler.getSnapshot(index)
            .toItemStack();
        if (current != null && !sameType(current, stack)) {
            return false;
        }
        return handler.insert(index, new BigItemStack(stack, 1L), StorageAction.SIMULATE)
            .getProcessedAmount() > 0L;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        syncSlots();
        return accessibleSlots;
    }

    @Override
    public boolean canInsertItem(int index, @Nonnull ItemStack stack, int side) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, @Nonnull ItemStack stack, int side) {
        syncSlots();
        if (isInputSlot(index)) {
            return false;
        }
        index -= exposed.length;
        if (!valid(index) || stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return false;
        }
        BigItemStack current = handler.getSnapshot(index);
        return current.getAmount() > 0L && current.isSameType(stack);
    }

    @Nonnull
    public List<ItemStorageView> getViews() {
        syncSlots();
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
        stack.stackSize = (int) Math.min(snapshot.getAmount(), Integer.MAX_VALUE);
        return stack;
    }

    private boolean valid(int index) {
        return index >= 0 && index < exposed.length;
    }

    private void syncSlots() {
        int count = Math.max(0, handler.getStorageCount());
        if (count == exposed.length) {
            return;
        }
        resetSlots(count);
    }

    private void resetSlots(int count) {
        exposed = new ItemStack[count];
        baseline = new ItemStack[count];
        inputExposed = new ItemStack[count];
        inputBaseline = new ItemStack[count];
        rollbackStacks = new ItemStack[count];
        rollbackAmounts = new long[count];
        handedOut.clear();
        inputHandedOut.clear();
        accessibleSlots = new int[count * 2];
        for (int index = 0; index < accessibleSlots.length; index++) accessibleSlots[index] = index;
        invalidateLimit();
    }

    private static int count(ItemStack stack) {
        return stack == null ? 0 : Math.max(0, stack.stackSize);
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
        return index >= 0 && index < exposed.length;
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
