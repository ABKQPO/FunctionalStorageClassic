package com.hfstudio.functionalstorage.common.integration;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;

public class AdaptiveLinking {

    public static void tryLink(ControllableDrawerTile member) {
        World world = member.getWorldObj();
        if (world == null || world.isRemote || member.getAdaptiveMatrixPosition() != null) {
            return;
        }
        AdaptiveMatrixTile candidate = findUniqueMatrix(member, world);
        if (candidate != null) {
            candidate.linkDrawer(member.xCoord, member.yCoord, member.zCoord, false);
        }
    }

    public static AdaptiveMatrixTile resolveMatrix(ControllableDrawerTile member) {
        World world = member.getWorldObj();
        int[] position = member.getAdaptiveMatrixPosition();
        if (position == null) position = member.getControllerPosition();
        if (world == null || position == null || !world.blockExists(position[0], position[1], position[2])) return null;
        TileEntity linkedTile = world.getTileEntity(position[0], position[1], position[2]);
        if (linkedTile instanceof AdaptiveMatrixTile matrix) return validMatrixMember(member, matrix) ? matrix : null;
        if (!(linkedTile instanceof DrawerControllerTile controller) || controller.isInvalid()) return null;
        int[] matrixPosition = controller.getAdaptiveMatrixPosition();
        if (matrixPosition == null || !world.blockExists(matrixPosition[0], matrixPosition[1], matrixPosition[2])
            || !(world.getTileEntity(
                matrixPosition[0],
                matrixPosition[1],
                matrixPosition[2]) instanceof AdaptiveMatrixTile matrix)
            || matrix.isInvalid()
            || !controller.isLinkedToAdaptiveMatrix(matrix.xCoord, matrix.yCoord, matrix.zCoord)) {
            return null;
        }
        return validMatrixMember(member, matrix) ? matrix : null;
    }

    private static boolean validMatrixMember(ControllableDrawerTile member, AdaptiveMatrixTile matrix) {
        return isWithinRange(member, matrix) && matrix.isLinkedNetworkMember(member);
    }

    private static AdaptiveMatrixTile findUniqueMatrix(ControllableDrawerTile member, World world) {
        AdaptiveMatrixTile candidate = null;
        for (Object tile : world.loadedTileEntityList) {
            if (!(tile instanceof AdaptiveMatrixTile matrix) || matrix.isInvalid() || !inRange(member, matrix))
                continue;
            if (candidate != null && candidate != matrix) return null;
            candidate = matrix;
        }
        return candidate;
    }

    public static boolean isWithinRange(ControllableDrawerTile member, AdaptiveMatrixTile matrix) {
        return isWithinRange(member.xCoord, member.yCoord, member.zCoord, matrix);
    }

    public static boolean isWithinRange(int x, int y, int z, AdaptiveMatrixTile matrix) {
        int range = matrix.getLinkingRange();
        return Math.abs((long) x - matrix.xCoord) <= range && Math.abs((long) y - matrix.yCoord) <= range
            && Math.abs((long) z - matrix.zCoord) <= range;
    }

    private static boolean inRange(ControllableDrawerTile member, AdaptiveMatrixTile matrix) {
        return isWithinRange(member, matrix);
    }
}
