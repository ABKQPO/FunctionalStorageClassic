package com.hfstudio.functionalstorage.common.presentation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;

public class AdaptivePresentationRegistry {

    public static final String SORT_COUNT = "count";
    public static final String SORT_MOD = "mod";
    public static final String SORT_ITEM_ID = "item_id";
    public static final String SORT_TAG = "tag";
    public static final String SORT_CATEGORY = "category";
    public static final String LAYOUT_BALANCED = "balanced";
    public static final String LAYOUT_PROPORTIONAL = "proportional";
    public static final String LAYOUT_MANUAL = "manual";
    public static final String ORDER_UP = "up";
    public static final String ORDER_DOWN = "down";
    public static final String ORDER_NORTH = "north";
    public static final String ORDER_SOUTH = "south";
    public static final String ORDER_EAST = "east";
    public static final String ORDER_WEST = "west";

    private static final Map<String, AdaptiveSortStrategy> SORTS = new LinkedHashMap<>();
    private static final Map<String, AdaptiveLayoutStrategy> LAYOUTS = new LinkedHashMap<>();
    private static final Map<String, AdaptiveOrderStrategy> ORDERS = new LinkedHashMap<>();

    static {
        registerSort(new CountSort());
        registerSort(new ModSort());
        registerSort(new ItemIdSort());
        registerSort(new TagSort());
        registerSort(new CategorySort());
        registerLayout(new BalancedLayout());
        registerLayout(new ProportionalLayout());
        registerLayout(new ManualLayout());
        registerOrder(
            new PositionOrder(
                ORDER_UP,
                Comparator.comparingInt((ControllableDrawerTile tile) -> tile.yCoord)
                    .reversed()));
        registerOrder(
            new PositionOrder(ORDER_DOWN, Comparator.comparingInt((ControllableDrawerTile tile) -> tile.yCoord)));
        registerOrder(
            new PositionOrder(ORDER_NORTH, Comparator.comparingInt((ControllableDrawerTile tile) -> tile.zCoord)));
        registerOrder(
            new PositionOrder(
                ORDER_SOUTH,
                Comparator.comparingInt((ControllableDrawerTile tile) -> tile.zCoord)
                    .reversed()));
        registerOrder(
            new PositionOrder(
                ORDER_EAST,
                Comparator.comparingInt((ControllableDrawerTile tile) -> tile.xCoord)
                    .reversed()));
        registerOrder(
            new PositionOrder(ORDER_WEST, Comparator.comparingInt((ControllableDrawerTile tile) -> tile.xCoord)));
    }

    private AdaptivePresentationRegistry() {}

    public static synchronized void registerSort(@Nonnull AdaptiveSortStrategy strategy) {
        register(SORTS, strategy.getId(), strategy);
    }

    public static synchronized void registerLayout(@Nonnull AdaptiveLayoutStrategy strategy) {
        register(LAYOUTS, strategy.getId(), strategy);
    }

    public static synchronized void registerOrder(@Nonnull AdaptiveOrderStrategy strategy) {
        register(ORDERS, strategy.getId(), strategy);
    }

    @Nonnull
    public static AdaptiveSortStrategy sort(@Nonnull String id) {
        return SORTS.getOrDefault(id, SORTS.get(SORT_COUNT));
    }

    @Nonnull
    public static AdaptiveLayoutStrategy layout(@Nonnull String id) {
        return LAYOUTS.getOrDefault(id, LAYOUTS.get(LAYOUT_BALANCED));
    }

    @Nonnull
    public static AdaptiveOrderStrategy order(@Nonnull String id) {
        return ORDERS.getOrDefault(id, ORDERS.get(ORDER_UP));
    }

    @Nonnull
    public static String resolveSortId(@Nonnull String id) {
        return SORTS.containsKey(id) ? id : SORT_COUNT;
    }

    @Nonnull
    public static String resolveLayoutId(@Nonnull String id) {
        return LAYOUTS.containsKey(id) ? id : LAYOUT_BALANCED;
    }

    @Nonnull
    public static String resolveOrderId(@Nonnull String id) {
        return ORDERS.containsKey(id) ? id : ORDER_UP;
    }

