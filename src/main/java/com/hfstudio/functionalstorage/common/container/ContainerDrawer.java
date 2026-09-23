package com.hfstudio.functionalstorage.common.container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.serverutilities.ServerUtilitiesIntegration;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.interaction.ContainerExchange;
import com.hfstudio.functionalstorage.common.interaction.FluidContainerInteraction;
import com.hfstudio.functionalstorage.common.inventory.adapter.UpgradeSlotInventory;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;

import lombok.Getter;

/** Physical storage and upgrade slots backed by the owning drawer. */
public class ContainerDrawer extends Container implements MenuSettingsReceiver, StorageTransferMenu, GhostFilterMenu {

    private static final int PLAYER_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;
    private static final int MAX_VISIBLE_STORAGE_SLOTS = 36;

    private final ControllableDrawerTile tile;
    @Getter
    private final DrawerGuiLayout layout;
    private final IInventory openedInventory;
    private final int storageSlotCount;
    private final InventoryBasic display;
    private final StorageSubscription subscription;
    private boolean displayDirty = true;
    private boolean stateDirty = true;

    public ContainerDrawer(@Nonnull ControllableDrawerTile tile, @Nonnull EntityPlayer player) {
        this.tile = tile;

        this.openedInventory = tile instanceof StorageNetworkTile ? null : tile.getInventoryView();
        this.storageSlotCount = tile instanceof StorageNetworkTile || tile.getActiveStorage() == null ? 0
            : Math.min(
                tile.getActiveStorage()
                    .getStorageCount(),
                MAX_VISIBLE_STORAGE_SLOTS);
        this.display = new InventoryBasic("Drawer", false, storageSlotCount);
        this.subscription = storageSlotCount == 0 || tile.getWorldObj().isRemote ? StorageSubscription.CLOSED
            : tile.getActiveStorage()
                .subscribe(change -> {
                    displayDirty = true;
                    stateDirty = true;
                });
        int storageSlots = visibleStorageSlots();
        this.layout = new DrawerGuiLayout(storageSlots, ((DrawerBlock) tile.getBlockType()).getFaceLayout());
        IInventory storage = display;
        for (int index = 0; index < storageSlots; index++) {
            addSlotToContainer(new StorageSlot(storage, index, layout.storageX(index), layout.storageY(index)));
        }

        refreshDisplay();
        int upgradeY = layout.upgradeY();
        IInventory storageUpgrades = UpgradeSlotInventory.of(tile, true);
        for (int slot = 0; slot < tile.getStorageUpgradeSlots(); slot++) {
            addSlotToContainer(new UpgradeSlot(storageUpgrades, slot, true, 10 + slot * 18, upgradeY));
        }
        IInventory utilityUpgrades = UpgradeSlotInventory.of(tile, false);
        for (int slot = 0; slot < tile.getUtilityUpgradeSlots(); slot++) {
            addSlotToContainer(new UpgradeSlot(utilityUpgrades, slot, false, 114 + slot * 18, upgradeY));
        }

        int playerTop = layout.inventoryY();
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

    @Nonnull
    public ControllableDrawerTile getTile() {
        return tile;
    }

    public int getVisibleStorageSlots() {
        return visibleStorageSlots();
    }

    @Override
    public IBigItemHandler getTransferStorage() {
        return tile.getItemHandler();
    }

    @Override
    public int getTransferSlotCount() {
        return storageSlotCount;
    }

    @Override
    public int getTransferSlot(int menuSlot) {
        return menuSlot >= 0 && menuSlot < storageSlotCount ? menuSlot : -1;
    }

    @Override
    public void applySettings(EntityPlayer player, int value, String text) {
        if (!(tile instanceof StorageNetworkTile) && canInteractWith(player)) tile.setPriority(value);
    }

    @Override
    public void applyFilter(EntityPlayer player, int slot, ItemStack template) {
        if (canInteractWith(player) && slot >= 0 && slot < storageSlotCount && tile.setItemFilter(slot, template))
            detectAndSendChanges();
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        int slot = id - GuiHandler.GUI_UPGRADE_BASE;
        if (!canInteractWith(player) || slot < 0 || slot >= tile.getUtilityUpgradeSlots()) {
            return false;
        }
        ItemStack stack = tile.getUtilityUpgrade(slot);
        if (stack != null && stack.getItem() instanceof RedstoneUpgradeItem upgrade) {
            upgrade.cycleSlot(
                stack,
                tile.getActiveStorage() == null ? 1
                    : tile.getActiveStorage()
                        .getStorageCount());
            tile.markOptionsDirty();
            return true;
        }
        if (stack == null || !(stack.getItem() instanceof AutomationUpgradeItem)) {
            return false;
        }
        player.openGui(FunctionalStorage.instance, id, tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        return true;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return (tile instanceof StorageNetworkTile || tile.getInventoryView() == openedInventory)
            && tile.getWorldObj() != null
            && !ServerUtilitiesIntegration.blocksInteraction(player, tile.xCoord, tile.yCoord, tile.zCoord)
            && tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && player.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) <= 64D;
    }

    @Override
    public void detectAndSendChanges() {
        if (stateDirty && storageSlotCount > 0 && !tile.getWorldObj().isRemote && !crafters.isEmpty()) {
            stateDirty = false;
            Packet update = tile.getDescriptionPacket();
            for (Object crafter : crafters) {
                if (crafter instanceof EntityPlayerMP player && player.playerNetServerHandler != null) {
                    player.playerNetServerHandler.sendPacket(update);
                }
            }
        }
        refreshDisplay();
        super.detectAndSendChanges();
    }

    private void refreshDisplay() {
        if (tile.getWorldObj().isRemote || tile.getItemHandler() == null) return;
        if (!displayDirty && !subscription.isClosed()) return;
        displayDirty = false;
        for (int index = 0; index < storageSlotCount; index++) {
            BigItemStack stored = tile.getItemHandler()
                .getSnapshot(index);
            ItemStack icon = stored.isEmpty() ? null : stored.getTemplate();
            if (!ItemStack.areItemStacksEqual(display.getStackInSlot(index), icon)) {
                display.setInventorySlotContents(index, icon);
            }
        }
    }

    @Override
    public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
        if (!canInteractWith(player)) return null;
        boolean storage = index >= 0 && index < storageSlotCount;
        if (mode == 5) return super.slotClick(index, button, mode, player);
        if (!storage && mode != 1 && mode != 6) return super.slotClick(index, button, mode, player);
        if (tile.getWorldObj().isRemote) return null;
        func_94533_d();
        if (mode == 1 && (button == 0 || button == 1)) {
            transferStackInSlot(player, index);
        } else if (mode == 6 && (button == 0 || button == 1)) {
            collectToCursor(player);
            super.slotClick(index, button, mode, player);
        } else if (storage && mode == 0 && (button == 0 || button == 1)) {
            clickStorage(player, index, button);
        } else if (storage && tile.getItemHandler() != null) {
            clickItemShortcut(player, index, button, mode);
        }
        synchronize(player);
        return null;
    }

