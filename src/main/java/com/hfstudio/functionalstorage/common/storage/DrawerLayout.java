package com.hfstudio.functionalstorage.common.storage;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import lombok.Getter;

/** Stable serialized layout identifiers and per-slot base capacities. */
public enum DrawerLayout {

    X_1("1x1", 1, 1.0D),
    X_2("1x2", 2, 0.5D),
    X_4("2x2", 4, 0.25D);

    private static final Map<String, DrawerLayout> BY_ID = new HashMap<>();

    static {
        for (DrawerLayout layout : values()) {
            BY_ID.put(layout.id, layout);
        }
    }

    @Getter
    private final String id;
    @Getter
    private final int slotCount;
    private final double capacityScale;

    DrawerLayout(String id, int slotCount, double capacityScale) {
        this.id = id;
        this.slotCount = slotCount;
        this.capacityScale = capacityScale;
    }

    /**
     * Resolves a stable identifier.
     *
     * @param id serialized layout identifier
     * @return the matching layout, or {@link #X_1} when the identifier is unknown
     */
    public static DrawerLayout fromId(String id) {
        DrawerLayout layout = id == null ? null : BY_ID.get(id);
        return layout == null ? X_1 : layout;
    }

    public static DrawerLayout fromStorage(NBTTagCompound tag, String storageKey, DrawerLayout fallback) {
        if (tag.hasKey("DrawerLayout")) {
            return fromId(tag.getString("DrawerLayout"));
        }
        int slots = fallback.slotCount;
        NBTTagList entries = tag.getCompoundTag(storageKey)
            .getTagList("Entries", 10);
        for (int index = 0; index < entries.tagCount(); index++) {
            slots = Math.max(
                slots,
                entries.getCompoundTagAt(index)
                    .getInteger("Index") + 1);
        }
        return slots > 2 ? X_4 : slots > 1 ? X_2 : X_1;
    }

    /**
     * Resolves the layout used by a block metadata value.
     *
     * @param index metadata index
     * @return the matching layout, or {@link #X_1} when out of range
     */
    public static DrawerLayout byIndex(int index) {
        DrawerLayout[] values = values();
        return index < 0 || index >= values.length ? X_1 : values[index];
    }

    public int getIndex() {
        return ordinal();
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
