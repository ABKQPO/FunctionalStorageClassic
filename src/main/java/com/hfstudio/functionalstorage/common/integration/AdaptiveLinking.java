package com.hfstudio.functionalstorage.common.integration;

import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

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

    private static boolean inRange(ControllableDrawerTile member, AdaptiveMatrixTile matrix) {
        int range = matrix.getLinkingRange();
        return Math.abs(member.xCoord - matrix.xCoord) <= range && Math.abs(member.yCoord - matrix.yCoord) <= range
            && Math.abs(member.zCoord - matrix.zCoord) <= range;
    }
}
