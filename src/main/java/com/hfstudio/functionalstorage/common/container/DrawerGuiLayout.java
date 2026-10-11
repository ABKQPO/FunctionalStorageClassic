package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;

public class DrawerGuiLayout {

    private final int storageSlots;
    private final DrawerFaceLayout face;
    private final int columns;
    private final int extraRows;

    public DrawerGuiLayout(int storageSlots, DrawerFaceLayout face) {
        this(storageSlots, face, 9, 0);
    }

    public DrawerGuiLayout(int storageSlots, DrawerFaceLayout face, int columns, int extraRows) {
        this.storageSlots = storageSlots;
        this.face = face;
        this.columns = Math.max(1, columns);
        this.extraRows = Math.max(0, extraRows);
    }

    public int storageX(int slot) {
        return storageSlots <= face.getSlotCount() ? 64 + Math.round(face.getSlotX(slot) * 48F) - 8
            : 8 + slot % columns * 18;
    }

    public int storageY(int slot) {
        return storageSlots <= face.getSlotCount() ? 16 + Math.round(face.getSlotY(slot) * 48F) - 8
            : 18 + slot / columns * 18;
    }

    public int upgradeY() {
        return storageSlots <= 4 ? 70 + extraRows * 18
            : 34 + (storageSlots + columns - 1) / columns * 18 + extraRows * 18;
    }

    public int inventoryY() {
        return upgradeY() + 34;
    }

    public int height() {
        return inventoryY() + 82;
    }
}
