package com.hfstudio.functionalstorage.api.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

/**
 * Public contract implemented by items that contribute storage upgrade behavior.
 *
 * <p>
 * Stacks passed to this interface are owned by the caller and must not be mutated. Conflict
 * declarations are directional: an installer must call both the candidate and installed
 * upgrade, rejecting the pair if either returns {@code true}. Implementations must not retain the
 * builder or stack references after a call. No method is assumed to be thread-safe because
 * Minecraft item stacks are mutable.
 * </p>
 */
public interface IStorageUpgrade {

    /**
     * Adds this stack's numeric and feature contributions to the supplied builder.
     *
     * @param stack   installed upgrade stack
     * @param builder accumulator for this stack's contributions
     */
    void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder);

    /**
     * Returns whether this upgrade rejects coexistence with {@code otherStack}.
     *
     * <p>
     * The default permits coexistence. Callers are responsible for also invoking the other
     * upgrade's method with the arguments reversed.
     * </p>
     *
     * @param stack      this upgrade stack
     * @param otherStack candidate coexistence partner
     * @return whether the pair must be rejected
     */
    default boolean conflictsWith(@Nonnull ItemStack stack, @Nonnull ItemStack otherStack) {
        return false;
    }
}
