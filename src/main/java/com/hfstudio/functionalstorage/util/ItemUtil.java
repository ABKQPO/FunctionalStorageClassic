package com.hfstudio.functionalstorage.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * Item identity helpers shared by the storage layer and the upgrade routing.
 */
public class ItemUtil {

    private ItemUtil() {}

    /**
     * Compares item, metadata, and stack NBT while ignoring stack size.
     *
     * @param left  first stack, may be null
     * @param right second stack, may be null
     * @return whether both stacks represent the same item type
     */
    public static boolean areItemStacksEqual(@Nullable ItemStack left, @Nullable ItemStack right) {
        if (left == null || right == null || left.getItem() == null || right.getItem() == null) {
            return false;
        }
        if (left.getItem() != right.getItem() || left.getItemDamage() != right.getItemDamage()) {
            return false;
        }
        NBTTagCompound leftTag = left.getTagCompound();
        NBTTagCompound rightTag = right.getTagCompound();
        return Objects.equals(leftTag, rightTag);
    }

    /**
     * Compares two stacks for compatibility, optionally accepting items that
     * share a permitted ore dictionary entry.
     *
     * @param template           configured slot template
     * @param stack              incoming stack
     * @param allowOreDictionary whether ore-dictionary equivalence is allowed
     * @return whether the stack may be stored in the template's slot
     */
    public static boolean areItemStacksCompatible(@Nonnull ItemStack template, @Nonnull ItemStack stack,
        boolean allowOreDictionary) {
        return areItemStacksEqual(template, stack) || (allowOreDictionary && sharesOreDictionary(template, stack));
    }

    /**
     * Reports whether two stacks share an ore dictionary entry that the
     * configured blacklist and whitelist permit.
     *
     * @param left  first stack
     * @param right second stack
     * @return whether a permitted shared entry exists
     */
    public static boolean sharesOreDictionary(@Nonnull ItemStack left, @Nonnull ItemStack right) {
        if (left.getItem() == null || right.getItem() == null) {
            return false;
        }
        int[] leftIds = OreDictionary.getOreIDs(left);
        int[] rightIds = OreDictionary.getOreIDs(right);
        if (leftIds.length == 0 || rightIds.length == 0) {
            return false;
        }
        for (int leftId : leftIds) {
            for (int rightId : rightIds) {
                if (leftId == rightId && isOreNameAllowed(OreDictionary.getOreName(leftId))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param oreName ore dictionary entry name
     * @return whether the configured filters allow this entry to match
     */
    public static boolean isOreNameAllowed(@Nullable String oreName) {
        if (oreName == null || "Unknown".equals(oreName)) {
            return false;
        }
        if (containsConfiguredName(FunctionalStorageConfig.GENERAL.oreDictionaryBlacklist, oreName)) {
            return false;
        }
        String[] whitelist = FunctionalStorageConfig.GENERAL.oreDictionaryWhitelist;
        return !hasConfiguredName(whitelist) || containsConfiguredName(whitelist, oreName);
    }

    /**
     * Reads a stack from persisted data, normalizing an empty result to null.
     *
     * @param tag stack tag
     * @return the restored stack, or {@code null}
     */
    @Nullable
    public static ItemStack readStack(@Nullable NBTTagCompound tag) {
        if (tag == null || tag.hasNoTags()) {
            return null;
        }
        ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
        return stack == null || stack.getItem() == null ? null : stack;
    }

    /**
     * Creates a count-one copy of a stack.
     *
     * @param stack source stack
     * @return a normalized copy, or {@code null}
     */
    @Nullable
    public static ItemStack copyWithSizeOne(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    /**
     * Collects every item registered under an ore dictionary name.
     *
     * @param oreName ore dictionary entry name
     * @return the matching stacks, never null
     */
    @Nonnull
    public static List<ItemStack> oreEntries(String oreName) {
        List<ItemStack> entries = OreDictionary.getOres(oreName);
        return entries == null ? Collections.emptyList() : entries;
    }

    /**
     * Resolves an item from a {@code modid:name} or {@code modid:name:meta}
     * description.
     *
     * @param description item description
     * @return the resolved item, or {@code null}
     */
    @Nullable
    public static Item itemFromDescription(@Nullable String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split(":");
        if (parts.length < 2) {
            return null;
        }
        return (Item) Item.itemRegistry.getObject(parts[0] + ":" + parts[1]);
    }

    /**
     * Resolves a metadata value from a {@code modid:name:meta} description.
     *
     * @param description item description
     * @return the parsed metadata, or zero
     */
    public static int metadataFromDescription(@Nullable String description) {
        if (description == null) {
            return 0;
        }
        String[] parts = description.trim()
            .split(":");
        if (parts.length < 3) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean containsConfiguredName(@Nullable String[] configuredNames, @Nonnull String oreName) {
        if (configuredNames == null) {
            return false;
        }
        for (String configuredName : configuredNames) {
            if (configuredName != null && oreName.equals(configuredName.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConfiguredName(@Nullable String[] configuredNames) {
        if (configuredNames == null) {
            return false;
        }
        for (String configuredName : configuredNames) {
            if (configuredName != null && !configuredName.trim()
                .isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
