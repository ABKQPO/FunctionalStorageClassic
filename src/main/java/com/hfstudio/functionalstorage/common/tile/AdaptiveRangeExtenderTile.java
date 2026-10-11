package com.hfstudio.functionalstorage.common.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class AdaptiveRangeExtenderTile extends ControllableDrawerTile {

    private boolean automaticLinkChecked;

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
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public int getUtilityUpgradeSlots() {
        return 0;
    }

    @Override
    public boolean openGui(EntityPlayer player) {
        return false;
    }

    @Override
    protected void writeStorageData(NBTTagCompound tag) {}

    @Override
    protected void readStorageData(NBTTagCompound tag) {}
}
