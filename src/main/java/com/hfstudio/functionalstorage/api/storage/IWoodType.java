package com.hfstudio.functionalstorage.api.storage;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Wood variant a wooden drawer can be built from.
 *
 * <p>
 * The mod supplies its own variants, but other mods may contribute their own
 * woods through {@code FunctionalStorage#registerWoodType}. A contributed wood
 * is only registered when both its log and planks resolve, so a pack that
 * disables a wood never leaves a dangling block or recipe behind.
 * </p>
 */
public interface IWoodType {

    /**
     * @return the stable low-case identifier used by registry names, assets, and
     *         language keys
     */
    @Nonnull
    String getName();

    /**
     * @return the log block, or {@code null} when this wood is unavailable
     */
    @Nullable
    Block getLog();

    /**
     * @return the planks block, or {@code null} when this wood is unavailable
     */
    @Nullable
    Block getPlanks();

    /**
     * @return metadata of the planks used in recipes
     */
    int getPlankMetadata();

    /**
     * @return metadata of the log, used for ore dictionary registration
     */
    int getLogMetadata();

    /**
     * @return whether both the log and the planks are present
     */
    default boolean isAvailable() {
        return getLog() != null && getPlanks() != null;
    }

    /**
     * @return a plank stack, or {@code null} when this wood is unavailable
     */
    @Nullable
    default ItemStack getPlankStack() {
        Block planks = getPlanks();
        return planks == null ? null : new ItemStack(planks, 1, getPlankMetadata());
    }

    /**
     * @return a log stack, or {@code null} when this wood is unavailable
     */
    @Nullable
    default ItemStack getLogStack() {
        Block log = getLog();
        return log == null ? null : new ItemStack(log, 1, getLogMetadata());
    }

    /**
     * @return the available woods contributed so far
     */
    @Nonnull
    static List<IWoodType> registered() {
        return WoodTypeRegistry.get();
    }
}
