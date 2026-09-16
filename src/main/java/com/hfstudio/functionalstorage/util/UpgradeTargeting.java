package com.hfstudio.functionalstorage.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem.RelativeDirection;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Shared geometry and slot selection helpers for the automation upgrades.
 */
public class UpgradeTargeting {

    private UpgradeTargeting() {}

    /**
     * Resolves the world direction an automation upgrade works towards,
     * combining the drawer's placement orientation with the upgrade's own
     * configured relative direction.
     *
     * @param tile  owning drawer
     * @param stack installed upgrade stack
     * @return the target direction
     */
    @Nonnull
    public static ForgeDirection targetDirection(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        ForgeDirection front = DrawerBlock.getFrontFacing(tile.getBlockMetadata());
        RelativeDirection relative = stack.getItem() instanceof AutomationUpgradeItem
            ? ((AutomationUpgradeItem) stack.getItem()).getDirection(stack)
            : RelativeDirection.FRONT;
        return resolve(front, relative);
    }

    /**
     * Maps a relative direction onto a world direction.
     *
     * @param front    the drawer's front face
     * @param relative relative offset
     * @return the resulting world direction
     */
    @Nonnull
    public static ForgeDirection resolve(@Nonnull ForgeDirection front, @Nonnull RelativeDirection relative) {
        switch (relative) {
            case BACK:
                return front.getOpposite();
            case UP:
                return ForgeDirection.UP;
            case DOWN:
                return ForgeDirection.DOWN;
            case LEFT:
                return horizontalLeft(front);
            case RIGHT:
                return horizontalLeft(front).getOpposite();
            default:
                return front;
        }
    }

    /**
     * @param front horizontal facing
     * @return the facing ninety degrees to the left
     */
    @Nonnull
    public static ForgeDirection horizontalLeft(@Nonnull ForgeDirection front) {
        switch (front) {
            case NORTH:
                return ForgeDirection.WEST;
            case WEST:
                return ForgeDirection.SOUTH;
            case SOUTH:
                return ForgeDirection.EAST;
            case EAST:
                return ForgeDirection.NORTH;
            default:
                return ForgeDirection.NORTH;
        }
    }

    /**
     * @param stack upgrade stack
     * @param count available drawer slots
     * @return the drawer slots this upgrade may use, never empty when count is positive
     */
    @Nonnull
    public static List<Integer> selectedSlots(@Nonnull ItemStack stack, int count) {
        List<Integer> slots = new ArrayList<>();
        int[] selected = stack.getItem() instanceof AutomationUpgradeItem
            ? ((AutomationUpgradeItem) stack.getItem()).getSelectedSlots(stack)
            : null;
        if (selected == null) {
            for (int index = 0; index < count; index++) {
                slots.add(index);
            }
            return slots;
        }
        for (int index : selected) {
            if (index >= 0 && index < count) {
                slots.add(index);
            }
        }
        return slots;
    }
}
