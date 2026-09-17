package com.hfstudio.functionalstorage.common.inventory.adapter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public interface DrawerInventoryAccess extends IInventory {

    IInventory getInventoryView();

    @Override
    default int getSizeInventory() {
        return getInventoryView().getSizeInventory();
    }

    @Override
    default ItemStack getStackInSlot(int slot) {
        return getInventoryView().getStackInSlot(slot);
    }

    @Override
    default ItemStack decrStackSize(int slot, int count) {
        return getInventoryView().decrStackSize(slot, count);
    }

    @Override
    default ItemStack getStackInSlotOnClosing(int slot) {
        return getInventoryView().getStackInSlotOnClosing(slot);
    }

    @Override
    default void setInventorySlotContents(int slot, ItemStack stack) {
        getInventoryView().setInventorySlotContents(slot, stack);
    }

    @Override
    default String getInventoryName() {
        return getInventoryView().getInventoryName();
    }

    @Override
    default boolean hasCustomInventoryName() {
        return getInventoryView().hasCustomInventoryName();
    }

    @Override
    default int getInventoryStackLimit() {
        return getInventoryView().getInventoryStackLimit();
    }

    @Override
    default boolean isUseableByPlayer(EntityPlayer player) {
        return getInventoryView().isUseableByPlayer(player);
    }

    @Override
    default void openInventory() {
        getInventoryView().openInventory();
    }

    @Override
    default void closeInventory() {
        getInventoryView().closeInventory();
    }

    @Override
    default boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getInventoryView().isItemValidForSlot(slot, stack);
    }
}