    @Nonnull
    public static String nextSortId(@Nonnull String current) {
        return nextId(SORTS, current);
    }

    @Nonnull
    public static String nextLayoutId(@Nonnull String current) {
        return nextId(LAYOUTS, current);
    }

    @Nonnull
    public static String nextOrderId(@Nonnull String current) {
        return nextId(ORDERS, current);
    }

    private static <T> void register(Map<String, T> target, String id, T value) {
        if (id == null || id.isEmpty() || target.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Adaptive presentation strategy ID is already registered: " + id);
        }
    }

    private static <T> String nextId(Map<String, T> values, String current) {
        List<String> ids = new ArrayList<>(values.keySet());
        int index = ids.indexOf(current);
        return ids.get(index < 0 ? 0 : (index + 1) % ids.size());
    }

    private static String resourceId(StorageSnapshot<?, ?> snapshot) {
        if (snapshot.getKey() instanceof FluidStorageKey fluid) {
            String name = FluidRegistry.getDefaultFluidName(fluid.getFluid());
            return name == null ? fluid.getFluid()
                .getName() : name;
        }
        if (!(snapshot instanceof BigItemStack)) {
            return Mods.Thaumcraft.isModLoaded() ? aspectId(snapshot) : "";
        }
        ItemStack stack = itemTemplate(snapshot);
        Item item = stack == null ? null : stack.getItem();
        String name = item == null ? "" : Item.itemRegistry.getNameForObject(item);
        return name == null ? "" : name.toLowerCase(Locale.ENGLISH) + ':' + stack.getItemDamage();
    }

    @Optional.Method(modid = "Thaumcraft")
    private static String aspectId(StorageSnapshot<?, ?> snapshot) {
        return snapshot.getKey() instanceof AspectStorageKey aspect ? "thaumcraft:" + aspect.getTag() : "";
    }

    private static ItemStack itemTemplate(StorageSnapshot<?, ?> snapshot) {
        return snapshot instanceof BigItemStack item ? item.getTemplate() : null;
    }

    private static String modId(StorageSnapshot<?, ?> snapshot) {
        String name = resourceId(snapshot);
        int separator = name == null ? -1 : name.indexOf(':');
        return separator < 0 ? ""
            : name.substring(0, separator)
                .toLowerCase(Locale.ENGLISH);
    }

