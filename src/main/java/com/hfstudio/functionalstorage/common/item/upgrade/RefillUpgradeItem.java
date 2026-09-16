package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import lombok.Getter;

/**
 * Refill upgrade, merged in from More Functional Storage. Tops up the owning
 * player's held stack from the drawer whenever it runs low.
 *
 * <p>
 * The dimensional variant uses the same logic but resolves the owner through
 * the server player list, so it also works when the player is in another
 * dimension. Whether that succeeds is decided by
 * {@link #isDimensional()} rather than by which lookup succeeds, keeping the
 * intent explicit.
 * </p>
 */
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
        int currentSlot = player.inventory.currentItem;
        ItemStack held = player.inventory.getStackInSlot(currentSlot);
        if (held == null || held.getItem() == null) {
            return;
        }
        int maxStack = Math.max(1, held.getMaxStackSize());
        if (held.stackSize >= maxStack) {
            return;
        }

        List<Integer> slots = UpgradeTargeting.selectedSlots(
            stack,
            tile.getItemHandler()
                .getStorageCount());
        ItemStack filter = getFilter(stack);
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
            if (filter != null && !ItemUtil.areItemStacksEqual(filter, template)) {
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
            player.inventory.markDirty();
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
