package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

/**
 * Immutable fluid snapshot composed of a defensively copied type template and
 * a long amount. A present template always holds one millibucket. A zero
 * amount may retain a template to represent a locked filter; negative amounts
 * are clamped to zero. An absent resource is normalized to {@link #empty()}.
 */
public class BigFluidStack implements StorageSnapshot<BigFluidStack, FluidStorageKey> {

    private static final BigFluidStack EMPTY = new BigFluidStack();

    @Nullable
    private final FluidStack template;
    @Nullable
    private final FluidStorageKey key;
    private final long amount;

    private BigFluidStack() {
        this.template = null;
        this.key = null;
        this.amount = 0L;
    }

    /**
     * Creates a snapshot. The supplied stack and its NBT are copied
     * immediately and are never retained by reference.
     *
     * @param template fluid type and NBT to represent; {@code null} is empty
     * @param amount   represented amount; negative values are clamped to zero
     */
    public BigFluidStack(@Nullable FluidStack template, long amount) {
        if (template == null || template.getFluid() == null) {
            this.template = null;
            this.key = null;
            this.amount = 0L;
            return;
        }
        FluidStack copy = template.copy();
        copy.amount = 1;
        this.template = copy;
        this.key = new FluidStorageKey(copy);
        this.amount = Math.max(0L, amount);
    }

    @Nonnull
    public static BigFluidStack empty() {
        return EMPTY;
    }

    // Only existing snapshots may share the private, never-exposed template.
    private BigFluidStack(FluidStack template, FluidStorageKey key, long amount) {
        this.template = template;
        this.key = key;
        this.amount = amount;
    }

    /**
     * Returns a fresh copy of the normalized template. Mutating the returned
     * stack or its NBT cannot alter this snapshot.
     *
     * @return a one-millibucket template, or {@code null}
     */
    @Nullable
    public FluidStack getTemplate() {
        return template == null ? null : template.copy();
    }

    @Nullable
    @Override
    public FluidStorageKey getKey() {
        return key;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    /** Changing the amount retains the resource type, including at zero. */
    @Nonnull
    @Override
    public BigFluidStack withAmount(long newAmount) {
        if (template == null) {
            return empty();
        }
        long normalized = Math.max(0L, newAmount);
        return normalized == amount ? this : new BigFluidStack(template, key, normalized);
    }

    /**
     * Distinguishes an unfiltered empty snapshot from a zero-amount snapshot
     * retaining a locked type.
     *
     * @return whether this snapshot carries a resource template
     */
    @Override
    public boolean hasTemplate() {
        return key != null;
    }

    /**
     * Compares fluid identity and NBT while ignoring amount.
     *
     * @param other other fluid stack
     * @return {@code true} when this snapshot and the stack represent the same fluid type
     */
    public boolean isSameType(@Nullable FluidStack other) {
        return template != null && other != null && template.isFluidEqual(other);
    }

    /**
     * Converts this snapshot to Forge's int-amount representation. Amounts
     * above {@link Integer#MAX_VALUE} are saturated at that boundary.
     *
     * @return a fresh mutable stack, or {@code null} when nothing is stored
     */
    @Nullable
    public FluidStack toFluidStack() {
        if (template == null || amount == 0L) {
            return null;
        }
        FluidStack result = template.copy();
        result.amount = amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BigFluidStack other)) {
            return false;
        }
        return amount == other.amount && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return 31 * (key == null ? 0 : key.hashCode()) + Long.hashCode(amount);
    }

    @Override
    public String toString() {
        return "BigFluidStack{" + key + " x" + amount + '}';
    }
}
