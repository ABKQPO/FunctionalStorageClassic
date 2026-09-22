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
    private final BitSet handedOut = new BitSet();
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
        commitSlot(index);
        if (!valid(index)) {
            return null;
        }
        refresh(index);
        // The caller may edit the returned stack in place, which is how a hopper
        // moves items. Marking the slot here is what makes that edit visible; only
        // marked slots are examined when changes are committed.
        handedOut.set(index);
        return exposed[index];
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        if (!valid(index)) {
            return;
        }
        commitSlot(index);
        // A caller may pass back the same stack it just edited. Keep the original baseline.
        if (baseline[index] == null && exposed[index] == null) {
            refresh(index);
        }
        exposed[index] = stack == null ? null : stack.copy();
        handedOut.set(index);
        commitSlot(index);
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        commitSlot(index);
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
            long stackSize = Math.max(
                1,
                handler.getSnapshot(index)
                    .getTemplateStackSize());
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
        changeListener.run();
    }

    /**
     * Discards the cached insertion limit so the next read recomputes it.
     *
     * <p>
     * Only an upgrade, lock, or reload transition changes the capacity the limit
     * derives from, and the owning tile calls this from exactly those transitions.
     * Committing stored amounts must not call it: a transfer reads the limit once
     * per step, so dropping it there would rescan a whole aggregated network for
     * every stack moved.
     * </p>
     */
    public void invalidateLimit() {
        stackLimit = Integer.MIN_VALUE;
    }

    /**
     * Commits stack-size changes made directly by vanilla slots and hoppers.
     *
     * <p>
     * Only slots whose stack was actually handed to a caller since the last commit
     * are examined. A caller can only mutate a stack it received, so nothing is
     * missed, and the cost stays proportional to the work done rather than to the
     * size of the inventory. Rescanning every slot that was ever handed out would
     * make a full sweep of the inventory cost the square of its slot count, which is
     * exactly how an external storage bus reads a drawer.
     * </p>
     */
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

    /**
     * Commits any pending edit to one slot and resynchronizes it.
     *
     * <p>
     * Touching a single slot must not commit the whole inventory. A caller may hold a
     * stack it was handed while it reads another slot, and it may edit that first
     * stack afterwards: committing every outstanding slot on each read would clear the
     * mark on a stack the caller is still holding, and the later edit would be lost
     * with the storage keeping a stale amount. Committing only the slot being touched
     * leaves every other mark in place, and a stack that was merely read is unchanged,
     * so committing it costs one comparison.
     * </p>
     *
     * @param index slot whose pending edit must be committed
     */
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
        if (!ItemStack.areItemStacksEqual(before, after)) {
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
        }
        // Replacing a populated drawer with a different type would hide its reserve.
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
        // The exposed stack now agrees with the committed amount, so there is
        // nothing left to reconcile for this slot.
        handedOut.clear(index);
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
