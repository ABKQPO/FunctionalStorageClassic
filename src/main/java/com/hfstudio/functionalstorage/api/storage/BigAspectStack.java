package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import thaumcraft.api.aspects.Aspect;

/**
 * Immutable essentia snapshot composed of an aspect and a long amount. A zero
 * amount may retain an aspect to represent a locked filter; negative amounts
 * are clamped to zero. An absent resource is normalized to {@link #empty()}.
 */
public class BigAspectStack implements StorageSnapshot<BigAspectStack, AspectStorageKey> {

    private static final BigAspectStack EMPTY = new BigAspectStack();

    @Nullable
    private final Aspect aspect;
    @Nullable
    private final AspectStorageKey key;
    private final long amount;

    private BigAspectStack() {
        this.aspect = null;
        this.key = null;
        this.amount = 0L;
    }

    /**
     * Creates a snapshot.
     *
     * @param aspect represented aspect; {@code null} is empty
     * @param amount represented amount; negative values are clamped to zero
     */
    public BigAspectStack(@Nullable Aspect aspect, long amount) {
        if (aspect == null) {
            this.aspect = null;
            this.key = null;
            this.amount = 0L;
            return;
        }
        this.aspect = aspect;
        this.key = new AspectStorageKey(aspect);
        this.amount = Math.max(0L, amount);
    }

    @Nonnull
    public static BigAspectStack empty() {
        return EMPTY;
    }

    @Nullable
    public Aspect getAspect() {
        return aspect;
    }

    @Nullable
    @Override
    public AspectStorageKey getKey() {
        return key;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Nonnull
    @Override
    public BigAspectStack withAmount(long newAmount) {
        return aspect == null ? empty() : new BigAspectStack(aspect, newAmount);
    }

    /**
     * Distinguishes an unfiltered empty snapshot from a zero-amount snapshot
     * retaining a locked aspect.
     *
     * @return whether this snapshot carries a resource template
     */
    @Override
    public boolean hasTemplate() {
        return key != null;
    }

    public boolean isSameType(@Nullable Aspect other) {
        return aspect != null && aspect == other;
    }

    /**
     * Converts this snapshot to Thaumcraft's int-amount representation.
     * Amounts above {@link Integer#MAX_VALUE} are saturated at that boundary.
     *
     * @return a fresh count value, or zero when nothing is stored
     */
    public int toAmount() {
        if (aspect == null || amount == 0L) {
            return 0;
        }
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BigAspectStack other)) {
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
        return "BigAspectStack{" + key + " x" + amount + '}';
    }
}
