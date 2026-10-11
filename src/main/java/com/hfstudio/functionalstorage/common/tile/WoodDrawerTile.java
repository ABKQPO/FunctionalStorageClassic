package com.hfstudio.functionalstorage.common.tile;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayEntry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayProvider;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

public class WoodDrawerTile extends ControllableDrawerTile implements ISidedInventory, AdaptiveDisplayProvider {

    private static final String KEY_ITEMS = "Items";

    private DrawerLayout layout;
    private BigItemHandler handler;
    private int displayReservationMask;
    private final ItemStack[] displayReservations = new ItemStack[4];

    public WoodDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public WoodDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = createHandler();
        bindStorageHandler(handler);
    }

    private BigItemHandler createHandler() {
        return new BigItemHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                double multiplier = calculateModifier(UpgradeAttribute.ITEM_CAPACITY, 1D);
                return getUpgradeState().hasFeature(StorageFeature.IRON_DOWNGRADE)
                    ? multiplier / Math.max(1, FunctionalStorageConfig.STORAGE.baseItemCapacity)
                    : multiplier / layout.getSlotCount();
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
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Nullable
    @Override
    public List<AdaptiveDisplayEntry> getAdaptiveDisplayEntries() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        return matrix == null ? null
            : !matrix.isPowered() ? List.of()
                : matrix.getDisplayEntries(ConnectedDrawerScope.pack(xCoord, yCoord, zCoord));
    }

    @Nullable
    @Override
    public DrawerFaceLayout getAdaptiveDisplayLayout() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        return matrix == null ? null : matrix.getDisplayLayout(ConnectedDrawerScope.pack(xCoord, yCoord, zCoord));
    }

    public boolean handleAdaptiveActivation(EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held != null && (held.getItem() == RegistrationHandler.configurationTool
            || held.getItem() == RegistrationHandler.linkingTool
            || held.getItem() instanceof IStorageUpgrade)) {
            return false;
        }
        matrix.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
        matrix.sendStorageUpdate(player);
        return true;
    }

    public boolean handleAdaptiveClick(EntityPlayer player, int slot) {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null) {
            return false;
        }
        List<AdaptiveDisplayEntry> entries = getAdaptiveDisplayEntries();
        if (entries == null) {
            return false;
        }
        if (slot < 0 || slot >= entries.size()) {
            return true;
        }
        BigItemStack content = entries.get(slot)
            .content();
        ItemStack template = content.getTemplate();
        if (template == null || content.getAmount() <= 0L) {
            return true;
        }
        long amount = player.isSneaking() ? Math.max(1, template.getMaxStackSize()) : 1L;
        matrix.extractDisplayItem(player, template, amount);
        return true;
    }

    @Nullable
    private AdaptiveMatrixTile adaptiveMatrix() {
        int[] controller = getAdaptiveMatrixPosition();
        if (controller == null) {
            controller = getControllerPosition();
        }
        if (worldObj == null || controller == null
            || !worldObj.blockExists(controller[0], controller[1], controller[2])) {
            return null;
        }
        return worldObj.getTileEntity(controller[0], controller[1], controller[2]) instanceof AdaptiveMatrixTile matrix
            ? matrix
            : null;
    }

    @Override
    public int getRequestedDisplayCells() {
        int requested = switch (getDrawerOptions().getAdvancedValue(ConfigurationAction.ADAPTIVE_CELLS)) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> -1;
        };
        if (requested < 0) return requested;
        int minimum = displayReservationMask >= 4 ? 4 : displayReservationMask >= 2 ? 2 : 1;
        return Math.max(requested, minimum);
    }

    @Override
    public boolean isDisplayCellReserved(int cell) {
        return cell >= 0 && cell < displayReservations.length && (displayReservationMask & 1 << cell) != 0;
    }

    @Override
    public ItemStack getDisplayReservation(int cell) {
        ItemStack template = cell >= 0 && cell < displayReservations.length ? displayReservations[cell] : null;
        return template == null ? null : template.copy();
    }

    public boolean setDisplayReservation(int cell, ItemStack template, boolean reserved) {
        if (cell < 0 || cell >= displayReservations.length || worldObj != null && worldObj.isRemote) return false;
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (reserved && template != null && matrix != null && !matrix.canReserveDisplayItem(this, cell, template))
            return false;
        displayReservationMask = reserved ? displayReservationMask | 1 << cell : displayReservationMask & ~(1 << cell);
        displayReservations[cell] = reserved && template != null ? template.copy() : null;
        if (displayReservations[cell] != null) displayReservations[cell].stackSize = 1;
        markOptionsDirty();
        return true;
    }

    public String configureDisplayCell(int cell, boolean reserveEmpty) {
        if (adaptiveMatrix() == null || cell < 0 || cell >= 4) return "unlinked";
        if (!reserveEmpty && isDisplayCellReserved(cell)) {
            setDisplayReservation(cell, null, false);
            return "cleared";
        }
        List<AdaptiveDisplayEntry> entries = getAdaptiveDisplayEntries();
        ItemStack template = reserveEmpty || entries == null || cell >= entries.size() ? null
            : entries.get(cell)
                .content()
                .getTemplate();
        return setDisplayReservation(cell, template, true) ? template == null ? "empty" : "reserved" : "conflict";
    }

    @Override
    public boolean isEverythingEmpty() {
        return displayReservationMask == 0 && super.isEverythingEmpty();
    }

    @Override
    public void markOptionsDirty() {
        super.markOptionsDirty();
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix != null) matrix.invalidateNetwork();
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setString("DrawerLayout", layout.getId());
        tag.setTag(KEY_ITEMS, handler.serializeNBT());
        NBTTagList reservations = new NBTTagList();
        for (int cell = 0; cell < displayReservations.length; cell++) {
            if (!isDisplayCellReserved(cell)) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("Cell", cell);
            if (displayReservations[cell] != null)
                entry.setTag("Template", displayReservations[cell].writeToNBT(new NBTTagCompound()));
            reservations.appendTag(entry);
        }
        tag.setTag("AdaptiveReservations", reservations);
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        DrawerLayout restored = DrawerLayout.fromStorage(tag, KEY_ITEMS, layout);
        if (restored != layout) {
            layout = restored;
            handler = createHandler();
            bindStorageHandler(handler);
        }

        handler.deserializeNBT(tag.hasKey(KEY_ITEMS, 10) ? tag.getCompoundTag(KEY_ITEMS) : null);
        displayReservationMask = 0;
        for (int cell = 0; cell < displayReservations.length; cell++) displayReservations[cell] = null;
        NBTTagList reservations = tag.getTagList("AdaptiveReservations", 10);
        for (int index = 0; index < reservations.tagCount(); index++) {
            NBTTagCompound entry = reservations.getCompoundTagAt(index);
            int cell = entry.getInteger("Cell");
            if (cell < 0 || cell >= displayReservations.length) continue;
            displayReservationMask |= 1 << cell;
            displayReservations[cell] = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Template"));
        }
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

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ((ISidedInventory) view()).getAccessibleSlotsFromSide(side);
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, int side) {
        return ((ISidedInventory) view()).canInsertItem(index, stack, side);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, int side) {
        return ((ISidedInventory) view()).canExtractItem(index, stack, side);
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
