package com.hfstudio.functionalstorage.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.storage.CompactingTier;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * Resolves the compacting chain of an item, for example nugget to ingot to
 * block, by consulting configured rules first and then the crafting manager.
 */
public class CompactingUtil {

    private CompactingUtil() {}

    /**
     * Finds the compacting chain anchored on a clicked slot. The clicked slot
     * always keeps the clicked item, slots to its left become higher tiers and
     * slots to its right become lower tiers.
     *
     * @param world      world used for recipe lookup
     * @param stack      item to build a chain for
     * @param maxSlots   maximum number of tiers
     * @param anchorSlot slot index that must keep the clicked item
     * @return the tier definitions, never null
     */
    @Nonnull
    public static List<CompactingTier> anchoredResults(@Nullable World world, @Nullable ItemStack stack, int maxSlots,
        int anchorSlot) {
        List<CompactingTier> fallback = results(world, stack, maxSlots);
        if (stack == null || stack.getItem() == null || anchorSlot < 0 || anchorSlot >= maxSlots) {
            return fallback;
        }

        ItemStack clicked = ItemUtil.copyWithSizeOne(stack);
        List<HigherTier> higherTiers = new ArrayList<>();
        ItemStack searching = clicked;
        for (int step = 0; step < anchorSlot; step++) {
            HigherTier higher = findHigherTier(world, searching);
            if (higher == null) {
                break;
            }
            higherTiers.add(higher);
            searching = ItemUtil.copyWithSizeOne(higher.result);
        }

        List<LowerTier> lowerTiers = new ArrayList<>();
        searching = clicked;
        for (int step = 0; step < maxSlots - anchorSlot - 1; step++) {
            LowerTier lower = findLowerTier(world, searching);
            if (lower == null) {
                break;
            }
            lowerTiers.add(lower);
            searching = ItemUtil.copyWithSizeOne(lower.result);
        }

        List<CompactingTier> anchored = new ArrayList<>(maxSlots);
        for (int index = 0; index < maxSlots; index++) {
            anchored.add(CompactingTier.empty());
        }

        long clickedUnits = 1L;
        for (LowerTier lower : lowerTiers) {
            clickedUnits = saturatedMultiply(clickedUnits, lower.count);
        }
        anchored.set(anchorSlot, new CompactingTier(clicked, clickedUnits));

        long higherUnits = clickedUnits;
        for (int step = 0; step < higherTiers.size(); step++) {
            HigherTier higher = higherTiers.get(step);
            higherUnits = saturatedMultiply(higherUnits, higher.inputCount);
            int target = anchorSlot - 1 - step;
            if (target < 0) {
                break;
            }
            anchored.set(target, new CompactingTier(higher.result, higherUnits));
        }

        long lowerUnits = clickedUnits;
        for (int step = 0; step < lowerTiers.size(); step++) {
            LowerTier lower = lowerTiers.get(step);
            lowerUnits = Math.max(1L, lowerUnits / lower.count);
            int target = anchorSlot + 1 + step;
            if (target >= maxSlots) {
                break;
            }
            anchored.set(target, new CompactingTier(lower.result, lowerUnits));
        }

        return anchored;
    }

    /**
     * Builds the natural compacting chain of an item, ordered from the highest
     * tier down to the lowest.
     *
     * @param world    world used for recipe lookup
     * @param stack    item to build a chain for
     * @param maxSlots maximum number of tiers
     * @return the tier definitions, never null
     */
    @Nonnull
    public static List<CompactingTier> results(@Nullable World world, @Nullable ItemStack stack, int maxSlots) {
        List<CompactingTier> results = new ArrayList<>();
        if (stack == null || stack.getItem() == null || maxSlots <= 0) {
            return padTo(results, maxSlots);
        }

        ItemStack current = ItemUtil.copyWithSizeOne(stack);

        List<HigherTier> higherTiers = new ArrayList<>();
        ItemStack searching = current;
        for (int step = 0; step < maxSlots - 1; step++) {
            HigherTier higher = findHigherTier(world, searching);
            if (higher == null) {
                break;
            }
            higherTiers.add(higher);
            searching = ItemUtil.copyWithSizeOne(higher.result);
        }

        if (higherTiers.isEmpty()) {
            appendDescendingChain(world, current, maxSlots, results);
        } else {
            appendAscendingChain(world, current, higherTiers, maxSlots, results);
        }
        return padTo(results, maxSlots);
    }

