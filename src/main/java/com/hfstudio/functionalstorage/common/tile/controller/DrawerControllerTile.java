package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * Storage controller. Drawers link themselves to a controller within the
 * configured radius and the controller exposes their combined contents to
 * automation as a single inventory.
 */
public class DrawerControllerTile extends TileEntity {

    private final Set<Long> drawers = new LinkedHashSet<>();

    /**
     * Links a drawer to this controller.
     *
     * @param x drawer x
     * @param y drawer y
     * @param z drawer z
     */
    public void addDrawer(int x, int y, int z) {
        if (drawers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit) {
            return;
        }
        if (drawers.add(pack(x, y, z))) {
            markDirty();
        }
    }

    /**
     * Unlinks a drawer from this controller.
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
     * @return the number of linked drawers
     */
    public int getDrawerCount() {
        return drawers.size();
    }

    /**
     * @return an unmodifiable view of the packed drawer coordinates
     */
    @Nonnull
    public Set<Long> getDrawers() {
        return Collections.unmodifiableSet(drawers);
    }

    /**
     * @return the effective linking radius including upgrade contributions
     */
    public int getLinkingRange() {
        return Math.max(1, FunctionalStorageConfig.GENERAL.drawerControllerLinkingRange);
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }
}
