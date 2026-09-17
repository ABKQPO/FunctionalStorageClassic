package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;

public class DrawerGuiLayout {

    private final int storageSlots;
    private final DrawerFaceLayout face;

    public DrawerGuiLayout(int storageSlots, DrawerFaceLayout face) {
        this.storageSlots = storageSlots;
        this.face = face;
    }

    public int storageX(int slot) {
        return storageSlots <= face.getSlotCount() ? 64 + Math.round(face.getSlotX(slot) * 48F) - 8 : 8 + slot % 9 * 18;
    }

    public int storageY(int slot) {
        return storageSlots <= face.getSlotCount() ? 16 + Math.round(face.getSlotY(slot) * 48F) - 8
            : 18 + slot / 9 * 18;
    }

    public int upgradeY() {
        return storageSlots <= 4 ? 70 : 34 + (storageSlots + 8) / 9 * 18;
    }

    public int inventoryY() {
        return upgradeY() + 34;
    }

    public int height() {
        return inventoryY() + 82;
    }
}
