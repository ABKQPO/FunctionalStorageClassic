package com.hfstudio.functionalstorage.common.container;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;

public class ContainerArmory extends Container implements MenuSettingsReceiver {

    public static final int COLUMNS = 8;
    public static final int VISIBLE_SLOTS = 32;
    private final ArmoryCabinetTile tile;
    private final InventoryBasic display = new InventoryBasic("Armory", false, VISIBLE_SLOTS);
    private final List<Integer> filtered = new ArrayList<>();
    private final StorageSubscription subscription;
    private String query = "";
    private int row;
    private int maxRow;
    private int lastRow = -1;
    private int lastMaxRow = -1;
    private boolean dirty = true;
    private BitSet searchMatches;
    private int searchRevision;

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        crafter.sendProgressBarUpdate(this, 0, row);
        crafter.sendProgressBarUpdate(this, 1, maxRow);
    }

    public ContainerArmory(ArmoryCabinetTile tile, EntityPlayer player) {
        this.tile = tile;
        for (int slot = 0; slot < VISIBLE_SLOTS; slot++)
            addSlotToContainer(new ArmorySlot(slot, 8 + slot % COLUMNS * 18, 19 + slot / COLUMNS * 18));
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++)
            addSlotToContainer(new Slot(player.inventory, 9 + y * 9 + x, 8 + x * 18, 102 + y * 18));
        for (int x = 0; x < 9; x++) addSlotToContainer(new Slot(player.inventory, x, 8 + x * 18, 160));
        subscription = tile.getItemHandler()
            .subscribe(change -> {
                dirty = true;
                searchRevision++;
            });
        rebuild();
    }

    public int getScrollRow() {
        return row;
    }

    public int getMaxScrollRow() {
        return maxRow;
    }

    public int getSearchRevision() {
        return searchRevision;
    }

    public ArmoryCabinetTile getTile() {
        return tile;
    }

    public void applySearch(int requestedRow, String text, int[] matches) {
        query = text.trim()
            .toLowerCase(Locale.ROOT);
        searchMatches = query.isEmpty() ? null
            : new BitSet(
                tile.getItemHandler()
                    .getStorageCount());
        if (searchMatches != null) for (int slot : matches) {
            if (slot >= 0 && slot < tile.getItemHandler()
                .getStorageCount()) searchMatches.set(slot);
        }
        rebuild();
        row = Math.max(0, Math.min(requestedRow, maxRow));
        detectAndSendChanges();
    }

    private void rebuild() {
        if (tile.getWorldObj().isRemote) return;
        filtered.clear();
        for (int index = 0; index < tile.getItemHandler()
            .getStorageCount(); index++) {
            if (query.isEmpty()) {
                filtered.add(index);
                continue;
            }
            BigItemStack stored = tile.getItemHandler()
                .getSnapshot(index);
            if (stored.hasTemplate()) {
                if (searchMatches != null) {
                    if (searchMatches.get(index)) filtered.add(index);
                    continue;
                }
                ItemStack item = stored.getTemplate();
                String name = item.getDisplayName() + " " + Item.itemRegistry.getNameForObject(item.getItem());
                if (name.toLowerCase(Locale.ROOT)
                    .contains(query)) filtered.add(index);
            }
        }
        maxRow = Math.max(0, (filtered.size() + COLUMNS - 1) / COLUMNS - 4);
        row = Math.min(row, maxRow);
        dirty = false;
    }

    @Override
    public void applySettings(EntityPlayer player, int value, String text) {
        if (!canInteractWith(player)) return;
        String next = text.toLowerCase(Locale.ROOT)
            .trim();
        if (!query.equals(next)) {
            query = next;
            searchMatches = null;
            row = 0;
            dirty = true;
        }
        if (dirty) rebuild();
        row = Math.max(0, Math.min(value, maxRow));
        detectAndSendChanges();
    }

    @Override
    public void detectAndSendChanges() {
        if (dirty) rebuild();
        super.detectAndSendChanges();
        if (row == lastRow && maxRow == lastMaxRow) return;
        for (Object object : crafters) {
            ICrafting crafter = (ICrafting) object;
            crafter.sendProgressBarUpdate(this, 0, row);
            crafter.sendProgressBarUpdate(this, 1, maxRow);
        }
        lastRow = row;
        lastMaxRow = maxRow;
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) row = value;
        else if (id == 1) maxRow = value;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        subscription.close();
        super.onContainerClosed(player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.getWorldObj()
            .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (tile.getWorldObj().isRemote) return null;
        if (index < 0 || index >= inventorySlots.size()) return null;
        Slot slot = inventorySlots.get(index);
        ItemStack stack = slot.getStack();
        if (stack == null) return null;
        ItemStack original = stack.copy();
        if (index < VISIBLE_SLOTS) {
            if (!mergeItemStack(stack, VISIBLE_SLOTS, inventorySlots.size(), true)) return null;
        } else {
            ItemStack remainder = tile.getItemHandler()
                .insertItem(0, stack, false);
            if (remainder != null && remainder.stackSize == stack.stackSize) return null;
            stack.stackSize = remainder == null ? 0 : remainder.stackSize;
        }
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    public class ArmorySlot extends Slot {

        private final int visible;

        public ArmorySlot(int visible, int x, int y) {
            super(display, visible, x, y);
            this.visible = visible;
        }

        private int index() {
            int index = row * COLUMNS + visible;
            return index < filtered.size() ? filtered.get(index) : -1;
        }

        @Override
        public ItemStack getStack() {
            if (tile.getWorldObj().isRemote) return super.getStack();
            int index = index();
            return index < 0 ? null
                : tile.getInventoryView()
                    .getStackInSlot(index);
        }

        @Override
        public void putStack(ItemStack stack) {
            if (tile.getWorldObj().isRemote) super.putStack(stack);
            else if (index() >= 0) tile.getInventoryView()
                .setInventorySlotContents(index(), stack);
            onSlotChanged();
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            if (tile.getWorldObj().isRemote) return super.decrStackSize(amount);
            return index() < 0 ? null
                : tile.getInventoryView()
                    .decrStackSize(index(), amount);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return (tile.getWorldObj().isRemote || index() >= 0) && stack.getMaxStackSize() == 1;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        @Override
        public void onSlotChanged() {
            if (!tile.getWorldObj().isRemote) tile.getInventoryView()
                .markDirty();
        }
    }
}
