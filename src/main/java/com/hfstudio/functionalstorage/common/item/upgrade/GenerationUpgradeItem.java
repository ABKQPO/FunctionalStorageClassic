package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;

/** A tiered resource generator configured by a registered definition. */
public class GenerationUpgradeItem extends ResourceGenerationUpgradeItem {

    @Getter
    public static class GenerationKind {

        private final String idPrefix;
        private final Function<ItemStack, ItemStack> itemFactory;
        private final FluidStack fluid;
        private final LongSupplier[] rates;
        private final IntSupplier[] intervals;
        private final IntFunction<String> registryNames;

        public GenerationKind(String idPrefix, Function<ItemStack, ItemStack> itemFactory, FluidStack fluid,
            LongSupplier[] rates, IntSupplier[] intervals) {
            this(idPrefix, itemFactory, fluid, rates, intervals, tier -> idPrefix + "_" + tier);
        }

        public GenerationKind(String idPrefix, Function<ItemStack, ItemStack> itemFactory, FluidStack fluid,
            LongSupplier[] rates, IntSupplier[] intervals, IntFunction<String> registryNames) {
            this.idPrefix = Objects.requireNonNull(idPrefix, "idPrefix");
            this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
            this.fluid = fluid == null ? null : fluid.copy();
            this.registryNames = Objects.requireNonNull(registryNames, "registryNames");
            if (rates.length == 0 || rates.length != intervals.length) {
                throw new IllegalArgumentException(
                    "Generation rates and intervals must define the same nonempty tiers");
            }
            this.rates = rates.clone();
            this.intervals = intervals.clone();
            for (int index = 0; index < rates.length; index++) {
                Objects.requireNonNull(this.rates[index], "rate");
                Objects.requireNonNull(this.intervals[index], "interval");
            }
        }

        public int getTierCount() {
            return rates.length;
        }

        public FluidStack getFluid() {
            return fluid == null ? null : fluid.copy();
        }

        public LongSupplier[] getRates() {
            return rates.clone();
        }

        public IntSupplier[] getIntervals() {
            return intervals.clone();
        }
    }

    @Getter
    private final GenerationKind kind;
    @Getter
    private final int tier;

    public GenerationUpgradeItem(@Nonnull GenerationKind kind, int tier) {
        super(registryName(kind, tier), kind.intervals[tier - 1], null, kind.getFluid(), kind.rates[tier - 1]);
        this.kind = kind;
        this.tier = tier;
    }

    @Override
    protected ItemStack resolveGeneratedItem(ItemStack upgrade) {
        return kind.itemFactory.apply(upgrade);
    }

    @Override
    public ItemStack getGeneratedItem() {
        ItemStack item = resolveGeneratedItem(new ItemStack(this));
        return item == null ? null : item.copy();
    }

    @Nonnull
    public static String registryName(@Nonnull GenerationKind kind, int tier) {
        if (tier < 1 || tier > kind.getTierCount()) {
            throw new IllegalArgumentException("Generator tier is outside its registered definition");
        }
        return Objects.requireNonNull(kind.registryNames.apply(tier), "registry name");
    }
}
