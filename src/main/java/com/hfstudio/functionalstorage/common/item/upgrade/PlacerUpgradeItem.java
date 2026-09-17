package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.function.Predicate;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

public class PlacerUpgradeItem extends AutomationUpgradeItem {

    public PlacerUpgradeItem() {
        super("placer_upgrade", FunctionalStorageConfig.UPGRADES.placerTick);
    }

    @Override
    public void work(ControllableDrawerTile tile, ItemStack stack, int slot) {
        if (!(tile.getWorldObj() instanceof WorldServer world) || tile.getItemHandler() == null) return;
        ForgeDirection facing = UpgradeTargeting.targetDirection(tile, stack);
        int x = tile.xCoord + facing.offsetX;
        int y = tile.yCoord + facing.offsetY;
        int z = tile.zCoord + facing.offsetZ;

        if (!world.blockExists(x, y, z) || !world.getBlock(x, y, z)
            .isReplaceable(world, x, y, z)) return;
        IBigItemHandler handler = tile.getItemHandler();
        Predicate<ItemStack> filter = UpgradeSettings.itemFilter(stack);
        for (int index : UpgradeTargeting.selectedSlots(stack, handler.getStorageCount())) {
            BigItemStack snapshot = handler.extract(index, 1, StorageAction.SIMULATE)
                .getProcessed();
            ItemStack template = snapshot.toItemStack();
            if (template == null || !(template.getItem() instanceof ItemBlock) || !filter.test(template)) continue;
            BigItemStack reserved = handler.extract(index, 1, StorageAction.EXECUTE)
                .getProcessed();
            ItemStack placing = reserved.toItemStack();
            if (placing == null) continue;
            boolean placed = false;
            try (UpgradePlayerContext context = new UpgradePlayerContext(
                world,
                stack,
                placing,
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                facing)) {
                placed = ForgeHooks.onPlaceItemIntoWorld(
                    placing,
                    context.getPlayer(),
                    world,
                    x,
                    y,
                    z,
                    facing.ordinal(),
                    0.5F,
                    0.5F,
                    0.5F);
            } finally {
                if (!placed) {
                    long remaining = handler.insert(index, reserved, StorageAction.EXECUTE)
                        .getRemainingAmount();
                    if (remaining > 0) {
                        world.spawnEntityInWorld(
                            new EntityItem(
                                world,
                                x + 0.5D,
                                y + 0.5D,
                                z + 0.5D,
                                reserved.withAmount(remaining)
                                    .toItemStack()));
                    }
                }
            }
            return;
        }
    }

}