    private static String primaryOreName(StorageSnapshot<?, ?> snapshot) {
        ItemStack stack = itemTemplate(snapshot);
        if (stack == null || stack.getItem() == null) {
            return "~untagged";
        }
        String result = "~untagged";
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if (name != null && name.compareTo(result) < 0) {
                result = name;
            }
        }
        return result;
    }

    private static String category(StorageSnapshot<?, ?> snapshot) {
        if (snapshot.getKey() instanceof FluidStorageKey) return "fluid";
        if (!(snapshot instanceof BigItemStack)) return "aspect";
        ItemStack stack = itemTemplate(snapshot);
        if (stack == null || stack.getItem() == null) {
            return "~empty";
        }
        if (stack.getItem() instanceof ItemBlock) {
            return "block";
        }
        if (stack.getMaxStackSize() == 1) {
            return "equipment";
        }
        return "item";
    }

    public static class CountSort implements AdaptiveSortStrategy {

        @Override
        public String getId() {
            return SORT_COUNT;
        }

        @Override
        public void sort(List<AdaptiveDisplayEntry> entries) {
            entries.sort(
                Comparator.comparingLong(
                    (AdaptiveDisplayEntry entry) -> entry.content()
                        .getAmount())
                    .reversed()
                    .thenComparing(entry -> resourceId(entry.content())));
        }
    }

    public static class ModSort implements AdaptiveSortStrategy {

        @Override
        public String getId() {
            return SORT_MOD;
        }

        @Override
        public void sort(List<AdaptiveDisplayEntry> entries) {
            entries.sort(
                Comparator.comparing((AdaptiveDisplayEntry entry) -> modId(entry.content()))
                    .thenComparing(entry -> resourceId(entry.content())));
        }
    }

    public static class ItemIdSort implements AdaptiveSortStrategy {

        @Override
        public String getId() {
            return SORT_ITEM_ID;
        }

        @Override
        public void sort(List<AdaptiveDisplayEntry> entries) {
            entries.sort(Comparator.comparing(entry -> resourceId(entry.content())));
        }
    }

    public static class TagSort implements AdaptiveSortStrategy {

        @Override
        public String getId() {
            return SORT_TAG;
        }

        @Override
        public void sort(List<AdaptiveDisplayEntry> entries) {
            entries.sort(
                Comparator.comparing((AdaptiveDisplayEntry entry) -> primaryOreName(entry.content()))
                    .thenComparing(entry -> resourceId(entry.content())));
        }
    }

    public static class CategorySort implements AdaptiveSortStrategy {

        @Override
        public String getId() {
            return SORT_CATEGORY;
        }

        @Override
        public void sort(List<AdaptiveDisplayEntry> entries) {
            entries.sort(
                Comparator.comparing((AdaptiveDisplayEntry entry) -> category(entry.content()))
                    .thenComparing(entry -> resourceId(entry.content())));
        }
    }

    public static class BalancedLayout implements AdaptiveLayoutStrategy {

        @Override
        public String getId() {
            return LAYOUT_BALANCED;
        }

        @Override
        public List<Integer> allocate(int entryCount, List<Integer> capacities, List<Integer> requested) {
            return spread(entryCount, capacities, requested, false);
        }
    }

    public static class ProportionalLayout implements AdaptiveLayoutStrategy {

        @Override
        public String getId() {
            return LAYOUT_PROPORTIONAL;
        }

        @Override
        public List<Integer> allocate(int entryCount, List<Integer> capacities, List<Integer> requested) {
            return spread(entryCount, capacities, requested, true);
        }
    }

    public static class ManualLayout implements AdaptiveLayoutStrategy {

        @Override
        public String getId() {
            return LAYOUT_MANUAL;
        }

        @Override
        public List<Integer> allocate(int entryCount, List<Integer> capacities, List<Integer> requested) {
            List<Integer> result = new ArrayList<>(capacities.size());
            for (int index = 0; index < capacities.size(); index++) {
                int capacity = Math.max(0, capacities.get(index));
                int wanted = index < requested.size() ? Math.max(0, requested.get(index)) : 0;
                int cells = Math.min(capacity, wanted);
                result.add(cells);
            }
            return result;
        }
    }

    private static List<Integer> spread(int entryCount, List<Integer> capacities, List<Integer> requested,
        boolean proportional) {
        List<Integer> result = new ArrayList<>(capacities.size());
        long totalCapacity = 0L;
        for (int capacity : capacities) totalCapacity += Math.max(0, capacity);
        int remaining = (int) Math.min(Math.max(0, entryCount), totalCapacity);
        for (int index = 0; index < capacities.size(); index++) result.add(0);
        if (remaining == 0 || totalCapacity == 0) return result;

        if (proportional) {
            for (int index = 0; index < capacities.size(); index++) {
                int capacity = Math.max(0, capacities.get(index));
                int cells = (int) ((long) remaining * capacity / totalCapacity);
                cells = Math.min(capacity, cells);
                result.set(index, cells);
            }
        }
        int allocated = sum(result);
        while (allocated < remaining) {
            for (int index = 0; index < capacities.size() && allocated < remaining; index++) {
                if (result.get(index) < Math.max(0, capacities.get(index))) {
                    result.set(index, result.get(index) + 1);
                    allocated++;
                }
            }
        }
        return result;
    }

    private static int sum(List<Integer> values) {
        int total = 0;
        for (int value : values) total += Math.max(0, value);
        return total;
    }

    public static class PositionOrder implements AdaptiveOrderStrategy {

        private final String id;
        private final Comparator<ControllableDrawerTile> primary;

        private PositionOrder(String id, Comparator<ControllableDrawerTile> primary) {
            this.id = id;
            this.primary = primary.thenComparingInt(tile -> tile.xCoord)
                .thenComparingInt(tile -> tile.yCoord)
                .thenComparingInt(tile -> tile.zCoord);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void sort(List<ControllableDrawerTile> drawers) {
            drawers.sort(primary);
        }
    }
}
