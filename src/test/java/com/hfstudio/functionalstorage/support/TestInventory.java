package com.hfstudio.functionalstorage.support;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A plain inventory of fixed slots, used as the far side of a transfer.
 * <p>
 * Transfers are written against Forge's inventory interface, so exercising them needs
 * an ordinary inventory and not storage of this mod's own kind. This one behaves like
 * a chest: it honours the stack limit, merges into a matching slot, and refuses a
 * slot holding a different item.
 */
public class TestInventory implements IInventory {

    private final ItemStack[] slots;
    private final int stackLimit;

    public TestInventory(int size) {
        this(size, 64);
    }

    public TestInventory(int size, int stackLimit) {
        this.slots = new ItemStack[Math.max(0, size)];
        this.stackLimit = Math.max(1, stackLimit);
    }

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index < 0 || index >= slots.length ? null : slots[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack current = getStackInSlot(index);
        if (current == null || count <= 0) {
            return null;
        }
        ItemStack taken = current.splitStack(Math.min(count, current.stackSize));
        if (current.stackSize <= 0) {
            slots[index] = null;
        }
        return taken.stackSize <= 0 ? null : taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= slots.length) {
            return;
        }
        slots[index] = stack == null || stack.getItem() == null ? null : stack;
    }

    @Override
    public String getInventoryName() {
        return "functionalstorage:test";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return stackLimit;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index >= 0 && index < slots.length && stack != null && stack.getItem() != null;
    }

    /**
     * Sums how many of one item this inventory holds across every slot.
     *
     * @param item item to total
     * @return the summed amount
     */
    public int totalOf(Item item) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getItem() == item) {
                total += Math.max(0, stack.stackSize);
            }
        }
        return total;
    }

    /**
     * Counts how many slots currently hold anything.
     *
     * @return the number of populated slots
     */
    public int populatedSlots() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getItem() != null && stack.stackSize > 0) {
                count++;
            }
        }
        return count;
    }
}