    private static void appendDescendingChain(@Nullable World world, @Nonnull ItemStack current, int maxSlots,
        @Nonnull List<CompactingTier> results) {
        List<LowerTier> lowerTiers = new ArrayList<>();
        ItemStack searching = current;
        for (int step = 0; step < maxSlots - 1; step++) {
            LowerTier lower = findLowerTier(world, searching);
            if (lower == null) {
                break;
            }
            lowerTiers.add(lower);
            searching = ItemUtil.copyWithSizeOne(lower.result);
        }

        long totalUnits = 1L;
        for (LowerTier lower : lowerTiers) {
            totalUnits = saturatedMultiply(totalUnits, lower.count);
        }
        results.add(new CompactingTier(current, totalUnits));

        long divisor = 1L;
        for (LowerTier lower : lowerTiers) {
            divisor = saturatedMultiply(divisor, lower.count);
            results.add(new CompactingTier(lower.result, Math.max(1L, totalUnits / divisor)));
        }
    }

    private static void appendAscendingChain(@Nullable World world, @Nonnull ItemStack current,
        @Nonnull List<HigherTier> higherTiers, int maxSlots, @Nonnull List<CompactingTier> results) {
        List<ItemStack> chain = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        chain.add(current);
        for (HigherTier higher : higherTiers) {
            counts.add(higher.inputCount);
            chain.add(ItemUtil.copyWithSizeOne(higher.result));
        }

        long[] units = new long[chain.size()];
        units[0] = 1L;
        for (int index = 1; index < chain.size(); index++) {
            units[index] = saturatedMultiply(units[index - 1], counts.get(index - 1));
        }
        for (int index = chain.size() - 1; index >= 0; index--) {
            results.add(new CompactingTier(chain.get(index), units[index]));
        }

        if (results.size() < maxSlots) {
            LowerTier lower = findLowerTier(world, current);
            if (lower != null) {
                for (int index = 0; index < results.size(); index++) {
                    CompactingTier tier = results.get(index);
                    results.set(
                        index,
                        new CompactingTier(tier.getTemplate(), saturatedMultiply(tier.getBaseUnits(), lower.count)));
                }
                results.add(new CompactingTier(lower.result, 1L));
            }
        }
    }

    private static List<CompactingTier> padTo(@Nonnull List<CompactingTier> results, int maxSlots) {
        while (results.size() < maxSlots) {
            results.add(CompactingTier.empty());
        }
        return results.size() > maxSlots ? new ArrayList<>(results.subList(0, maxSlots)) : results;
    }

    @Nullable
    public static HigherTier findHigherTier(@Nullable World world, @Nullable ItemStack input) {
        HigherTier configured = findConfiguredHigherTier(input);
        if (configured != null) {
            return configured;
        }
        HigherTier result = tryCompact(world, input, 3);
        return result != null ? result : tryCompact(world, input, 2);
    }

    @Nullable
    public static LowerTier findLowerTier(@Nullable World world, @Nullable ItemStack input) {
        LowerTier configured = findConfiguredLowerTier(input);
        if (configured != null) {
            return configured;
        }
        if (world == null || input == null || input.getItem() == null) {
            return null;
        }
        FakeCraftingInventory container = new FakeCraftingInventory(1);
        container.setInventorySlotContents(0, ItemUtil.copyWithSizeOne(input));
        ItemStack output = CraftingManager.getInstance()
            .findMatchingRecipe(container, world);
        if (output == null || output.getItem() == null || output.stackSize <= 1) {
            return null;
        }
        if (ItemUtil.areItemStacksEqual(output, input)) {
            return null;
        }
        return new LowerTier(output, output.stackSize);
    }

    @Nullable
    private static HigherTier findConfiguredHigherTier(@Nullable ItemStack input) {
        for (ConfiguredRule rule : configuredRules()) {
            if (ItemUtil.areItemStacksEqual(rule.lower, input)) {
                return new HigherTier(rule.higher, rule.ratio);
            }
        }
        return null;
    }

    @Nullable
    private static LowerTier findConfiguredLowerTier(@Nullable ItemStack input) {
        for (ConfiguredRule rule : configuredRules()) {
            if (ItemUtil.areItemStacksEqual(rule.higher, input)) {
                return new LowerTier(rule.lower, rule.ratio);
            }
        }
        return null;
    }

