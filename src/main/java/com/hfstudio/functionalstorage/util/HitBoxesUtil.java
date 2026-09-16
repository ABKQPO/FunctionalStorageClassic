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

    /**
     * @param facing horizontal facing
     * @return the index of the facing in {@link #HORIZONTAL}, or zero
     */
    public static int horizontalIndex(@Nonnull ForgeDirection facing) {
        for (int index = 0; index < HORIZONTAL.length; index++) {
            if (HORIZONTAL[index] == facing) {
                return index;
            }
        }
        return 0;
    }

    /**
     * Converts a vanilla rotation quadrant into a horizontal facing.
     *
     * @param quadrant rotation quadrant
     * @return the matching facing
     */
    @Nonnull
    public static ForgeDirection horizontalFromQuadrant(int quadrant) {
        return HORIZONTAL[((quadrant % 4) + 4) % 4];
    }

    /**
     * Maps a clicked block face to the drawer attachment it implies.
     *
     * @param side clicked face ordinal
     * @return the resulting attachment
     */
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
        double horizontal;
        double vertical;

        if (attachment == DrawerAttachment.WALL) {
            vertical = 1D - localY;
            switch (facing) {
                case NORTH:
                    horizontal = localX;
                    break;
                case SOUTH:
                    horizontal = 1D - localX;
                    break;
                case WEST:
                    horizontal = localZ;
                    break;
                case EAST:
                    horizontal = 1D - localZ;
                    break;
                default:
                    return -1;
            }
        } else {
            double depth = attachment == DrawerAttachment.FLOOR ? localY : 1D - localY;
            if (depth < 0.5D) {
                return -1;
            }
            switch (facing) {
                case NORTH:
                    horizontal = localX;
                    vertical = 1D - localZ;
                    break;
                case SOUTH:
                    horizontal = 1D - localX;
                    vertical = localZ;
                    break;
                case WEST:
                    horizontal = localZ;
                    vertical = localX;
                    break;
                case EAST:
                    horizontal = 1D - localZ;
                    vertical = 1D - localX;
                    break;
                default:
                    return -1;
            }
        }

        switch (layout) {
            case X_1:
                return 0;
            case X_2:
                return vertical < 0.5D ? 0 : 1;
            case X_4:
                boolean right = horizontal >= 0.5D;
                boolean bottom = vertical >= 0.5D;
                if (!bottom) {
                    return right ? 0 : 1;
                }
                return right ? 2 : 3;
            default:
                return -1;
        }
    }
}
