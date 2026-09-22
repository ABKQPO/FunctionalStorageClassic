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

    private final IBigItemHandler handler;
    private final String name;
    private final Runnable changeListener;
    private final ItemStack[] exposed;
    private final ItemStack[] baseline;
    private final BitSet observed = new BitSet();
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
        accessibleSlots = new int[exposed.length];
        for (int index = 0; index < accessibleSlots.length; index++) accessibleSlots[index] = index;
    }

    @Override
    public int getSizeInventory() {
        return exposed.length;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        flushChanges();
        if (!valid(index)) {
            return null;
        }
        refresh(index);
        return exposed[index];
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        if (!valid(index)) {
            return;
        }
        // A caller may pass back the same stack it just edited. Keep the original baseline.
        if (baseline[index] == null && exposed[index] == null) {
            refresh(index);
        }
        exposed[index] = stack == null ? null : stack.copy();
        observed.set(index);
        flushChanges();
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        flushChanges();
        if (!valid(index) || count <= 0) {
            return null;
        }
        ItemStack current = getStackInSlot(index);
        if (current == null) {
            return null;
        }
        ItemStack result = handler.extract(index, Math.min(count, current.stackSize), StorageAction.EXECUTE)
            .getProcessed()
            .toItemStack();
        refresh(index);
        return result;
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
        int count = Math.max(0, handler.getStorageCount());
        if (count == 0) {
            return 64;
        }
        long unit = Long.MAX_VALUE;
        boolean measured = false;
        for (int index = 0; index < count; index++) {
            long capacity = Math.max(0L, handler.getCapacity(index));
            if (capacity <= 0L) {
                continue;
            }
            ItemStack template = handler.getSnapshot(index)
                .getTemplate();
            int stackSize = template == null ? 64 : Math.max(1, template.getMaxStackSize());
            unit = Math.min(unit, capacity / stackSize);
            measured = true;
        }
        if (!measured) {
            return 64;
        }
        long limit = unit > Integer.MAX_VALUE / 64L ? Integer.MAX_VALUE : unit * 64L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, limit));
    }

    @Override
    public void markDirty() {
        flushChanges();
        stackLimit = Integer.MIN_VALUE;
        changeListener.run();
    }

    /** Commits stack-size changes made directly by vanilla slots and hoppers. */
    public void flushChanges() {
        if (synchronizing) {
            return;
        }
        synchronizing = true;
        try {
            for (int index = observed.nextSetBit(0); index >= 0; index = observed.nextSetBit(index + 1)) {
                ItemStack before = baseline[index];
                ItemStack after = exposed[index];
                if (ItemStack.areItemStacksEqual(before, after)) {
                    continue;
                }
                int oldCount = count(before);
                int newCount = count(after);
                if (sameType(before, after)) {
                    int delta = newCount - oldCount;
                    if (delta > 0) {
                        handler.insert(index, new BigItemStack(after, delta), StorageAction.EXECUTE);
                    } else if (delta < 0) {
                        handler.extract(index, -delta, StorageAction.EXECUTE);
                    }
                } else if (oldCount == 0) {
                    handler.insert(index, new BigItemStack(after, newCount), StorageAction.EXECUTE);
                } else if (newCount == 0) {
                    handler.extract(index, oldCount, StorageAction.EXECUTE);
                }
                // Replacing a populated drawer with a different type would hide its reserve.
                refresh(index);
            }
        } finally {
            synchronizing = false;
        }
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
        ItemStack current = valid(index) ? handler.getSnapshot(index)
            .toItemStack() : null;
        if (current != null && !sameType(current, stack)) {
            return false;
        }
        return valid(index) && stack.getItem() != null
            && handler.insert(index, new BigItemStack(stack, 1L), StorageAction.SIMULATE)
                .getProcessedAmount() > 0L;
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
        ItemStack stack = snapshot.toItemStack();
        // Keep the external stack count equal to the stored amount. StorageDrawers
        // uses the inventory stack limit for insertion, not for truncating reads.
        if (!ItemStack.areItemStacksEqual(stack, baseline[index])) {
            exposed[index] = stack;
            baseline[index] = stack == null ? null : stack.copy();
        } else if (!ItemStack.areItemStacksEqual(exposed[index], baseline[index])) {
            exposed[index] = stack;
        }
        observed.set(index, exposed[index] != null || baseline[index] != null);
    }

    private boolean valid(int index) {
        return index >= 0 && index < exposed.length;
    }

    private static int count(ItemStack stack) {
        return stack == null ? 0 : Math.max(0, stack.stackSize);
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first != null && second != null
            && first.isItemEqual(second)
            && ItemStack.areItemStackTagsEqual(first, second);
    }
}
