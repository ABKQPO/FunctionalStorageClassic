package com.hfstudio.functionalstorage.common.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.util.ItemUtil;

import lombok.Getter;

/**
 * Immutable definition of one visible compression tier. A tier names an item
 * and how many lowest-tier units it represents, for example a block that
 * equals nine ingots which equal eighty-one nuggets.
 */
public class CompactingTier {

    private static final CompactingTier EMPTY = new CompactingTier(null, 1L);

    @Nullable
    private final ItemStack template;
    @Getter
    private final long baseUnits;

    /**
     * Creates a tier. The supplied stack is copied immediately and normalized
     * to a count of one.
     *
     * @param template  item represented by this tier, or {@code null} for an empty tier
     * @param baseUnits number of lowest-tier units, clamped to at least one
     */
    public CompactingTier(@Nullable ItemStack template, long baseUnits) {
        this.template = ItemUtil.copyWithSizeOne(template);
        this.baseUnits = Math.max(1L, baseUnits);
    }

    @Nonnull
    public static CompactingTier empty() {
        return EMPTY;
    }

    @Nullable
    public ItemStack getTemplate() {
        return ItemUtil.copyWithSizeOne(template);
    }

    public boolean hasTemplate() {
        return template != null;
    }

    public boolean sameDefinition(@Nullable CompactingTier other) {
        return other != null && baseUnits == other.baseUnits && ItemUtil.areItemStacksEqual(template, other.template);
    }

    @Nonnull
    public CompactingTier copy() {
        return new CompactingTier(template, baseUnits);
    }

    @Override
    public String toString() {
        return "CompactingTier{" + (template == null ? "empty" : template.getDisplayName()) + " x" + baseUnits + '}';
    }
}
