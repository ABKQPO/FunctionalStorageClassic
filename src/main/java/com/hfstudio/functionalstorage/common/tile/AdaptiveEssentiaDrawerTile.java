package com.hfstudio.functionalstorage.common.tile;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayEntry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayProvider;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplaySettings;
import com.hfstudio.functionalstorage.common.storage.AspectStorageResource;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

public class AdaptiveEssentiaDrawerTile extends EssentiaDrawerTile implements AdaptiveDisplayProvider {

    private boolean automaticLinkChecked;
    private final AdaptiveDisplaySettings displaySettings = new AdaptiveDisplaySettings();

    public AdaptiveEssentiaDrawerTile() {
        super(DrawerLayout.X_4);
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            AdaptiveLinking.tryLink(this);
        }
        super.updateEntity();
    }

    @Override
    public ControllableDrawerTile getDisplayOwner() {
        return this;
    }

    @Override
    public AspectStorageResource getDisplayResource() {
        return AspectStorageResource.INSTANCE;
    }

    @Override
    public AdaptiveDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    @Override
    protected int getClientModelKey() {
        return getAdaptiveModelKey();
    }

    @Override
    public boolean onSlotActivated(@Nonnull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        return exchangeDisplayContainer(player, slot) || super.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
    }

    @Override
    public void onSlotClicked(@Nonnull EntityPlayer player, int slot) {
        exchangeDisplayContainer(player, slot);
    }

    private boolean exchangeDisplayContainer(EntityPlayer player, int slot) {
        if (!EssentiaContainerRegistry.isEssentiaContainer(player.getHeldItem())) return false;
        if (worldObj == null || worldObj.isRemote) return true;
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null || !matrix.isPowered()) return true;
        List<AdaptiveDisplayEntry> entries = getAdaptiveDisplayEntries();
        BigAspectStack selected = slot >= 0 && slot < entries.size()
            && entries.get(slot)
                .content() instanceof BigAspectStack content ? content : BigAspectStack.empty();
        EssentiaContainerRegistry.activate(player, matrix.getAspectHandler(), selected);
        matrix.sendStorageUpdate(player);
        return true;
    }

    @Override
    public ItemStack createPickBlockStack(ItemStack base) {
        return base;
    }

    @Override
    public boolean isEverythingEmpty() {
        return displaySettings.isEmpty() && super.isEverythingEmpty();
    }

    @Override
    public void markOptionsDirty() {
        super.markOptionsDirty();
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix != null) matrix.invalidateNetwork();
    }

    @Override
    protected void onLockChanging(boolean locked) {
        captureAdaptiveLock(locked);
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        super.writeStorageData(tag);
        displaySettings.writeToNBT(tag);
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        super.readStorageData(tag);
        displaySettings.readFromNBT(tag);
    }
}
