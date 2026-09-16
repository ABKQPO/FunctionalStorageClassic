package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.CompactingUtil;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/**
 * Stonecutting upgrade, merged in from More Functional Storage. Applies the
 * one-to-many conversion of a stored stack in place, so a drawer can turn
 * blocks into ingots or logs into planks without a crafting table. The same
 * conversion table used by compacting drawers drives this upgrade.
 */
public class StonecuttingUpgradeItem extends AutomationUpgradeItem {

    private static final int MAX_CONVERSIONS_PER_RUN = 64;

    public StonecuttingUpgradeItem() {
        super("stonecutting_upgrade", FunctionalStorageConfig.UPGRADES.stonecuttingTick);
    }

    @Override
    public boolean hasDirection() {
        return false;
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote || tile.getItemHandler() == null) {
            return;
        }
        List<Integer> slots = UpgradeTargeting.selectedSlots(
            stack,
            tile.getItemHandler()
                .getStorageCount());
        ItemStack filter = getFilter(stack);

        for (int index : slots) {
            if (convertSlot(tile, stack, index, world, filter)) {
                return;
            }
        }
    }

    private boolean convertSlot(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack upgradeStack, int index,
        @Nonnull World world, @Nullable ItemStack filter) {
        ItemStack snapshotTemplate = tile.getItemHandler()
            .getSnapshot(index)
            .getTemplate();
        if (snapshotTemplate == null) {
            return false;
        }
        if (filter != null && !ItemUtil.areItemStacksEqual(filter, snapshotTemplate)) {
            return false;
        }
        CompactingUtil.LowerTier conversion = CompactingUtil.findLowerTier(world, snapshotTemplate);
        if (conversion == null || conversion.count <= 1) {
            return false;
        }

        long available = Math.min(
            tile.getItemHandler()
                .getSnapshot(index)
                .getAmount(),
            MAX_CONVERSIONS_PER_RUN);
        if (available <= 0L) {
            return false;
        }

        ItemStack probe = conversion.result.copy();
        probe.stackSize = (int) Math.min(Integer.MAX_VALUE, available * conversion.count);
        if (tile.getItemHandler()
            .insertItem(index, probe, true) != null) {
            return false;
        }

        long consumed = tile.getItemHandler()
            .extract(index, available, StorageAction.EXECUTE)
            .getProcessedAmount();
        if (consumed <= 0L) {
            return false;
        }

        ItemStack produced = conversion.result.copy();
        produced.stackSize = (int) Math.min(Integer.MAX_VALUE, consumed * conversion.count);
        ItemStack leftover = tile.getItemHandler()
            .insertItem(index, produced, false);
        if (leftover != null && leftover.stackSize > 0) {
            release(tile, leftover);
        }
        return true;
    }

    private void release(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        World world = tile.getWorldObj();
        if (world == null) {
            return;
        }
        world.spawnEntityInWorld(
            new EntityItem(world, tile.xCoord + 0.5D, tile.yCoord + 1.0D, tile.zCoord + 0.5D, stack));
    }
}
