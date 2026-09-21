package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/** Collects dropped items and complete fluid sources on the selected side. */
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

        List<EntityItem> entities = world.getEntitiesWithinAABB(EntityItem.class, area);
        if (entities == null || entities.isEmpty()) {
            return;
        }
        int budget = FunctionalStorageConfig.UPGRADES.upgradeCollectorItems;
        IBigItemHandler storage = UpgradeSettings.itemStorage(tile.getItemHandler(), stack);
        List<Integer> candidates = UpgradeTargeting.selectedSlots(stack, storage.getStorageCount());
        for (EntityItem entity : entities) {
            if (budget <= 0) {
                break;
            }
            if (entity.isDead) continue;
            ItemStack dropped = entity.getEntityItem();
            if (dropped == null || dropped.getItem() == null) {
                continue;
            }
            int request = Math.min(budget, dropped.stackSize);
            BigItemStack probe = new BigItemStack(dropped, request);
            // Collected drops merge into the slot already holding that item, and a
            // full matching slot is skipped rather than spilling into a new slot.
            int target = storage.pickInsertionIndex(candidates, probe);
            if (target < 0) {
                continue;
            }
            int stored = (int) storage.insert(target, probe, StorageAction.EXECUTE)
                .getProcessedAmount();
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
        int budget = FunctionalStorageConfig.UPGRADES.upgradeCollectorFluid;
        IBigFluidHandler storage = UpgradeSettings.fluidStorage(tile.getFluidHandler(), stack);
        FluidStack drain;
        if (block instanceof IFluidBlock fluidBlock) drain = fluidBlock.drain(world, x, y, z, false);
        else if (world.getBlockMetadata(x, y, z) == 0 && (block == Blocks.water || block == Blocks.flowing_water))
            drain = new FluidStack(FluidRegistry.WATER, 1000);
        else if (world.getBlockMetadata(x, y, z) == 0 && (block == Blocks.lava || block == Blocks.flowing_lava))
            drain = new FluidStack(FluidRegistry.LAVA, 1000);
        else return;
        if (drain == null || drain.amount <= 0 || budget <= 0) return;
        int credit = (int) Math
            .min(drain.amount, (long) Math.max(0, UpgradeSettings.get(stack, "FluidCollectionCredit")) + budget);
        UpgradeSettings.set(stack, "FluidCollectionCredit", credit);
        // World sources are indivisible: reserve the whole drain before removing the block.
        if (credit < drain.amount || storage.fill(drain, false) != drain.amount) return;
        FluidStack taken;
        if (block instanceof IFluidBlock fluidBlock) taken = fluidBlock.drain(world, x, y, z, true);
        else if (block == Blocks.water || block == Blocks.flowing_water) taken = drain;
        else taken = world.setBlockToAir(x, y, z) ? drain : null;
        if (taken != null && taken.amount > 0) {
            storage.fill(taken, true);
            UpgradeSettings.set(stack, "FluidCollectionCredit", 0);
        }
    }
}
