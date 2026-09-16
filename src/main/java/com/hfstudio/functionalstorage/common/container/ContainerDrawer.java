package com.hfstudio.functionalstorage.common.container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.inventory.adapter.UpgradeSlotInventory;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UtilityUpgradeItem;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Container for a controllable drawer. Exposes the drawer's visible storage
 * slots, its upgrade slots, and the player inventory, so upgrades can be
 * installed and swapped without breaking the block.
 */
public class ContainerDrawer extends Container {

    private static final int PLAYER_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;
    private static final int MAX_VISIBLE_STORAGE_SLOTS = 36;

    private final ControllableDrawerTile tile;

    public ContainerDrawer(@Nonnull ControllableDrawerTile tile, @Nonnull EntityPlayer player) {
        this.tile = tile;

        int storageSlots = visibleStorageSlots();
        IInventory storage = storageInventory();
        for (int index = 0; index < storageSlots; index++) {
            addSlotToContainer(new StorageSlot(storage, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18));
        }

        int upgradeY = 18 + ((storageSlots + 8) / 9) * 18 + 6;
        IInventory storageUpgrades = UpgradeSlotInventory.of(tile, true);
        for (int slot = 0; slot < tile.getStorageUpgradeSlots(); slot++) {
            addSlotToContainer(new UpgradeSlot(storageUpgrades, slot, true, 8 + slot * 18, upgradeY));
        }
        IInventory utilityUpgrades = UpgradeSlotInventory.of(tile, false);
        for (int slot = 0; slot < tile.getUtilityUpgradeSlots(); slot++) {
            addSlotToContainer(
                new UpgradeSlot(
                    utilityUpgrades,
                    slot,
                    false,
                    8 + (tile.getStorageUpgradeSlots() + slot) * 18,
                    upgradeY));
        }

        int playerTop = upgradeY + 24;
        for (int row = 0; row < PLAYER_ROWS; row++) {
            for (int column = 0; column < PLAYER_COLUMNS; column++) {
                addSlotToContainer(
                    new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, playerTop + row * 18));
            }
        }
        for (int column = 0; column < PLAYER_COLUMNS; column++) {
            addSlotToContainer(new Slot(player.inventory, column, 8 + column * 18, playerTop + 58));
        }
    }

    /**
     * @return the owning drawer
     */
    @Nonnull
    public ControllableDrawerTile getTile() {
        return tile;
    }

    /**
     * @return the number of visible storage slots in this container
     */
    public int getVisibleStorageSlots() {
        return visibleStorageSlots();
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return true;
    }

    @Nullable
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return null;
        }
        Slot slot = (Slot) inventorySlots.get(index);
        if (!slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        int storageSlots = visibleStorageSlots();
        int upgradeSlots = tile.getStorageUpgradeSlots() + tile.getUtilityUpgradeSlots();
        int upgradeStart = storageSlots;
        int playerStart = storageSlots + upgradeSlots;

        boolean moved;
        if (index < playerStart) {
            moved = mergeItemStack(stack, playerStart, inventorySlots.size(), true);
        } else if (stack.getItem() instanceof IStorageUpgrade || stack.getItem() instanceof AutomationUpgradeItem) {
            moved = mergeItemStack(stack, upgradeStart, playerStart, false)
                || mergeItemStack(stack, 0, storageSlots, false);
        } else {
            moved = mergeItemStack(stack, 0, storageSlots, false);
        }
        if (!moved) {
            return null;
        }

        if (stack.stackSize <= 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }

    @Nullable
    private IInventory storageInventory() {
        return tile.getInventoryView();
    }

    private int visibleStorageSlots() {
        IInventory inventory = storageInventory();
        return inventory == null ? 0 : Math.min(inventory.getSizeInventory(), MAX_VISIBLE_STORAGE_SLOTS);
    }

    /**
     * Slot that reads and writes the drawer through its aggregated inventory
     * view, so one visible slot can represent a long amount.
     */
    public static class StorageSlot extends Slot {

        public StorageSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return inventory.isItemValidForSlot(slotNumber, stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
        }
    }

    /**
     * Slot that admits only upgrades of the matching group.
     */
    public static class UpgradeSlot extends Slot {

        private final boolean storage;

        public UpgradeSlot(IInventory inventory, int index, boolean storage, int x, int y) {
            super(inventory, index, x, y);
            this.storage = storage;
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (stack.getItem() == null || !(stack.getItem() instanceof IStorageUpgrade)) {
                return false;
            }
            boolean utility = stack.getItem() instanceof UtilityUpgradeItem
                || stack.getItem() instanceof AutomationUpgradeItem;
            return storage != utility;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
