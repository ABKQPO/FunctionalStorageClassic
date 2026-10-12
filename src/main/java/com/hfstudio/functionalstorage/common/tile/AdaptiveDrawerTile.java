package com.hfstudio.functionalstorage.common.tile;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayEntry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayProvider;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplaySettings;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

public class AdaptiveDrawerTile extends WoodDrawerTile implements AdaptiveDisplayProvider {

    private boolean automaticLinkChecked;
    private final AdaptiveDisplaySettings displaySettings = new AdaptiveDisplaySettings();

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

    @Override
    public ControllableDrawerTile getDisplayOwner() {
        return this;
    }

    @Override
    public ItemStorageResource getDisplayResource() {
        return ItemStorageResource.INSTANCE;
    }

    @Override
    public AdaptiveDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    @Override
    protected int calculateClientModelKey() {
        return getAdaptiveModelKey();
    }

    @Override
    public boolean onSlotActivated(@NotNull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() == RegistrationHandler.configurationTool
            || held.getItem() == RegistrationHandler.adaptiveConfigurationTool
            || held.getItem() == RegistrationHandler.linkingTool
            || held.getItem() instanceof IStorageUpgrade) {
            return super.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
        }
        if (worldObj == null || worldObj.isRemote) return true;
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null || !matrix.isPowered()) return true;
        matrix.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
        matrix.sendStorageUpdate(player);
        return true;
    }

    @Override
    public void onSlotClicked(@NotNull EntityPlayer player, int slot) {
        if (worldObj == null || worldObj.isRemote) return;
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null || !matrix.isPowered()) return;
        List<AdaptiveDisplayEntry> entries = getAdaptiveDisplayEntries();
        if (slot < 0 || slot >= entries.size()) {
            return;
        }
        if (!(entries.get(slot)
            .content() instanceof BigItemStack content)) return;
        ItemStack template = content.getTemplate();
        if (template == null || content.getAmount() <= 0L) {
            return;
        }
        long amount = player.isSneaking() ? Math.max(1, template.getMaxStackSize()) : 1L;
        matrix.extractDisplayItem(player, template, amount);
    }

    @Override
    public @NotNull ItemStack createPickBlockStack(@NotNull ItemStack base) {
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
    protected void writeStorageData(@NotNull NBTTagCompound tag) {
        super.writeStorageData(tag);
        displaySettings.writeToNBT(tag);
    }

    @Override
    protected void readStorageData(@NotNull NBTTagCompound tag) {
        super.readStorageData(tag);
        displaySettings.readFromNBT(tag);
    }
}
