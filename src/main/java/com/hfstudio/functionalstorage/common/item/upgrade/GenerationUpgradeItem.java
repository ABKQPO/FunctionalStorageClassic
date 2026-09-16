package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;

/**
 * Generation upgrades. Water generation fills a fluid drawer, stone generation
 * produces cobblestone, and universal generation produces a configured item.
 * Each tier changes only the amount produced per run.
 *
 * <p>Generation is limited to safe, always-available resources so an unconfigured
 * pack cannot be turned into an infinite source of arbitrary items.</p>
 */
public class GenerationUpgradeItem extends AutomationUpgradeItem {

    /**
     * Resource a generation upgrade produces.
     */
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

        /**
         * @return the identifier prefix used for registration and lang keys
         */
        public String getIdPrefix() {
            return idPrefix;
        }

        /**
         * @return the highest tier available for this kind
         */
        public int getTierCount() {
            return tierCount;
        }
    }

    private final GenerationKind kind;
    private final int tier;
    public GenerationUpgradeItem(@Nonnull GenerationKind kind, int tier) {
        super(kind.getIdPrefix() + "_t" + Math.max(1, tier), 1);
        this.kind = kind;
        this.tier = Math.max(1, tier);
    }

    /**
     * @return the resource this upgrade produces
     */
    @Nonnull
    public GenerationKind getKind() {
        return kind;
    }

    /**
     * @return the tier of this upgrade
     */
    public int getTier() {
        return tier;
    }

    @Override
    public boolean hasDirection() {
        return false;
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
        net.minecraft.item.Item item = ItemUtil.itemFromDescription(description);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, ItemUtil.metadataFromDescription(description));
    }

    private int waterRate() {
        switch (tier) {
            case 2:
                return FunctionalStorageConfig.UPGRADES.waterGenerationTier2;
            case 3:
                return FunctionalStorageConfig.UPGRADES.waterGenerationTier3;
            case 4:
                return FunctionalStorageConfig.UPGRADES.waterGenerationTier4;
            default:
                return FunctionalStorageConfig.UPGRADES.waterGenerationTier1;
        }
    }

    private int stoneRate() {
        switch (tier) {
            case 2:
                return FunctionalStorageConfig.UPGRADES.stoneGenerationTier2;
            case 3:
                return FunctionalStorageConfig.UPGRADES.stoneGenerationTier3;
            case 4:
                return FunctionalStorageConfig.UPGRADES.stoneGenerationTier4;
            default:
                return FunctionalStorageConfig.UPGRADES.stoneGenerationTier1;
        }
    }

    /**
     * @param kind generation kind
     * @param tier tier index
     * @return the registry name of a generation upgrade
     */
    @Nonnull
    public static String registryName(@Nonnull GenerationKind kind, int tier) {
        return (kind.getIdPrefix() + "_t" + tier).toLowerCase(Locale.ROOT);
    }
}
