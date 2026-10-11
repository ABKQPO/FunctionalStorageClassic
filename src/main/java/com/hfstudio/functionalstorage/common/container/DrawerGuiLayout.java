package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;

import lombok.Getter;

public class DrawerGuiLayout {

    public static final int TERMINAL_GRID_X = 8;
    public static final int TERMINAL_GRID_Y = 20;
    public static final int TERMINAL_COLUMNS = 9;
    public static final int TERMINAL_ROWS = 3;
    public static final int TERMINAL_SCROLLBAR_X = 174;
    public static final int TERMINAL_SCROLLBAR_Y = 20;
    public static final int TERMINAL_SCROLLBAR_TRAVEL = 37;
    public static final int TERMINAL_SCROLLBAR_WIDTH = 12;
    public static final int TERMINAL_SCROLLBAR_HEIGHT = 15;
    public static final int TERMINAL_SEARCH_X = 96;
    public static final int TERMINAL_SEARCH_Y = 7;
    public static final int TERMINAL_SEARCH_WIDTH = 70;
    public static final int TERMINAL_SEARCH_HEIGHT = 9;
    public static final int TERMINAL_WIDTH = 193;
    public static final int TERMINAL_CONTROL_STRIP = 18;
    public static final int TERMINAL_TEXTURE_HEIGHT = 172;
    public static final int TERMINAL_HEIGHT = TERMINAL_TEXTURE_HEIGHT + TERMINAL_CONTROL_STRIP;
    public static final int CRAFTING_TERMINAL_HEIGHT = 229;
    public static final int TERMINAL_STRIP_Y = 73;
    public static final int TERMINAL_STRIP_FILLER_Y = 84;
    public static final int TERMINAL_INVENTORY_Y = 90 + TERMINAL_CONTROL_STRIP;
    public static final int CRAFTING_TERMINAL_INVENTORY_Y = 147;
    public static final int CRAFTING_MATRIX_X = 26;
    public static final int CRAFTING_MATRIX_Y = 78;
    public static final int CRAFTING_RESULT_X = 134;
    public static final int CRAFTING_RESULT_Y = 96;

    private final int storageSlots;
    private final DrawerFaceLayout face;
    @Getter
    private final boolean terminal;
    private final int inventoryY;

    public DrawerGuiLayout(int storageSlots, DrawerFaceLayout face) {
        this(storageSlots, face, false, false);
    }

    public DrawerGuiLayout(int storageSlots, DrawerFaceLayout face, boolean terminal, boolean crafting) {
        this.storageSlots = storageSlots;
        this.face = face;
        this.terminal = terminal;
        this.inventoryY = !terminal ? -1 : crafting ? CRAFTING_TERMINAL_INVENTORY_Y : TERMINAL_INVENTORY_Y;
    }

    public int storageX(int slot) {
        if (terminal) return TERMINAL_GRID_X + slot % TERMINAL_COLUMNS * 18;
        return storageSlots <= face.getSlotCount() ? 64 + Math.round(face.getSlotX(slot) * 48F) - 8 : 8 + slot % 9 * 18;
    }

    public int storageY(int slot) {
        if (terminal) return TERMINAL_GRID_Y + slot / TERMINAL_COLUMNS * 18;
        return storageSlots <= face.getSlotCount() ? 16 + Math.round(face.getSlotY(slot) * 48F) - 8
            : 18 + slot / 9 * 18;
    }

    public int upgradeY() {
        if (terminal) return inventoryY - 34;
        return storageSlots <= 4 ? 70 : 34 + (storageSlots + 8) / 9 * 18;
    }

    public int inventoryY() {
        return terminal ? inventoryY : upgradeY() + 34;
    }

    public int height() {
        return terminal ? inventoryY + 82 : inventoryY() + 82;
    }
}