    private void clickStorage(EntityPlayer player, int index, int button) {
        ItemStack cursor = player.inventory.getItemStack();
        IBigItemHandler handler = tile.getItemHandler();
        if (handler != null) {
            if (cursor == null) {
                ItemStack available = extract(index, 64, StorageAction.SIMULATE);
                if (available != null) player.inventory.setItemStack(
                    extract(
                        index,
                        button == 1 ? (available.stackSize + 1) / 2 : available.stackSize,
                        StorageAction.EXECUTE));
            } else {
                insert(index, cursor, button == 1 ? 1 : cursor.stackSize);
                if (cursor.stackSize == 0) player.inventory.setItemStack(null);
            }
        } else if (tile.getFluidHandler() != null) {
            FluidContainerInteraction.activate(
                cursor,
                tile.getFluidHandler(),
                index,
                result -> ContainerExchange.completeCursor(player, result));
        } else if (tile.getAspectHandler() != null) {
            EssentiaContainerRegistry.activate(
                cursor,
                tile.getAspectHandler(),
                index,
                result -> ContainerExchange.completeCursor(player, result));
        }
    }

    private void clickItemShortcut(EntityPlayer player, int index, int button, int mode) {
        if (mode == 2 && button >= 0 && button < 9) {
            ItemStack hotbar = player.inventory.getStackInSlot(button);
            if (hotbar == null)
                player.inventory.setInventorySlotContents(button, extract(index, 64, StorageAction.EXECUTE));
            else {
                insert(index, hotbar, hotbar.stackSize);
                if (hotbar.stackSize == 0) player.inventory.setInventorySlotContents(button, null);
            }
        } else if (mode == 3 && player.capabilities.isCreativeMode && player.inventory.getItemStack() == null) {
            ItemStack copy = extract(index, 64, StorageAction.SIMULATE);
            if (copy != null) {
                copy.stackSize = copy.getMaxStackSize();
                player.inventory.setItemStack(copy);
            }
        } else if (mode == 4 && player.inventory.getItemStack() == null && (button == 0 || button == 1)) {
            ItemStack dropped = extract(index, button == 0 ? 1 : 64, StorageAction.EXECUTE);
            if (dropped != null) player.dropPlayerItemWithRandomChoice(dropped, true);
        }
    }

