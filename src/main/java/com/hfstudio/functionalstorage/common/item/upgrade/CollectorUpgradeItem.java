package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/**
 * Collector upgrade. Sweeps dropped items and fluid source blocks in front of
 * the drawer into it, which is how a drawer can be fed without a hopper.
 */
public class CollectorUpgradeItem extends AutomationUpgradeItem {

    private static final double COLLECT_RADIUS = 1.5D;

    public CollectorUpgradeItem() {
        super("collector_upgrade", FunctionalStorageConfig.UPGRADES.upgradeTick);
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote) {
            return;
        }
        collectEntities(tile, stack);
        collectFluid(tile, stack);
    }

    private void collectEntities(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (tile.getItemHandler() == null) {
            return;
        }
        World world = tile.getWorldObj();
        ForgeDirection side = UpgradeTargeting.targetDirection(tile, stack);
        double targetX = tile.xCoord + 0.5D + side.offsetX * 0.5D;
        double targetY = tile.yCoord + 0.5D + side.offsetY * 0.5D;
        double targetZ = tile.zCoord + 0.5D + side.offsetZ * 0.5D;

        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(
            targetX - COLLECT_RADIUS,
            targetY - COLLECT_RADIUS,
            targetZ - COLLECT_RADIUS,
            targetX + COLLECT_RADIUS,
            targetY + COLLECT_RADIUS,
            targetZ + COLLECT_RADIUS);

        @SuppressWarnings("unchecked")
        List<EntityItem> entities = world.getEntitiesWithinAABB(EntityItem.class, area);
        if (entities == null || entities.isEmpty()) {
            return;
        }
        int budget = FunctionalStorageConfig.UPGRADES.upgradeCollectorItems;
        for (EntityItem entity : entities) {
            if (budget <= 0 || entity.isDead) {
                break;
            }
            ItemStack dropped = entity.getEntityItem();
            if (dropped == null || dropped.getItem() == null) {
                continue;
            }
            int request = Math.min(budget, dropped.stackSize);
            ItemStack probe = dropped.copy();
            probe.stackSize = request;
            ItemStack leftover = tile.getItemHandler()
                .insertItem(0, probe, false);
            int stored = request - (leftover == null ? 0 : leftover.stackSize);
            if (stored <= 0) {
                continue;
            }
            budget -= stored;
            dropped.stackSize -= stored;
            if (dropped.stackSize <= 0) {
                entity.setDead();
            } else {
                entity.setEntityItemStack(dropped);
            }
        }
    }

    private void collectFluid(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (tile.getFluidHandler() == null) {
            return;
        }
        World world = tile.getWorldObj();
        ForgeDirection side = UpgradeTargeting.targetDirection(tile, stack);
        int x = tile.xCoord + side.offsetX;
        int y = tile.yCoord + side.offsetY;
        int z = tile.zCoord + side.offsetZ;

        Block block = world.getBlock(x, y, z);
        if (!(block instanceof IFluidBlock)) {
            return;
        }
        IFluidBlock fluidBlock = (IFluidBlock) block;
        if (fluidBlock.getFluid() == null) {
            return;
        }
        int budget = FunctionalStorageConfig.UPGRADES.upgradeCollectorFluid;
        FluidStack drain = fluidBlock.drain(world, x, y, z, false);
        if (drain == null || drain.amount <= 0) {
            return;
        }
        drain.amount = Math.min(drain.amount, budget);
        int accepted = tile.getFluidHandler()
            .fill(drain, false);
        if (accepted <= 0) {
            return;
        }
        FluidStack taken = fluidBlock.drain(world, x, y, z, true);
        if (taken != null && taken.amount > 0) {
            tile.getFluidHandler()
                .fill(taken, true);
        }
    }
}
