package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * Controller extension tile. Holds the same drawer index as a controller but
 * forwards automation to the controller it is attached to, so a network can
 * cover more space without a second controller.
 */
public class ControllerExtensionTile extends TileEntity {

    private static final String KEY_CONTROLLER = "ControllerPos";

    private final Set<Long> drawers = new LinkedHashSet<>();
    private int controllerX = Integer.MIN_VALUE;
    private int controllerY = Integer.MIN_VALUE;
    private int controllerZ = Integer.MIN_VALUE;

    /**
     * Links a drawer to this extension.
     *
     * @param x drawer x
     * @param y drawer y
     * @param z drawer z
     */
    public void addDrawer(int x, int y, int z) {
        if (drawers.add(pack(x, y, z))) {
            markDirty();
        }
    }

    /**
     * Unlinks a drawer from this extension.
     *
     * @param x drawer x
     * @param y drawer y
     * @param z drawer z
     */
    public void removeDrawer(int x, int y, int z) {
        if (drawers.remove(pack(x, y, z))) {
            markDirty();
        }
    }

    /**
     * @return an unmodifiable view of the packed drawer coordinates
     */
    @Nonnull
    public Set<Long> getDrawers() {
        return Collections.unmodifiableSet(drawers);
    }

    /**
     * @return the controller coordinates as a three element array, or {@code null}
     */
    public int[] getControllerPosition() {
        return controllerX == Integer.MIN_VALUE ? null : new int[] { controllerX, controllerY, controllerZ };
    }

    /**
     * Binds this extension to a controller.
     *
     * @param x controller x
     * @param y controller y
     * @param z controller z
     */
    public void setControllerPosition(int x, int y, int z) {
        this.controllerX = x;
        this.controllerY = y;
        this.controllerZ = z;
        markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (controllerX != Integer.MIN_VALUE) {
            tag.setIntArray(KEY_CONTROLLER, new int[] { controllerX, controllerY, controllerZ });
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey(KEY_CONTROLLER)) {
            int[] position = tag.getIntArray(KEY_CONTROLLER);
            if (position.length == 3) {
                controllerX = position[0];
                controllerY = position[1];
                controllerZ = position[2];
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }
}