    @Nonnull
    private static List<ConfiguredRule> configuredRules() {
        if (!FunctionalStorageConfig.GENERAL.registerExtraCompactingRules
            || FunctionalStorageConfig.GENERAL.extraCompactingRules == null) {
            return Collections.emptyList();
        }
        List<ConfiguredRule> rules = new ArrayList<>();
        for (String configured : FunctionalStorageConfig.GENERAL.extraCompactingRules) {
            ConfiguredRule rule = parseConfiguredRule(configured);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return rules;
    }

    @Nullable
    private static ConfiguredRule parseConfiguredRule(@Nullable String configured) {
        if (configured == null) {
            return null;
        }
        String[] parts = configured.split(",");
        if (parts.length != 3) {
            return null;
        }
        ItemStack higher = parseConfiguredStack(parts[0].trim());
        ItemStack lower = parseConfiguredStack(parts[1].trim());
        if (higher == null || lower == null || ItemUtil.areItemStacksEqual(higher, lower)) {
            return null;
        }
        try {
            int ratio = Integer.parseInt(parts[2].trim());
            return ratio >= 2 ? new ConfiguredRule(higher, lower, ratio) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private static ItemStack parseConfiguredStack(@Nonnull String configured) {
        if (configured.isEmpty()) {
            return null;
        }
        String itemName = configured;
        int metadata = 0;
        int lastColon = configured.lastIndexOf(':');
        if (lastColon > 0) {
            try {
                metadata = Integer.parseInt(configured.substring(lastColon + 1));
                if (metadata < 0) {
                    return null;
                }
                itemName = configured.substring(0, lastColon);
            } catch (NumberFormatException ignored) {
                metadata = 0;
            }
        }
        Item item = ItemUtil.itemFromDescription(itemName);
        return item == null ? null : new ItemStack(item, 1, metadata);
    }

    @Nullable
    private static HigherTier tryCompact(@Nullable World world, @Nullable ItemStack input, int gridSize) {
        if (world == null || input == null || input.getItem() == null) {
            return null;
        }
        FakeCraftingInventory container = new FakeCraftingInventory(gridSize);
        for (int slot = 0; slot < gridSize * gridSize; slot++) {
            container.setInventorySlotContents(slot, ItemUtil.copyWithSizeOne(input));
        }
        ItemStack output = CraftingManager.getInstance()
            .findMatchingRecipe(container, world);
        if (output == null || output.getItem() == null || ItemUtil.areItemStacksEqual(output, input)) {
            return null;
        }
        LowerTier reverse = findLowerTier(world, output);
        if (reverse == null || !ItemUtil.areItemStacksEqual(reverse.result, input)) {
            return null;
        }
        return new HigherTier(output, gridSize * gridSize);
    }

    private static long saturatedMultiply(long value, int factor) {
        if (value <= 0L || factor <= 0) {
            return 0L;
        }
        return value > Long.MAX_VALUE / factor ? Long.MAX_VALUE : value * factor;
    }

    public static class HigherTier {

        public final ItemStack result;
        public final int inputCount;

        public HigherTier(@Nonnull ItemStack result, int inputCount) {
            this.result = ItemUtil.copyWithSizeOne(result);
            this.inputCount = Math.max(1, inputCount);
        }
    }

    public static class LowerTier {

        public final ItemStack result;
        public final int count;

        public LowerTier(@Nonnull ItemStack result, int count) {
            this.result = ItemUtil.copyWithSizeOne(result);
            this.count = Math.max(1, count);
        }
    }

    public static class ConfiguredRule {

        public final ItemStack higher;
        public final ItemStack lower;
        public final int ratio;

        public ConfiguredRule(@Nonnull ItemStack higher, @Nonnull ItemStack lower, int ratio) {
            this.higher = ItemUtil.copyWithSizeOne(higher);
            this.lower = ItemUtil.copyWithSizeOne(lower);
            this.ratio = ratio;
        }
    }

    /**
     * Minimal crafting inventory used to query the crafting manager for
     * compression and decompression recipes without touching game state.
     */
    public static class FakeCraftingInventory extends InventoryCrafting {

        private final ItemStack[] items;
        private final int size;
        private final int gridSize;

        public FakeCraftingInventory(int gridSize) {
            super(null, gridSize, gridSize);
            this.gridSize = gridSize;
            this.size = gridSize * gridSize;
            this.items = new ItemStack[this.size];
        }

        @Override
        public int getSizeInventory() {
            return size;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index < 0 || index >= size ? null : items[index];
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (index >= 0 && index < size) {
                items[index] = stack;
            }
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index < 0 || index >= size || items[index] == null) {
                return null;
            }
            ItemStack result = items[index].splitStack(count);
            if (items[index].stackSize <= 0) {
                items[index] = null;
            }
            return result;
        }

        @Override
        public ItemStack getStackInRowAndColumn(int row, int column) {
            int index = row + column * gridSize;
            return index < 0 || index >= size ? null : items[index];
        }
    }
}
