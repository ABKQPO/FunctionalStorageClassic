package com.hfstudio.functionalstorage.common.tile;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayEntry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayProvider;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

public class AdaptiveDrawerTile extends WoodDrawerTile implements AdaptiveDisplayProvider {

    private boolean automaticLinkChecked;
    private int displayReservationMask;
    private final ItemStack[] displayReservations = new ItemStack[4];

    public AdaptiveDrawerTile() {
        super(DrawerLayout.X_4);
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            tryAutoLink();
        }
        super.updateEntity();
    }

    public void tryAutoLink() {
        AdaptiveLinking.tryLink(this);
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

    public boolean isAdaptiveDisplayPowered() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        return matrix != null && matrix.isPowered();
    }

    public int getAdaptiveModelKey() {
        DrawerFaceLayout layout = getAdaptiveDisplayLayout();
        return (layout == null ? 0 : layout.getSlotCount()) << 1 | (isAdaptiveDisplayPowered() ? 1 : 0);
    }

    @Override
    protected int getClientModelKey() {
        return getAdaptiveModelKey();
    }

    public boolean handleAdaptiveActivation(EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held != null && (held.getItem() == RegistrationHandler.configurationTool
            || held.getItem() == RegistrationHandler.adaptiveConfigurationTool
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
    public AdaptiveMatrixTile adaptiveMatrix() {
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
    protected void writeStorageData(@NotNull NBTTagCompound tag) {
        super.writeStorageData(tag);
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
    protected void readStorageData(@NotNull NBTTagCompound tag) {
        super.readStorageData(tag);
        displayReservationMask = 0;
        Arrays.fill(displayReservations, null);
        NBTTagList reservations = tag.getTagList("AdaptiveReservations", 10);
        for (int index = 0; index < reservations.tagCount(); index++) {
            NBTTagCompound entry = reservations.getCompoundTagAt(index);
            int cell = entry.getInteger("Cell");
            if (cell < 0 || cell >= displayReservations.length) continue;
            displayReservationMask |= 1 << cell;
            displayReservations[cell] = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Template"));
        }
    }
}
