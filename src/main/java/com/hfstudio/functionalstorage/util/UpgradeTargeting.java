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
        ForgeDirection horizontal = DrawerBlock.getHorizontalFacing(tile.getBlockMetadata());
        ForgeDirection up = switch (DrawerBlock.getAttachment(tile.getBlockMetadata())) {
            case FLOOR -> horizontal.getOpposite();
            case CEILING -> horizontal;
            case WALL -> ForgeDirection.UP;
        };
        return switch (relative) {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case UP -> up;
            case DOWN -> up.getOpposite();
            case LEFT -> horizontalLeft(horizontal);
            case RIGHT -> horizontalLeft(horizontal).getOpposite();
        };
    }

    @Nonnull
    public static ForgeDirection resolve(@Nonnull ForgeDirection front, @Nonnull RelativeDirection relative) {
        return switch (relative) {
            case BACK -> front.getOpposite();
            case UP -> ForgeDirection.UP;
            case DOWN -> ForgeDirection.DOWN;
            case LEFT -> horizontalLeft(front);
            case RIGHT -> horizontalLeft(front).getOpposite();
            default -> front;
        };
    }

    @Nonnull
    public static ForgeDirection horizontalLeft(@Nonnull ForgeDirection front) {
        return switch (front) {
            case NORTH -> ForgeDirection.WEST;
            case WEST -> ForgeDirection.SOUTH;
            case SOUTH -> ForgeDirection.EAST;
            case EAST -> ForgeDirection.NORTH;
            default -> ForgeDirection.NORTH;
        };
    }

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
