package com.hfstudio.functionalstorage.common.inventory.adapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Read and write view over one group of a drawer's upgrade slots, so vanilla
 * {@code Slot} instances can be attached to them without the tile having to
 * implement {@code IInventory}.
 */
public class UpgradeSlotInventory implements IInventory {

    private final ControllableDrawerTile tile;
    private final boolean storage;

    public UpgradeSlotInventory(@Nonnull ControllableDrawerTile tile, boolean storage) {
        this.tile = tile;
        this.storage = storage;
    }

    /**
     * Creates the view for a drawer's storage or utility upgrade slots.
     *
     * @param tile    owning drawer
     * @param storage {@code true} for storage upgrade slots
     * @return the inventory view
     */
    @Nonnull
    public static UpgradeSlotInventory of(@Nonnull ControllableDrawerTile tile, boolean storage) {
        return new UpgradeSlotInventory(tile, storage);
    }

    @Override
    public int getSizeInventory() {
        return storage ? tile.getStorageUpgradeSlots() : tile.getUtilityUpgradeSlots();
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        return storage ? tile.getStorageUpgrade(index) : tile.getUtilityUpgrade(index);
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack current = getStackInSlot(index);
        if (current == null || count <= 0) {
            return null;
        }
        ItemStack taken = current.splitStack(count);
        if (current.stackSize <= 0) {
            setInventorySlotContents(index, null);
        } else {
            setInventorySlotContents(index, current);
        }
        return taken;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        tile.setUpgradeSlot(storage, index, stack);
    }

    @Override
    public String getInventoryName() {
        return storage ? "container.functionalstorage.storage_upgrades"
            : "container.functionalstorage.utility_upgrades";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        tile.onUpgradeSlotChanged(storage, 0);
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
        return stack.getItem() != null;
    }
}
