package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Wooden item drawer tile. Holds one long-capacity item slot per layout slot
 * and inherits ore-dictionary matching from the item handler when the
 * corresponding upgrade is installed.
 *
 * <p>
 * The tile implements vanilla's {@link IInventory} contract directly so
 * hoppers and other 1.7.10 automation can address it without a capability
 * lookup, which that version has no general mechanism for.
 * </p>
 */
public class WoodDrawerTile extends ControllableDrawerTile implements IInventory {

    private static final String KEY_ITEMS = "Items";

    private final DrawerLayout layout;
    private final BigItemHandler handler;

    public WoodDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public WoodDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = new BigItemHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ITEM_CAPACITY, 1D);
            }

            @Override
            public boolean isLocked() {
                return WoodDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return WoodDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return WoodDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return WoodDrawerTile.this.hasMaxStorage();
            }

            @Override
            protected boolean allowsEquivalentItems() {
                return WoodDrawerTile.this.hasEquivalentItems();
            }
        };
        bindStorageHandler(handler);
    }

    /**
     * @return the slot layout of this drawer
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_ITEMS, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_ITEMS, 10) ? tag.getCompoundTag(KEY_ITEMS) : null);
    }

    @Override
    protected int calculateRedstoneSignal() {
        return redstoneForRatio(fillRatio());
    }

    @Override
    protected void reconcileStorageConfiguration() {
        handler.applyLockConfiguration(isLocked());
    }

    @Override
    public int getSizeInventory() {
        return view().getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return view().getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return view().decrStackSize(index, count);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return view().getStackInSlotOnClosing(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        view().setInventorySlotContents(index, stack);
    }

    @Override
    public String getInventoryName() {
        return view().getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return view().hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return view().getInventoryStackLimit();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return view().isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        view().openInventory();
    }

    @Override
    public void closeInventory() {
        view().closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return view().isItemValidForSlot(index, stack);
    }

    @Nonnull
    private IInventory view() {
        IInventory inventory = getInventoryView();
        if (inventory == null) {
            throw new IllegalStateException("wood drawer must expose an item inventory view");
        }
        return inventory;
    }

    private double fillRatio() {
        long total = 0L;
        long capacity = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
            capacity += handler.getCapacity(index);
        }
        return capacity <= 0L ? 0D : Math.min(1D, total / (double) capacity);
    }
}
