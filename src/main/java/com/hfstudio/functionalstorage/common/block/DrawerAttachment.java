package com.hfstudio.functionalstorage.common.block;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Physical surface to which a drawer block is attached. Values are ordered so
 * that {@code ordinal()} doubles as the metadata group index.
 */
public enum DrawerAttachment {

    WALL("wall"),
    FLOOR("floor"),
    CEILING("ceiling");

    private final String id;

    DrawerAttachment(String id) {
        this.id = id;
    }

    /**
     * Resolves an attachment from its metadata group index.
     *
     * @param index metadata group index
     * @return the matching attachment, or {@link #WALL} when out of range
     */
    public static DrawerAttachment byIndex(int index) {
        DrawerAttachment[] values = values();
        return index < 0 || index >= values.length ? WALL : values[index];
    }

    /**
     * @return the stable low-case identifier used by blockstate files
     */
    public String getId() {
        return id;
    }

    /**
     * @return the outward direction this attachment faces
     */
    public ForgeDirection asDirection() {
        switch (this) {
            case FLOOR:
                return ForgeDirection.UP;
            case CEILING:
                return ForgeDirection.DOWN;
            default:
                return ForgeDirection.NORTH;
        }
    }

    /**
     * @return the localized display name
     */
    public String getLocalizedName() {
        return StatCollector.translateToLocal("functionalstorage.attachment." + id);
    }

    @Override
    public String toString() {
        return id;
    }
}
