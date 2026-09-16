package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/**
 * Breaker upgrade, merged in from More Functional Storage. Breaks the block the
 * drawer faces and stores the drops, honouring an optional item filter and an
 * optional slot selection. Drops that do not fit are released into the world so
 * a breaker never destroys items.
 */
public class BreakerUpgradeItem extends AutomationUpgradeItem {

    public BreakerUpgradeItem() {
        super("breaker_upgrade", FunctionalStorageConfig.UPGRADES.breakerTick);
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote || tile.getItemHandler() == null) {
            return;
        }
        ForgeDirection facing = UpgradeTargeting.targetDirection(tile, stack);
        int x = tile.xCoord + facing.offsetX;
        int y = tile.yCoord + facing.offsetY;
        int z = tile.zCoord + facing.offsetZ;

        Block block = world.getBlock(x, y, z);
        if (block == null || block == Blocks.air || block.getBlockHardness(world, x, y, z) < 0F) {
            return;
        }
        int metadata = world.getBlockMetadata(x, y, z);
        if (block.hasTileEntity(metadata)) {
            return;
        }

        List<ItemStack> drops = block.getDrops(world, x, y, z, metadata, 0);
        List<ItemStack> accepted = filterDrops(stack, drops);
        if (accepted.isEmpty()) {
            return;
        }
        if (storeAll(tile, stack, accepted)) {
            world.setBlockToAir(x, y, z);
        }
    }

    @Nonnull
    private List<ItemStack> filterDrops(@Nonnull ItemStack upgradeStack, @Nullable List<ItemStack> drops) {
        List<ItemStack> accepted = new ArrayList<>();
        if (drops == null) {
            return accepted;
        }
        ItemStack filter = getFilter(upgradeStack);
        for (ItemStack drop : drops) {
            if (drop == null || drop.getItem() == null) {
                continue;
            }
            if (filter == null || ItemUtil.areItemStacksEqual(filter, drop)) {
                accepted.add(drop);
            }
        }
        return accepted;
    }

    private boolean storeAll(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack,
        @Nonnull List<ItemStack> drops) {
        List<Integer> slots = UpgradeTargeting.selectedSlots(
            stack,
            tile.getItemHandler()
                .getStorageCount());
        boolean complete = true;
        for (ItemStack drop : drops) {
            long remaining = drop.stackSize;
            for (int index : slots) {
                if (remaining <= 0L) {
                    break;
                }
                ItemStack probe = drop.copy();
                probe.stackSize = (int) Math.min(remaining, drop.stackSize);
                ItemStack leftover = tile.getItemHandler()
                    .insertItem(index, probe, false);
                remaining -= probe.stackSize - (leftover == null ? 0 : leftover.stackSize);
            }
            if (remaining > 0L) {
                complete = false;
                release(tile, drop, remaining);
            }
        }
        return complete;
    }

    private void release(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack drop, long remaining) {
        World world = tile.getWorldObj();
        if (world == null) {
            return;
        }
        ItemStack overflow = drop.copy();
        overflow.stackSize = (int) Math.min(Integer.MAX_VALUE, remaining);
        world.spawnEntityInWorld(
            new EntityItem(world, tile.xCoord + 0.5D, tile.yCoord + 1.0D, tile.zCoord + 0.5D, overflow));
    }
}
