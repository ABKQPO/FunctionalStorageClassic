package com.hfstudio.functionalstorage.common.storage;

import java.util.HashMap;
import java.util.Map;

import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * Server-side drawer layout data with stable serialized identifiers.
 *
 * <p>
 * This type deliberately contains no rendering or GUI coordinates. The base
 * capacity is the capacity multiplier for each slot before upgrades apply.
 * </p>
 */
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

    private final String id;
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

    /**
     * @return the stable string used in NBT and other persistent data
     */
    public String getId() {
        return id;
    }

    /**
     * @return the number of independent slots
     */
    public int getSlotCount() {
        return slotCount;
    }

    /**
     * @return the metadata index of this layout
     */
    public int getIndex() {
        return ordinal();
    }

    /**
     * @return the unmodified item capacity of each slot
     */
    public long getItemCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseItemCapacity * capacityScale));
    }

    /**
     * @return the unmodified fluid capacity of each tank in millibuckets
     */
    public long getFluidCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseFluidCapacity * capacityScale));
    }

    /**
     * @return the unmodified essentia capacity of each slot
     */
    public long getAspectCapacity() {
        return Math.max(0L, (long) Math.floor(FunctionalStorageConfig.STORAGE.baseAspectCapacity * capacityScale));
    }
}
