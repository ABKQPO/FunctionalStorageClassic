package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeSettings;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DrawerContentCollector {

    @Nonnull
    public static List<StorageContentEntry> collectDrawer(@Nonnull ControllableDrawerTile tile,
        @Nonnull EntityPlayer player) {
        List<StorageContentEntry> entries = new ArrayList<>();
        collectStorage(tile, entries);
        collectUpgradesInSlots(tile, entries);
        collectInventory(player.inventory, entries);
        collectCarriedStack(player, entries);
        return StorageContentEntry.merge(entries);
    }

    @Nonnull
    public static List<StorageContentEntry> collectStorageOnly(@Nonnull ControllableDrawerTile tile) {
        List<StorageContentEntry> entries = new ArrayList<>();
        collectStorage(tile, entries);
        return StorageContentEntry.merge(entries);
    }

    private static void collectStorage(@Nonnull ControllableDrawerTile tile, @Nonnull List<StorageContentEntry> out) {
        IBigItemHandler items = tile.getItemHandler();
        if (items != null) {
            for (int slot = 0; slot < items.getStorageCount(); slot++) {
                BigItemStack stored = items.getSnapshot(slot);
                ItemStack template = stored.getTemplate();
                if (template != null && stored.getAmount() > 0L) {
                    out.add(StorageContentEntry.ofItem(template, stored.getAmount()));
                }
            }
        }
        IBigFluidHandler fluids = tile.getFluidHandler();
        if (fluids != null) {
            for (int slot = 0; slot < fluids.getStorageCount(); slot++) {
                BigFluidStack stored = fluids.getSnapshot(slot);
                if (stored.getTemplate() != null && stored.getAmount() > 0L) {
                    out.add(StorageContentEntry.ofFluid(stored.getTemplate(), stored.getAmount()));
                }
            }
        }
        IBigAspectHandler aspects = tile.getAspectHandler();
        if (aspects != null) {
            for (int slot = 0; slot < aspects.getStorageCount(); slot++) {
                BigAspectStack stored = aspects.getSnapshot(slot);
                if (stored.getAspect() != null && stored.getAmount() > 0L) {
                    out.add(
                        StorageContentEntry
                            .ofAspect(DrawerTooltipData.AspectIcon.of(stored.getAspect()), stored.getAmount()));
                }
            }
        }
    }

    private static void collectUpgradesInSlots(@Nonnull ControllableDrawerTile tile,
        @Nonnull List<StorageContentEntry> out) {
        for (int slot = 0; slot < tile.getStorageUpgradeSlots(); slot++) {
            collectUpgrade(tile.getStorageUpgrade(slot), out);
        }
        for (int slot = 0; slot < tile.getUtilityUpgradeSlots(); slot++) {
            collectUpgrade(tile.getUtilityUpgrade(slot), out);
        }
    }

    public static void collectUpgrade(@Nullable ItemStack upgrade, @Nonnull List<StorageContentEntry> out) {
        if (upgrade == null || upgrade.getItem() == null || upgrade.stackSize <= 0) {
            return;
        }
        out.add(StorageContentEntry.ofItem(upgrade, upgrade.stackSize));
        for (int slot = 0; slot < UpgradeSettings.FILTER_SLOTS; slot++) {
            addStack(out, UpgradeSettings.getFilter(upgrade, slot));
        }
        for (String key : UpgradeSettings.NESTED_SLOT_KEYS) {
            addStack(out, UpgradeSettings.getStack(upgrade, key));
        }
    }

    public static void collectInventory(@Nullable IInventory inventory, @Nonnull List<StorageContentEntry> out) {
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            addStack(out, inventory.getStackInSlot(slot));
        }
    }

    public static void collectCarriedStack(@Nullable EntityPlayer player, @Nonnull List<StorageContentEntry> out) {
        if (player != null) {
            addStack(out, player.inventory.getItemStack());
        }
    }

    private static void addStack(@Nonnull List<StorageContentEntry> out, @Nullable ItemStack stack) {
        if (stack != null && stack.getItem() != null && stack.stackSize > 0) {
            out.add(StorageContentEntry.ofItem(stack, stack.stackSize));
        }
    }
}
