package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/** Produces water, cobblestone, or the configured generator item at the selected tier rate. */
public class GenerationUpgradeItem extends AutomationUpgradeItem {

    @Getter
    public enum GenerationKind {

        WATER("water_generation_upgrade", 1),
        STONE("stone_generation_upgrade", 1),
        UNIVERSAL("universal_item_generation", 1);

        private final String idPrefix;
        private final int tierCount;

        GenerationKind(String idPrefix, int tierCount) {
            this.idPrefix = idPrefix;
            this.tierCount = tierCount;
        }

    }

    private final GenerationKind kind;
    @Getter
    private final int tier;

    public GenerationUpgradeItem(@Nonnull GenerationKind kind, int tier) {
        super(kind.getIdPrefix() + "_t" + Math.max(1, tier), 1);
        this.kind = kind;
        this.tier = Math.max(1, tier);
    }

    @Nonnull
    public GenerationKind getKind() {
        return kind;
    }

    @Override
    public boolean hasDirection() {
        return false;
    }

    @Override
    public int getTickInterval() {
        return kind == GenerationKind.UNIVERSAL ? Math.max(1, FunctionalStorageConfig.UPGRADES.universalGenerationTick)
            : super.getTickInterval();
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        if (tile.getWorldObj() == null || tile.getWorldObj().isRemote) {
            return;
        }
        switch (kind) {
            case WATER:
                generateWater(tile, stack);
                break;
            case STONE:
                generateStone(tile, stack);
                break;
            default:
                generateItem(tile, stack);
                break;
        }
    }

    private void generateWater(@Nonnull ControllableDrawerTile tile, ItemStack upgrade) {
        if (tile.getFluidHandler() == null || FluidRegistry.WATER == null) {
            return;
        }
        IBigFluidHandler storage = UpgradeSettings.fluidStorage(tile.getFluidHandler(), upgrade);
        BigFluidStack request = new BigFluidStack(new FluidStack(FluidRegistry.WATER, 1), waterRate());
        if (storage.hasRoomInSingleSlot(request)) {
            storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
        }
    }

    private void generateStone(@Nonnull ControllableDrawerTile tile, ItemStack upgrade) {
        if (tile.getItemHandler() == null) {
            return;
        }
        insert(tile, upgrade, new ItemStack(Blocks.cobblestone, Math.max(1, stoneRate())));
    }

    private void generateItem(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (tile.getItemHandler() == null) {
            return;
        }
        ItemStack filter = getFilter(stack);
        ItemStack produced = filter != null ? filter : configuredUniversalItem();
        if (produced == null || produced.getItem() == null) {
            return;
        }
        ItemStack batch = produced.copy();
        batch.stackSize = 1;
        insert(tile, stack, batch);
    }

    private void insert(@Nonnull ControllableDrawerTile tile, ItemStack upgrade, @Nonnull ItemStack stack) {
        if (stack.getItem() == null || stack.stackSize <= 0) {
            return;
        }
        IBigItemHandler storage = UpgradeSettings.itemStorage(tile.getItemHandler(), upgrade);
        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        if (storage.hasRoomInSingleSlot(request)) {
            storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
        }
    }

    private ItemStack configuredUniversalItem() {
        if (!FunctionalStorageConfig.UPGRADES.universalGenerationRegistered) {
            return null;
        }
        String description = FunctionalStorageConfig.UPGRADES.universalGenerationItem;
        Item item = ItemUtil.itemFromDescription(description);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, ItemUtil.metadataFromDescription(description));
    }

    private int waterRate() {
        return switch (tier) {
            case 2 -> FunctionalStorageConfig.UPGRADES.waterGenerationTier2;
            case 3 -> FunctionalStorageConfig.UPGRADES.waterGenerationTier3;
            case 4 -> FunctionalStorageConfig.UPGRADES.waterGenerationTier4;
            default -> FunctionalStorageConfig.UPGRADES.waterGenerationTier1;
        };
    }

    private int stoneRate() {
        return switch (tier) {
            case 2 -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier2;
            case 3 -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier3;
            case 4 -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier4;
            default -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier1;
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (kind == GenerationKind.WATER) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "functionalupgrade.desc.generate_fluid",
                    NumberFormatUtil.formatFluid(waterRate()),
                    FluidRegistry.WATER.getLocalizedName(new FluidStack(FluidRegistry.WATER, 1))));
        } else {
            ItemStack produced = kind == GenerationKind.STONE ? new ItemStack(Blocks.cobblestone) : getFilter(stack);
            if (produced == null) {
                produced = configuredUniversalItem();
            }
            tooltip.add(
                produced == null ? StatCollector.translateToLocal("functionalstorage.upgrade.filter_empty")
                    : StatCollector.translateToLocalFormatted(
                        "functionalupgrade.desc.generate_item",
                        kind == GenerationKind.STONE ? stoneRate() : 1,
                        produced.getDisplayName()));
        }
    }

    @Nonnull
    public static String registryName(@Nonnull GenerationKind kind, int tier) {
        return (kind.getIdPrefix() + "_t" + tier).toLowerCase(Locale.ROOT);
    }
}