    private void collectToCursor(EntityPlayer player) {
        ItemStack cursor = player.inventory.getItemStack();
        IBigItemHandler handler = tile.getItemHandler();
        if (cursor == null || handler == null) return;
        int space = cursor.getMaxStackSize() - cursor.stackSize;
        if (space > 0)
            cursor.stackSize += (int) handler.extractRouted(new BigItemStack(cursor, space), StorageAction.EXECUTE)
                .getProcessedAmount();
    }

    private void insert(int index, ItemStack stack, int amount) {
        stack.stackSize -= (int) tile.getItemHandler()
            .insert(index, new BigItemStack(stack, amount), StorageAction.EXECUTE)
            .getProcessedAmount();
    }

    private ItemStack extract(int index, int amount, StorageAction action) {
        IBigItemHandler handler = tile.getItemHandler();
        ItemStack template = handler.getSnapshot(index)
            .getTemplate();
        return template == null ? null
            : handler.extract(index, Math.min(amount, template.getMaxStackSize()), action)
                .getProcessed()
                .toItemStack();
    }

    private void synchronize(EntityPlayer player) {
        player.inventory.markDirty();
        detectAndSendChanges();
        // Vanilla suppresses cursor updates after accepting a click transaction.
        if (player instanceof EntityPlayerMP serverPlayer && serverPlayer.playerNetServerHandler != null) {
            serverPlayer.updateHeldItem();
        }
    }

    @Override
    public boolean canDragIntoSlot(Slot slot) {
        return !(slot instanceof StorageSlot);
    }

    @Override
    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return stack == null || !(slot instanceof StorageSlot);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        subscription.close();
        super.onContainerClosed(player);
    }

    @Nullable
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        if (tile.getWorldObj().isRemote || !canInteractWith(player) || index < 0 || index >= inventorySlots.size())
            return null;
        int playerStart = storageSlotCount + tile.getStorageUpgradeSlots() + tile.getUtilityUpgradeSlots();
        if (index < storageSlotCount) {
            if (tile.getItemHandler() == null) return null;
            ItemStack available = extract(index, 64, StorageAction.SIMULATE);
            if (available == null) return null;
            ItemStack original = available.copy();
            if (!mergeItemStack(available, playerStart, inventorySlots.size(), true)) return null;
            extract(index, original.stackSize - available.stackSize, StorageAction.EXECUTE);
            return original;
        }
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < playerStart) {
            mergeItemStack(stack, playerStart, inventorySlots.size(), true);
        } else {
            if (stack.getItem() instanceof IStorageUpgrade) {
                moveUpgradesToSlots(stack, storageSlotCount, playerStart);
            }
            if (stack.stackSize > 0 && tile.getItemHandler() != null) {
                stack.stackSize -= (int) tile.getItemHandler()
                    .insertRouted(new BigItemStack(stack, stack.stackSize), StorageAction.EXECUTE)
                    .getProcessedAmount();
            }
        }
        if (stack.stackSize == original.stackSize) return null;
        if (stack.stackSize <= 0) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    private void moveUpgradesToSlots(ItemStack stack, int start, int end) {
        for (int targetIndex = start; targetIndex < end && stack.stackSize > 0; targetIndex++) {
            Slot target = inventorySlots.get(targetIndex);
            if (target.getHasStack() || !target.isItemValid(stack)) {
                continue;
            }
            ItemStack single = stack.splitStack(1);
            target.putStack(single);
            target.onSlotChanged();
        }
    }

    private int visibleStorageSlots() {
        return storageSlotCount;
    }

    /** A synchronized icon; storage mutations belong to the container click handler. */
    public static class StorageSlot extends Slot {

        public StorageSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return false;
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
            boolean storageUpgrade = ((IStorageUpgrade) stack.getItem()).isStorageUpgrade();
            return storage == storageUpgrade && inventory.isItemValidForSlot(getSlotIndex(), stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return ((UpgradeSlotInventory) inventory).canRemove(getSlotIndex());
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
