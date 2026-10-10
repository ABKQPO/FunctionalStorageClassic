package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeModifier;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

public class StorageUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    @Getter
    public static class StorageTier {

        private final String id;
        private final int priority;
        private final IntSupplier multiplier;
        private final Consumer<UpgradeState.Builder> modifiers;

        public StorageTier(String id, int priority, IntSupplier multiplier, Consumer<UpgradeState.Builder> modifiers) {
            this.id = Objects.requireNonNull(id, "id");
            this.priority = priority;
            this.multiplier = Objects.requireNonNull(multiplier, "multiplier");
            this.modifiers = Objects.requireNonNull(modifiers, "modifiers");
        }

        public int getMultiplier() {
            return Math.max(1, multiplier.getAsInt());
        }

        public UpgradeState getState() {
            UpgradeState.Builder builder = UpgradeState.builder();
            modifiers.accept(builder);
            return builder.build();
        }
    }

    @Getter
    private final StorageTier tier;

    public StorageUpgradeItem(@Nonnull StorageTier tier) {
        super(tier.getId());
        this.tier = tier;
    }

    @Override
    public boolean isStorageUpgrade() {
        return true;
    }

    public int getReplacementPriority(@Nonnull ItemStack stack) {
        return tier.getPriority();
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        tier.modifiers.accept(builder);
    }

    public double getMultiplier() {
        return tier.getMultiplier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        UpgradeState state = tier.getState();
        if (state.hasFeature(StorageFeature.IRON_DOWNGRADE)) {
            tooltip.add(StatCollector.translateToLocal("item.utility.downgrade"));
        }
        for (Map.Entry<UpgradeAttribute, List<UpgradeModifier>> entry : state.getModifiers()
            .entrySet()) {
            for (UpgradeModifier modifier : entry.getValue()) {
                String operationKey = switch (modifier.getOperation()) {
                    case MULTIPLY -> "modify_factor_mult";
                    case ADD_BASE -> "modify_base_inc";
                    case SET_BASE -> "modify_base_set";
                };
                tooltip.add(
                    StatCollector.translateToLocalFormatted(
                        "storageupgrade.desc." + operationKey,
                        StatCollector.translateToLocal(
                            entry.getKey()
                                .getTranslationKey()),
                        modifier.getValue()));
            }
        }
    }

    public static boolean grantsMaxCapacity(@Nonnull ItemStack stack) {
        if (!(stack.getItem() instanceof IStorageUpgrade upgrade)) return false;
        UpgradeState.Builder builder = UpgradeState.builder();
        upgrade.applyUpgrade(stack, builder);
        return builder.build()
            .hasFeature(StorageFeature.MAX_CAPACITY);
    }
}
