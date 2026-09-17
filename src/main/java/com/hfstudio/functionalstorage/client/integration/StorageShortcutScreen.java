package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.inventory.Slot;

public interface StorageShortcutScreen {

    boolean isTextInputFocused();

    Slot getSlotAt(int mouseX, int mouseY);
}
