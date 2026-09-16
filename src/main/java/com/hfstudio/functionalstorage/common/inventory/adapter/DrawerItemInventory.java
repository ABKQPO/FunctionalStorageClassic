package com.hfstudio.functionalstorage.common.inventory.adapter;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/**
 * Exposes a long-capacity item drawer through vanilla's {@link ISidedInventory}
 * so hoppers, pipes, and other 1.7.10 automation can address it.
 *
 * <p>
 * Minecraft 1.7.10 has no {@code IItemHandler}; {@code IInventory} is the
 * only common item contract, so this adapter presents one virtual slot per
 * distinct stored item type. A leading empty slot is reserved while the drawer
 * can still accept an unconfigured insertion, which is how automated systems
 * discover that a new item type may be introduced.
 * </p>
 */
public class DrawerItemInventory implements ISidedInventory {

    private final IBigItemHandler handler;
    private final String name;
    private final Runnable changeListener;

    public DrawerItemInventory(@Nonnull IBigItemHandler handler, @Nonnull String name,
        @Nonnull Runnable changeListener) {
        this.handler = handler;
        this.name = name;
        this.changeListener = changeListener;
    }

    @Override
    public int getSizeInventory() {
        return ItemStorageView.storages(handler)
            .size() + offset();
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        if (hasEmptySlot() && index == 0) {
            return null;
        }
        ItemStorageView view = ItemStorageView.storageAt(index, hasEmptySlot(), ItemStorageView.storages(handler));
        return view == null ? null : view.toItemStack();
    }

    /**
     * Inserts into the drawer, ignoring the requested slot so a routed
     * insertion behaves like a normal inventory write.
     *
     * @param index destination slot, treated as a hint only
     * @param stack stack to store
     */
    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return;
        }
        transfer(stack, StorageAction.EXECUTE);
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        return count <= 0 ? null : extract(index, count, StorageAction.EXECUTE);
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
        return 64;
    }

    @Override
    public void markDirty() {
        changeListener.run();
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
        if (stack.getItem() == null) {
            return false;
        }
        return handler.insertRouted(new BigItemStack(stack, 1L), StorageAction.SIMULATE)
            .getProcessedAmount() > 0L;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int size = getSizeInventory();
        int[] slots = new int[size];
        for (int index = 0; index < size; index++) {
            slots[index] = index;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int index, @Nonnull ItemStack stack, int side) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, @Nonnull ItemStack stack, int side) {
        return true;
    }

    /**
     * @return the current aggregated views, for diagnostics and tooltips
     */
    @Nonnull
    public List<ItemStorageView> getViews() {
        return ItemStorageView.storages(handler);
    }

    private boolean hasEmptySlot() {
        return ItemStorageView.hasEmptyStorage(handler);
    }

    private int offset() {
        return hasEmptySlot() ? 1 : 0;
    }

    @Nullable
    private ItemStack transfer(@Nonnull ItemStack stack, @Nonnull StorageAction action) {
        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .insertRouted(new BigItemStack(stack, stack.stackSize), action);
        if (result.isComplete()) {
            return null;
        }
        ItemStack remainder = stack.copy();
        remainder.stackSize = (int) Math.min(result.getRemainingAmount(), stack.stackSize);
        return remainder;
    }

    @Nullable
    private ItemStack extract(int index, int count, @Nonnull StorageAction action) {
        ItemStack template = getStackInSlot(index);
        if (template == null) {
            return null;
        }
        int requested = Math.min(count, Math.max(1, template.getMaxStackSize()));
        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .extractRouted(new BigItemStack(template, requested), action);
        return result.getProcessed()
            .toItemStack();
    }
}
