package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeModifier;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.common.storage.FluidStorageResource;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import lombok.Getter;

/**
 * Storage capacity upgrade. Each tier installs a multiplicative capacity
 * modifier whose magnitude depends on the resource kind's configured divisor.
 */
public class StorageUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    /**
     * Available storage upgrade tiers.
     */
    @Getter
    public enum StorageTier {

        IRON("iron_downgrade", 1, Integer.MIN_VALUE),
        COPPER("copper_upgrade", 8, 0),
        GOLD("gold_upgrade", 16, 1),
        DIAMOND("diamond_upgrade", 24, 2),
        NETHERITE("netherite_upgrade", 32, 3);

        private final String id;
        private final int multiplier;
        private final int priority;

        StorageTier(String id, int multiplier, int priority) {
            this.id = id;
            this.multiplier = multiplier;
            this.priority = priority;
        }

    }

    private final StorageTier tier;

    public StorageUpgradeItem(@Nonnull StorageTier tier) {
        super(tier.getId());
        this.tier = tier;
    }

    /**
     * @return the tier of this upgrade
     */
    @Nonnull
    public StorageTier getTier() {
        return tier;
    }

    /**
     * @param stack installed upgrade stack
     * @return the replacement priority, or {@link Integer#MIN_VALUE} when not installable
     */
    public int getReplacementPriority(@Nonnull ItemStack stack) {
        return tier.getPriority();
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addModifier(UpgradeAttribute.ITEM_CAPACITY, UpgradeModifier.multiply(getMultiplier()));
        builder.addModifier(UpgradeAttribute.FLUID_CAPACITY, UpgradeModifier.multiply(fluidMultiplier()));
        builder.addModifier(UpgradeAttribute.ASPECT_CAPACITY, UpgradeModifier.multiply(aspectMultiplier()));
        builder.addModifier(UpgradeAttribute.CONTROLLER_RANGE, UpgradeModifier.addBase(rangeBonus()));
    }

    /**
     * @return the item capacity multiplier contributed by this tier
     */
    public double getMultiplier() {
        return Math.max(1, tier.getMultiplier());
    }

    private double fluidMultiplier() {
        return Math.max(1D, getMultiplier() / FluidStorageResource.INSTANCE.upgradeDivisor());
    }

    private double aspectMultiplier() {
        return Math.max(1D, getMultiplier() / FunctionalStorageConfig.STORAGE.aspectDivisor);
    }

    private double rangeBonus() {
        return Math.max(0D, getMultiplier() / (double) Math.max(1, FunctionalStorageConfig.STORAGE.rangeDivisor));
    }

    /**
     * @param stack candidate upgrade stack
     * @return whether the stack carries a max capacity feature
     */
    public static boolean grantsMaxCapacity(@Nonnull ItemStack stack) {
        if (!(stack.getItem() instanceof IStorageUpgrade)) {
            return false;
        }
        UpgradeState.Builder builder = UpgradeState.builder();
        ((IStorageUpgrade) stack.getItem()).applyUpgrade(stack, builder);
        UpgradeState state = builder.build();
        return state.hasFeature(StorageFeature.MAX_CAPACITY);
    }
}
