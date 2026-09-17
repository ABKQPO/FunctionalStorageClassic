package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class ControllerExtensionTile extends StorageNetworkTile {

    private DrawerControllerTile controller() {
        int[] position = getControllerPosition();
        if (worldObj == null || position == null || !worldObj.blockExists(position[0], position[1], position[2]))
            return null;
        return worldObj.getTileEntity(position[0], position[1], position[2]) instanceof DrawerControllerTile controller
            ? controller
            : null;
    }

    @Override
    protected List<ControllableDrawerTile> connectedDrawers() {
        DrawerControllerTile controller = controller();
        return controller == null ? List.of() : controller.connectedDrawers();
    }

    @Override
    public void setControllerPosition(int x, int y, int z) {
        super.setControllerPosition(x, y, z);
        invalidateNetwork();
    }

    @Override
    public void clearControllerPosition() {
        super.clearControllerPosition();
        invalidateNetwork();
    }

    public void addDrawer(int x, int y, int z) {
        DrawerControllerTile controller = controller();
        if (controller != null) controller.linkDrawer(x, y, z, false);
        invalidateNetwork();
    }

    public void removeDrawer(int x, int y, int z) {
        DrawerControllerTile controller = controller();
        if (controller != null) controller.linkDrawer(x, y, z, true);
        invalidateNetwork();
    }

    public Set<Long> getDrawers() {
        DrawerControllerTile controller = controller();
        return controller == null ? Collections.emptySet() : controller.getDrawers();
    }

    @Override
    protected void writeStorageData(NBTTagCompound tag) {}

    @Override
    protected void readStorageData(NBTTagCompound tag) {
        invalidateNetwork();
    }

    @Override
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public int getUtilityUpgradeSlots() {
        return 0;
    }
}
