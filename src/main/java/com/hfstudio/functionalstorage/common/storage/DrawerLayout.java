package com.hfstudio.functionalstorage.common.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import lombok.Getter;

@Getter
public class DrawerLayout {

    private static final Map<String, DrawerLayout> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, DrawerLayout> BY_INDEX = new LinkedHashMap<>();
    private static final Map<Integer, DrawerLayout> BY_SLOTS = new LinkedHashMap<>();

    public static final DrawerLayout X_1 = register(
        new DrawerLayout("1x1", 0, 1.0D, DrawerFaceLayout.X_1, "PPP", "PCP", "PPP"));
    public static final DrawerLayout X_2 = register(
        new DrawerLayout("1x2", 1, 0.5D, DrawerFaceLayout.X_2, "PCP", "PPP", "PCP"));
    public static final DrawerLayout X_4 = register(
        new DrawerLayout("2x2", 2, 0.25D, DrawerFaceLayout.X_4, "CPC", "PPP", "CPC"));

    private final String id;
    private final int index;
    private final int slotCount;
    private final double capacityScale;
    private final DrawerFaceLayout faceLayout;
    private final String[] recipePattern;

    public DrawerLayout(String id, int index, double capacityScale, DrawerFaceLayout faceLayout,
        String... recipePattern) {
        this.id = Objects.requireNonNull(id, "id");
        if (index < 0 || !Double.isFinite(capacityScale) || capacityScale <= 0D) {
            throw new IllegalArgumentException(
                "Layouts require a nonnegative index and positive finite capacity scale");
        }
        this.index = index;
        this.capacityScale = capacityScale;
        this.faceLayout = Objects.requireNonNull(faceLayout, "faceLayout");
        this.slotCount = faceLayout.getSlotCount();
        if (recipePattern.length != 3) throw new IllegalArgumentException("Drawer recipes require three rows");
        this.recipePattern = recipePattern.clone();
        for (String row : this.recipePattern) {
            if (row == null || !row.matches("[PC ]{3}")) {
                throw new IllegalArgumentException(
                    "Drawer recipe rows must contain three frame, center, or empty cells");
            }
        }
    }

    public static synchronized DrawerLayout register(DrawerLayout layout) {
        if (BY_ID.containsKey(layout.id) || BY_INDEX.containsKey(layout.index)) {
            throw new IllegalArgumentException("Layout identifier or metadata index already registered: " + layout.id);
        }
        BY_ID.put(layout.id, layout);
        BY_INDEX.put(layout.index, layout);
        BY_SLOTS.putIfAbsent(layout.slotCount, layout);
        return layout;
    }

    public static synchronized DrawerLayout[] values() {
        return BY_ID.values()
            .toArray(new DrawerLayout[0]);
    }

    public static DrawerLayout fromId(String id) {
        DrawerLayout layout = id == null ? null : BY_ID.get(id);
        return layout == null ? X_1 : layout;
    }

    public String[] getRecipePattern() {
        return recipePattern.clone();
    }

    public static DrawerLayout fromStorage(NBTTagCompound tag, String storageKey, DrawerLayout fallback) {
        if (tag.hasKey("DrawerLayout")) return fromId(tag.getString("DrawerLayout"));
        int slots = fallback.slotCount;
        NBTTagList entries = tag.getCompoundTag(storageKey)
            .getTagList("Entries", 10);
        for (int entry = 0; entry < entries.tagCount(); entry++) {
            slots = Math.max(
                slots,
                entries.getCompoundTagAt(entry)
                    .getInteger("Index") + 1);
        }
        DrawerLayout layout = bySlotCount(slots);
        if (layout != null) return layout;
        DrawerLayout nearest = null;
        for (DrawerLayout candidate : BY_ID.values()) {
            if (candidate.slotCount >= slots && (nearest == null || candidate.slotCount < nearest.slotCount)) {
                nearest = candidate;
            }
        }
        return nearest == null ? fallback : nearest;
    }

    public static DrawerLayout bySlotCount(int slots) {
        return BY_SLOTS.get(slots);
    }

    public static DrawerLayout byIndex(int index) {
        DrawerLayout layout = BY_INDEX.get(index);
        return layout == null ? X_1 : layout;
    }

    public long getItemCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseItemCapacity * capacityScale));
    }

    public long getFluidCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseFluidCapacity * capacityScale));
    }

    public long getAspectCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseAspectCapacity * capacityScale));
    }
}
