package com.hfstudio.functionalstorage.common.block;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import lombok.Getter;

/**
 * Physical surface to which a drawer block is attached. Values are ordered so
 * that {@code ordinal()} doubles as the metadata group index.
 */
@Getter
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

    public ForgeDirection asDirection() {
        return switch (this) {
            case FLOOR -> ForgeDirection.UP;
            case CEILING -> ForgeDirection.DOWN;
            default -> ForgeDirection.NORTH;
        };
    }

    public String getLocalizedName() {
        return StatCollector.translateToLocal("functionalstorage.attachment." + id);
    }

    @Override
    public String toString() {
        return id;
    }
}
