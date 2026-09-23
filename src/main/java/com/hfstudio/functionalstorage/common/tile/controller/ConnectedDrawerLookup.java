package com.hfstudio.functionalstorage.common.tile.controller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.World;

import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;

public class ConnectedDrawerLookup {

    @Nonnull
    public static ConnectedDrawerScope scopeOf(@Nonnull World world, int x, int y, int z, int maxRadius) {
        int[] controller = controllerPosition(world, x, y, z);
        return controller == null ? ConnectedDrawerScope.empty()
            : controllerScope(world, controller[0], controller[1], controller[2], maxRadius);
    }

    @Nonnull
    public static ConnectedDrawerScope controllerScope(@Nonnull World world, int x, int y, int z, int maxRadius) {
        if (!world.blockExists(x, y, z)) {
            return ConnectedDrawerScope.empty();
        }
        return world.getTileEntity(x, y, z) instanceof DrawerControllerTile controller
            ? controller.connectedScope(maxRadius)
            : ConnectedDrawerScope.empty();
    }

    @Nullable
    public static int[] controllerPositionOf(@Nonnull World world, int x, int y, int z) {
        return controllerPosition(world, x, y, z);
    }

    @Nullable
    private static int[] controllerPosition(@Nonnull World world, int x, int y, int z) {
        if (!world.blockExists(x, y, z)) {
            return null;
        }
        return world.getTileEntity(x, y, z) instanceof StorageNetworkTile network ? network.getControllerPosition()
            : null;
    }
}
