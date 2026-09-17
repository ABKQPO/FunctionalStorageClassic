package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BreakerUpgradeItem extends AutomationUpgradeItem {

    public record PlannedInsert(int slot, BigItemStack stack) {}

    public BreakerUpgradeItem() {
        super("breaker_upgrade", FunctionalStorageConfig.UPGRADES.breakerTick);
    }

    @Override
    public void work(ControllableDrawerTile tile, ItemStack stack, int slot) {
        if (!(tile.getWorldObj() instanceof WorldServer world) || tile.getItemHandler() == null) return;
        ItemStack tool = UpgradeSettings.getStack(stack, "Tool");
        if (tool == null) return;
        ForgeDirection facing = UpgradeTargeting.targetDirection(tile, stack);
        int x = tile.xCoord + facing.offsetX;
        int y = tile.yCoord + facing.offsetY;
        int z = tile.zCoord + facing.offsetZ;
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);
        if (world.isAirBlock(x, y, z) || block.getBlockHardness(world, x, y, z) < 0F || block.hasTileEntity(metadata))
            return;
        try (UpgradePlayerContext context = new UpgradePlayerContext(
            world,
            stack,
            tool.copy(),
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
            facing)) {
            FakePlayer player = context.getPlayer();
            if (!block.canHarvestBlock(player, metadata)) return;
            List<ItemStack> drops = EnchantmentHelper.getSilkTouchModifier(player)
                && block.canSilkHarvest(world, player, x, y, z, metadata) ? List.of(new ItemStack(block, 1, metadata))
                    : block.getDrops(world, x, y, z, metadata, EnchantmentHelper.getFortuneModifier(player));
            int count = tile.getItemHandler()
                .getStorageCount();
            BigItemStack[] reserved = new BigItemStack[count];
            List<PlannedInsert> plan = new ArrayList<>();
            List<Integer> selected = UpgradeTargeting.selectedSlots(stack, count);
            Predicate<ItemStack> filter = UpgradeSettings.itemFilter(stack);
            for (ItemStack drop : drops) {
                if (!filter.test(drop)) return;
                long remaining = drop.stackSize;
                for (int target : selected) {
                    if (remaining == 0) break;
                    BigItemStack reservation = reserved[target];
                    if (reservation != null && !ItemUtil.areItemStacksEqual(reservation.getTemplate(), drop)) continue;
                    long occupied = reservation == null ? 0 : reservation.getAmount();
                    long available = tile.getItemHandler()
                        .insert(target, new BigItemStack(drop, remaining + occupied), StorageAction.SIMULATE)
                        .getProcessedAmount() - occupied;
                    long accepted = Math.min(remaining, Math.max(0L, available));
                    if (accepted == 0) continue;
                    reserved[target] = new BigItemStack(drop, occupied + accepted);
                    plan.add(new PlannedInsert(target, new BigItemStack(drop, accepted)));
                    remaining -= accepted;
                }
                if (remaining != 0) return;
            }
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(x, y, z, world, block, metadata, player))
                || !world.setBlockToAir(x, y, z)) return;
            for (PlannedInsert insertion : plan) {
                long remaining = tile.getItemHandler()
                    .insert(insertion.slot(), insertion.stack(), StorageAction.EXECUTE)
                    .getRemainingAmount();
                if (remaining > 0) {
                    world.spawnEntityInWorld(
                        new EntityItem(
                            world,
                            x + 0.5D,
                            y + 0.5D,
                            z + 0.5D,
                            insertion.stack()
                                .withAmount(remaining)
                                .toItemStack()));
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        ItemStack tool = UpgradeSettings.getStack(stack, "Tool");
        tooltip.add(
            tool == null ? StatCollector.translateToLocal("tooltip.morefunctionalstorage.no_tool")
                : StatCollector.translateToLocalFormatted("tooltip.morefunctionalstorage.tool", tool.getDisplayName()));
    }
}
