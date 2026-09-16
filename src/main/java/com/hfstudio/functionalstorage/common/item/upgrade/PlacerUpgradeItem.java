package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/**
 * Placer upgrade, merged in from More Functional Storage. Takes a block from
 * the drawer and places it against the face the drawer points at.
 */
public class PlacerUpgradeItem extends AutomationUpgradeItem {

    public PlacerUpgradeItem() {
        super("placer_upgrade", FunctionalStorageConfig.UPGRADES.placerTick);
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote || tile.getItemHandler() == null || world.isRemote) {
            return;
        }
        ForgeDirection facing = UpgradeTargeting.targetDirection(tile, stack);
        int x = tile.xCoord + facing.offsetX;
        int y = tile.yCoord + facing.offsetY;
        int z = tile.zCoord + facing.offsetZ;

        Block existing = world.getBlock(x, y, z);
        if (existing != null && existing != Blocks.air && !existing.isReplaceable(world, x, y, z)) {
            return;
        }

        List<Integer> slots = UpgradeTargeting.selectedSlots(
            stack,
            tile.getItemHandler()
                .getStorageCount());
        ItemStack filter = getFilter(stack);
        for (int index : slots) {
            ItemStack snapshot = tile.getItemHandler()
                .getSnapshot(index)
                .getTemplate();
            if (snapshot == null || !(snapshot.getItem() instanceof ItemBlock)) {
                continue;
            }
            if (filter != null && !ItemUtil.areItemStacksEqual(filter, snapshot)) {
                continue;
            }
            if (!place(world, x, y, z, facing, snapshot)) {
                continue;
            }
            tile.getItemHandler()
                .extract(index, 1L, StorageAction.EXECUTE);
            return;
        }
    }

    private boolean place(@Nonnull World world, int x, int y, int z, @Nonnull ForgeDirection facing,
        @Nonnull ItemStack template) {
        ItemBlock itemBlock = (ItemBlock) template.getItem();
        Block block = itemBlock.field_150939_a;
        if (block == null) {
            return false;
        }
        int metadata = itemBlock.getMetadata(template.getItemDamage());
        return world.setBlock(x, y, z, block, metadata, 3);
    }
}
