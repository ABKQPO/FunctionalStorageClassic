package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import lombok.Getter;

@Getter
public class RefillUpgradeItem extends AutomationUpgradeItem {

    private final boolean dimensional;

    public RefillUpgradeItem(boolean dimensional) {
        super(
            dimensional ? "dimensional_refill_upgrade" : "refill_upgrade",
            FunctionalStorageConfig.UPGRADES.refillTick);
        this.dimensional = dimensional;
    }

    @Override
    public void addDescription(List<String> tooltip) {
        String tooltipKey = getUnlocalizedName() + ".tooltip.";
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(tooltipKey + "0"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(tooltipKey + "1"));
    }

    @Override
    public boolean hasDirection() {
        return false;
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        if (tile.getWorldObj() == null || tile.getWorldObj().isRemote || tile.getItemHandler() == null) {
            return;
        }
        UUID owner = getOwner(stack);
        if (owner == null) {
            return;
        }
        EntityPlayer player = resolveOwner(tile, owner);
        if (player == null) {
            return;
        }
        int target = UpgradeSettings.get(stack, "RefillTarget");
        IInventory inventory = target == 2 ? player.getInventoryEnderChest() : player.inventory;
        int start = target == 1 ? 9 : 0;
        int end = target == 0 ? 9 : target == 1 ? 36 : inventory.getSizeInventory();
        List<Integer> slots = UpgradeTargeting.selectedSlots(
            stack,
            tile.getItemHandler()
                .getStorageCount());
        Predicate<ItemStack> filter = UpgradeSettings.itemFilter(stack);
        for (int index = start; index < end; index++) {
            refillSlot(tile, slots, filter, inventory, index);
        }
    }

    private void refillSlot(ControllableDrawerTile tile, List<Integer> slots, Predicate<ItemStack> filter,
        IInventory inventory, int currentSlot) {
        ItemStack held = inventory.getStackInSlot(currentSlot);
        if (held == null || held.getItem() == null) {
            return;
        }
        int maxStack = Math.max(1, held.getMaxStackSize());
        if (held.stackSize >= maxStack) {
            return;
        }

        int needed = maxStack - held.stackSize;
        for (int index : slots) {
            if (needed <= 0) {
                return;
            }
            BigItemStack snapshot = tile.getItemHandler()
                .getSnapshot(index);
            ItemStack template = snapshot.getTemplate();
            if (!ItemUtil.areItemStacksEqual(template, held)) {
                continue;
            }
            if (!filter.test(template)) {
                continue;
            }
            long extracted = tile.getItemHandler()
                .extract(index, needed, StorageAction.EXECUTE)
                .getProcessedAmount();
            if (extracted <= 0L) {
                continue;
            }
            held.stackSize += (int) Math.min(Integer.MAX_VALUE, extracted);
            needed -= (int) extracted;
            inventory.markDirty();
        }
    }

    @Nullable
    private EntityPlayer resolveOwner(@Nonnull ControllableDrawerTile tile, @Nonnull UUID owner) {
        if (!dimensional) {
            if (tile.getWorldObj() == null) {
                return null;
            }
            for (EntityPlayer candidate : tile.getWorldObj().playerEntities) {
                if (candidate instanceof EntityPlayer && owner.equals(candidate.getUniqueID())) {
                    return candidate;
                }
            }
            return null;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        for (EntityPlayerMP candidate : server.getConfigurationManager().playerEntityList) {
            if (candidate instanceof EntityPlayerMP && owner.equals(candidate.getUniqueID())) {
                return candidate;
            }
        }
        return null;
    }
}
