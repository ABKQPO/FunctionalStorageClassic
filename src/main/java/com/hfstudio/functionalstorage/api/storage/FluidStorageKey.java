package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * Immutable exact fluid identity containing the fluid and its stack NBT.
 */
public class FluidStorageKey implements StorageKey {

    private final Fluid fluid;
    @Nullable
    private final NBTTagCompound tag;
    private final int hashCode;

    /**
     * Creates a key from a fluid stack and defensively copies its NBT.
     *
     * @param stack fluid template; amount is ignored
     * @throws NullPointerException if {@code stack} is null
     */
    public FluidStorageKey(@Nonnull FluidStack stack) {
        Objects.requireNonNull(stack, "stack");
        this.fluid = Objects.requireNonNull(stack.getFluid(), "stack fluid");
        this.tag = stack.tag == null ? null : (NBTTagCompound) stack.tag.copy();
        this.hashCode = computeHashCode();
    }

    /**
     * @return the represented fluid
     */
    @Nonnull
    public Fluid getFluid() {
        return fluid;
    }

    /**
     * @return a defensive copy of the represented fluid NBT, or {@code null}
     */
    @Nullable
    public NBTTagCompound getTag() {
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }

    /**
     * @return a fresh one-millibucket stack carrying this key's NBT
     */
    @Nonnull
    public FluidStack toFluidStack() {
        FluidStack stack = new FluidStack(fluid, 1);
        stack.tag = tag == null ? null : (NBTTagCompound) tag.copy();
        return stack;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FluidStorageKey other)) {
            return false;
        }
        return fluid == other.fluid && Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "FluidStorageKey{" + fluid.getName() + '}';
    }

    private int computeHashCode() {
        return 31 * System.identityHashCode(fluid) + (tag == null ? 0 : tag.hashCode());
    }
}
