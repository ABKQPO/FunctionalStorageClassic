package com.hfstudio.functionalstorage.common.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class DrawerModelStateCache {

    private static final ConcurrentMap<Position, Integer> STATES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Coordinate, Integer> COORDINATE_STATES = new ConcurrentHashMap<>();

    public static void publish(@Nullable World world, int x, int y, int z, int modelKey) {
        if (world == null || !world.isRemote) {
            return;
        }
        STATES.put(new Position(world.provider.dimensionId, x, y, z), modelKey);
        COORDINATE_STATES.put(new Coordinate(x, y, z), modelKey);
    }

    @Nullable
    public static Integer get(@Nullable IBlockAccess world, int x, int y, int z) {
        if (world instanceof World clientWorld) {
            return STATES.get(new Position(clientWorld.provider.dimensionId, x, y, z));
        }
        if (world == null) {
            return null;
        }
        return COORDINATE_STATES.get(new Coordinate(x, y, z));
    }

    public static void remove(@Nullable World world, int x, int y, int z) {
        if (world == null || !world.isRemote) {
            return;
        }
        STATES.remove(new Position(world.provider.dimensionId, x, y, z));
        COORDINATE_STATES.remove(new Coordinate(x, y, z));
    }

    private record Position(int dimension, int x, int y, int z) {}

    private record Coordinate(int x, int y, int z) {}
}
