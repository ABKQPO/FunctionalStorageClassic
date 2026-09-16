package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class DrawerControllerTile extends TileEntity {

    private final Set<Long> drawers = new LinkedHashSet<>();

    public void addDrawer(int x, int y, int z) {
        if (drawers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit) {
            return;
        }
        if (drawers.add(pack(x, y, z))) {
            markDirty();
        }
    }

    public void removeDrawer(int x, int y, int z) {
        if (drawers.remove(pack(x, y, z))) {
            markDirty();
        }
    }

    public int getDrawerCount() {
        return drawers.size();
    }

    @Nonnull
    public Set<Long> getDrawers() {
        return Collections.unmodifiableSet(drawers);
    }

    public int getLinkingRange() {
        return Math.max(1, FunctionalStorageConfig.GENERAL.drawerControllerLinkingRange);
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }
}
