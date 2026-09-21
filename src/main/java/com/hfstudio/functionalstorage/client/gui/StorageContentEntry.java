package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;

public final class StorageContentEntry {

    public final ItemStack item;
    public final FluidStack fluid;
    public final AspectIcon aspect;
    public final long amount;

    public StorageContentEntry(@Nullable ItemStack item, @Nullable FluidStack fluid, @Nullable AspectIcon aspect,
        long amount) {
        this.item = item;
        this.fluid = fluid;
        this.aspect = aspect;
        this.amount = Math.max(0L, amount);
    }

    @Nonnull
    public static StorageContentEntry ofItem(@Nonnull ItemStack stack, long amount) {
        return new StorageContentEntry(stack, null, null, amount);
    }

    @Nonnull
    public static StorageContentEntry ofFluid(@Nonnull FluidStack stack, long amount) {
        return new StorageContentEntry(null, stack, null, amount);
    }

    @Nonnull
    public static StorageContentEntry ofAspect(@Nonnull AspectIcon aspect, long amount) {
        return new StorageContentEntry(null, null, aspect, amount);
    }

    @Nullable
    public ItemStack item() {
        return item;
    }

    @Nullable
    public FluidStack fluid() {
        return fluid;
    }

    @Nullable
    public AspectIcon aspect() {
        return aspect;
    }

    public long amount() {
        return amount;
    }

    public boolean isItem() {
        return item != null;
    }

    @Nonnull
    public Object identity() {
        if (item != null) {
            return new ItemIdentity(item);
        }
        if (fluid != null) {
            return new FluidIdentity(fluid);
        }
        return new AspectIdentity(aspect.name());
    }

    @Nonnull
    public String name() {
        if (item != null) {
            return item.getDisplayName();
        }
        return fluid != null ? fluid.getLocalizedName() : aspect.name();
    }

    @Nonnull
    public static List<StorageContentEntry> merge(@Nonnull List<StorageContentEntry> entries) {
        Map<Object, StorageContentEntry> merged = new LinkedHashMap<>(entries.size());
        for (StorageContentEntry entry : entries) {
            Object identity = entry.identity();
            merged.compute(
                identity,
                (k, existing) -> existing == null ? entry
                    : existing.withAmount(saturatedAdd(existing.amount(), entry.amount())));
        }
        return new ArrayList<>(merged.values());
    }

    @Nonnull
    public StorageContentEntry withAmount(long newAmount) {
        return new StorageContentEntry(item, fluid, aspect, newAmount);
    }

    public static long saturatedAdd(long left, long right) {
        long sum = left + right;
        return sum < 0L ? Long.MAX_VALUE : sum;
    }

    public static final class ItemIdentity {

        public final ItemStack stack;

        public ItemIdentity(@Nonnull ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ItemIdentity other)) {
                return false;
            }
            return ItemStack.areItemStackTagsEqual(stack, other.stack) && stack.getItem() == other.stack.getItem()
                && stack.getItemDamage() == other.stack.getItemDamage();
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(stack.getItem());
            result = 31 * result + stack.getItemDamage();
            return 31 * result + (stack.hasTagCompound() ? stack.getTagCompound()
                .hashCode() : 0);
        }
    }

    public static final class FluidIdentity {

        public final FluidStack stack;

        public FluidIdentity(@Nonnull FluidStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof FluidIdentity other)) {
                return false;
            }
            return stack.isFluidEqual(other.stack);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(stack.getFluid());
            return 31 * result + (stack.tag == null ? 0 : stack.tag.hashCode());
        }
    }

    public static final class AspectIdentity {

        public final String name;

        public AspectIdentity(@Nonnull String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof AspectIdentity other && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
