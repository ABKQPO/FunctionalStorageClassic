package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.inventory.FilteredItemStorage;
import com.hfstudio.functionalstorage.common.inventory.SelectedStorage;
import com.hfstudio.functionalstorage.util.ItemUtil;

public class UpgradeSettings {

    public static final int FILTER_SLOTS = 9;
    public static final String TOOL_KEY = "Tool";
    public static final String SPEED_AUGMENTS_KEY = "SpeedAugments";
    public static final String[] NESTED_SLOT_KEYS = { TOOL_KEY, SPEED_AUGMENTS_KEY };

    public record ItemFilter(ItemStack stack, String ore) {}

    private UpgradeSettings() {}

    public static IBigItemHandler itemStorage(IBigItemHandler storage, ItemStack stack) {
        AutomationUpgradeItem upgrade = (AutomationUpgradeItem) stack.getItem();
        int[] slots = upgrade.getSelectedSlots(stack);
        if (!upgrade.hasFilter(stack) && slots == null) return storage;
        return new FilteredItemStorage(storage, itemFilter(stack), slots);
    }

    public static int get(ItemStack stack, String key) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getInteger(key) : 0;
    }

    public static IBigFluidHandler fluidStorage(IBigFluidHandler storage, ItemStack stack) {
        int[] slots = ((AutomationUpgradeItem) stack.getItem()).getSelectedSlots(stack);
        return slots == null ? storage : new SelectedStorage.Fluid(storage, slots);
    }

    public static IBigAspectHandler aspectStorage(IBigAspectHandler storage, ItemStack stack) {
        int[] slots = ((AutomationUpgradeItem) stack.getItem()).getSelectedSlots(stack);
        return slots == null ? storage : new SelectedStorage.Aspect(storage, slots);
    }

    public static void set(ItemStack stack, String key, int value) {
        tag(stack).setInteger(key, value);
    }

    public static ItemStack getStack(ItemStack stack, String key) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(key, 10) ? ItemUtil.readStack(
                stack.getTagCompound()
                    .getCompoundTag(key))
                : null;
    }

    public static void setStack(ItemStack stack, String key, ItemStack value) {
        if (value == null) {
            tag(stack).removeTag(key);
        } else {
            tag(stack).setTag(key, value.writeToNBT(new NBTTagCompound()));
        }
    }

    public static boolean matches(ItemStack upgrade, ItemStack candidate) {
        return itemFilter(upgrade).test(candidate);
    }

    public static ItemStack getFilter(ItemStack upgrade, int slot) {
        return slot < 0 || slot >= FILTER_SLOTS ? null : getStack(upgrade, slot == 0 ? "Filter" : "Filter" + slot);
    }

    public static void setFilter(ItemStack upgrade, int slot, ItemStack filter) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        ItemStack template = filter == null ? null : filter.copy();
        if (template != null) template.stackSize = 1;
        setStack(upgrade, slot == 0 ? "Filter" : "Filter" + slot, template);
        tag(upgrade).removeTag("FilterOre" + slot);
    }

    public static boolean hasFilters(ItemStack upgrade) {
        if (!upgrade.hasTagCompound()) return false;
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            if (upgrade.getTagCompound()
                .hasKey(slot == 0 ? "Filter" : "Filter" + slot, 10)) return true;
        }
        return false;
    }

    public static String filterOre(ItemStack upgrade, int slot) {
        return upgrade.hasTagCompound() ? upgrade.getTagCompound()
            .getString("FilterOre" + slot) : "";
    }

    public static void cycleFilterOre(ItemStack upgrade, int slot, boolean backwards) {
        ItemStack filter = getFilter(upgrade, slot);
        if (filter == null) return;
        int[] ores = OreDictionary.getOreIDs(filter);
        String current = filterOre(upgrade, slot);
        int selected = 0;
        for (int index = 0; index < ores.length; index++) {
            if (OreDictionary.getOreName(ores[index])
                .equals(current)) selected = index + 1;
        }
        selected = Math.floorMod(selected + (backwards ? -1 : 1), ores.length + 1);
        tag(upgrade).setString("FilterOre" + slot, selected == 0 ? "" : OreDictionary.getOreName(ores[selected - 1]));
    }

    public static Predicate<ItemStack> itemFilter(ItemStack upgrade) {
        if (!hasFilters(upgrade)) return candidate -> true;
        List<ItemFilter> filters = new ArrayList<>();
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            ItemStack filter = getFilter(upgrade, slot);
            if (filter != null) filters.add(new ItemFilter(filter, filterOre(upgrade, slot)));
        }
        if (filters.isEmpty()) return candidate -> true;
        boolean blacklist = get(upgrade, "Blacklist") != 0;
        boolean strict = get(upgrade, "StrictMatching") != 0;
        boolean ore = get(upgrade, "OreMatching") != 0;
        return candidate -> {
            for (ItemFilter filter : filters) {
                ItemStack template = filter.stack();
                boolean matches = candidate != null && template.getItem() == candidate.getItem()
                    && template.getItemDamage() == candidate.getItemDamage();
                if (ore && candidate != null) {
                    if (filter.ore()
                        .isEmpty()) matches |= ItemUtil.sharesOreDictionary(template, candidate);
                    else {
                        matches = false;
                        for (int id : OreDictionary.getOreIDs(candidate)) {
                            if (filter.ore()
                                .equals(OreDictionary.getOreName(id))) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }
                if (matches && (!strict || ItemStack.areItemStackTagsEqual(template, candidate))) return !blacklist;
            }
            return blacklist;
        };
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
