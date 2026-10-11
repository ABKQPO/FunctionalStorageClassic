package com.hfstudio.functionalstorage.common.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class AdaptiveCraftingGridTile extends AdaptiveGridTile {

    private final CraftingInventory crafting = new CraftingInventory(this);
    private long craftingRevision;
    private boolean loadingCrafting;

    public CraftingInventory getCraftingInventory() {
        return crafting;
    }

    public long getCraftingRevision() {
        return craftingRevision;
    }

    @Override
    public boolean isEverythingEmpty() {
        for (int index = 0; index < crafting.getSizeInventory(); index++) {
            if (crafting.getStackInSlot(index) != null) return false;
        }
        return super.isEverythingEmpty();
    }

    @Override
    protected void writeStorageData(NBTTagCompound tag) {
        super.writeStorageData(tag);
        NBTTagList entries = new NBTTagList();
        for (int index = 0; index < crafting.getSizeInventory(); index++) {
            ItemStack stack = crafting.getStackInSlot(index);
            if (stack == null) continue;
            NBTTagCompound entry = stack.writeToNBT(new NBTTagCompound());
            entry.setByte("Slot", (byte) index);
            entries.appendTag(entry);
        }
        NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Entries", entries);
        tag.setTag("Crafting", inventory);
    }

    @Override
    protected void readStorageData(NBTTagCompound tag) {
        super.readStorageData(tag);
        loadingCrafting = true;
        try {
            for (int index = 0; index < crafting.getSizeInventory(); index++) {
                crafting.setInventorySlotContents(index, null);
            }
            NBTTagList entries = tag.getCompoundTag("Crafting")
                .getTagList("Entries", 10);
            for (int index = 0; index < entries.tagCount(); index++) {
                NBTTagCompound entry = entries.getCompoundTagAt(index);
                int slot = entry.getByte("Slot") & 255;
                ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
                if (slot < crafting.getSizeInventory() && stack != null && stack.stackSize > 0) {
                    crafting.setInventorySlotContents(slot, stack);
                }
            }
        } finally {
            loadingCrafting = false;
            craftingRevision++;
        }
    }

    @Override
    public void onBlockBroken() {
        if (worldObj != null && !worldObj.isRemote) {
            for (int index = 0; index < crafting.getSizeInventory(); index++) {
                ItemStack stack = crafting.getStackInSlotOnClosing(index);
                if (stack != null && !FunctionalStorageConfig.GENERAL.keepContentsOnBreak) {
                    worldObj.spawnEntityInWorld(
                        new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, stack));
                }
            }
        }
        super.onBlockBroken();
    }

    public static class CraftingInventory extends InventoryBasic {

        private final AdaptiveCraftingGridTile owner;

        public CraftingInventory(AdaptiveCraftingGridTile owner) {
            super("container.crafting", false, 9);
            this.owner = owner;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int index) {
            return decrStackSize(index, getInventoryStackLimit());
        }

        @Override
        public void markDirty() {
            owner.craftingRevision++;
            if (!owner.loadingCrafting && owner.worldObj != null && !owner.worldObj.isRemote) {
                owner.worldObj.markTileEntityChunkModified(owner.xCoord, owner.yCoord, owner.zCoord, owner);
            }
            super.markDirty();
        }
    }
}
