package com.hfstudio.functionalstorage.util;

import javax.annotation.Nonnull;

import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.block.DrawerAttachment;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;

/**
 * Geometry helpers that convert between the drawer's logical front face and
 * the physical hit position on the block.
 */
public class HitBoxesUtil {

    public static final ForgeDirection[] HORIZONTAL = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
        ForgeDirection.WEST };

    private HitBoxesUtil() {}

    public static int horizontalIndex(@Nonnull ForgeDirection facing) {
        for (int index = 0; index < HORIZONTAL.length; index++) {
            if (HORIZONTAL[index] == facing) {
                return index;
            }
        }
        return 0;
    }

    @Nonnull
    public static ForgeDirection horizontalFromQuadrant(int quadrant) {
        return HORIZONTAL[((quadrant % 4) + 4) % 4];
    }

    @Nonnull
    public static DrawerAttachment attachmentForFace(int side) {
        if (side == ForgeDirection.UP.ordinal()) {
            return DrawerAttachment.FLOOR;
        }
        if (side == ForgeDirection.DOWN.ordinal()) {
            return DrawerAttachment.CEILING;
        }
        return DrawerAttachment.WALL;
    }

    /**
     * Maps a local hit position on the front face to a drawer slot index.
     *
     * @param layout     face layout of the drawer
     * @param attachment attachment surface
     * @param facing     horizontal rotation
     * @param localX     local hit x within the block
     * @param localY     local hit y within the block
     * @param localZ     local hit z within the block
     * @return the slot index, or {@code -1} when no slot region was hit
     */
    public static int resolveSlot(@Nonnull DrawerFaceLayout layout, @Nonnull DrawerAttachment attachment,
        @Nonnull ForgeDirection facing, double localX, double localY, double localZ) {
        ForgeDirection front = switch (attachment) {
            case FLOOR -> ForgeDirection.UP;
            case CEILING -> ForgeDirection.DOWN;
            case WALL -> facing;
        };
        ForgeDirection up = switch (attachment) {
            case FLOOR -> facing.getOpposite();
            case CEILING -> facing;
            case WALL -> ForgeDirection.UP;
        };
        int rightX = up.offsetY * front.offsetZ - up.offsetZ * front.offsetY;
        int rightY = up.offsetZ * front.offsetX - up.offsetX * front.offsetZ;
        int rightZ = up.offsetX * front.offsetY - up.offsetY * front.offsetX;
        double x = localX - 0.5D;
        double y = localY - 0.5D;
        double z = localZ - 0.5D;
        double horizontal = 0.5D + x * rightX + y * rightY + z * rightZ;
        double vertical = 0.5D - x * up.offsetX - y * up.offsetY - z * up.offsetZ;
        return layout.slotAt(horizontal, vertical);
    }
}
