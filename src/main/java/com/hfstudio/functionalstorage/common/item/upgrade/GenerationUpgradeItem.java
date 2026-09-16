package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;

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
                generateWater(tile);
                break;
            case STONE:
                generateStone(tile);
                break;
            default:
                generateItem(tile, stack);
                break;
        }
    }

    private void generateWater(@Nonnull ControllableDrawerTile tile) {
        if (tile.getFluidHandler() == null || FluidRegistry.WATER == null) {
            return;
        }
        FluidStack water = new FluidStack(FluidRegistry.WATER, waterRate());
        if (tile.getFluidHandler()
            .fill(water, false) > 0) {
            tile.getFluidHandler()
                .fill(water, true);
        }
    }

    private void generateStone(@Nonnull ControllableDrawerTile tile) {
        if (tile.getItemHandler() == null) {
            return;
        }
        int rate = Math.max(1, stoneRate());
        insert(tile, new ItemStack(Blocks.cobblestone, Math.min(64, rate)));
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
        batch.stackSize = Math.min(produced.getMaxStackSize(), 1);
        insert(tile, batch);
    }

    private void insert(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (stack.getItem() == null || stack.stackSize <= 0) {
            return;
        }
        tile.getItemHandler()
            .insertItem(0, stack, false);
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

    @Nonnull
    public static String registryName(@Nonnull GenerationKind kind, int tier) {
        return (kind.getIdPrefix() + "_t" + tier).toLowerCase(Locale.ROOT);
    }
}
