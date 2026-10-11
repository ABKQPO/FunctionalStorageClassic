package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

public class AdaptiveFluidDrawerTile extends FluidDrawerTile {

    private boolean automaticLinkChecked;

    public AdaptiveFluidDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public AdaptiveFluidDrawerTile(@Nonnull DrawerLayout layout) {
        super(layout);
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            AdaptiveLinking.tryLink(this);
        }
        super.updateEntity();
    }

    public boolean isAdaptiveDisplayPowered() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        return matrix != null && matrix.isPowered();
    }

    @Override
    protected int getClientModelKey() {
        return isAdaptiveDisplayPowered() ? 1 : 0;
    }

    @Nullable
    private AdaptiveMatrixTile adaptiveMatrix() {
        int[] position = getAdaptiveMatrixPosition();
        if (position == null) position = getControllerPosition();
        if (worldObj == null || position == null || !worldObj.blockExists(position[0], position[1], position[2])) {
            return null;
        }
        return worldObj.getTileEntity(position[0], position[1], position[2]) instanceof AdaptiveMatrixTile matrix
            ? matrix
            : null;
    }
}
