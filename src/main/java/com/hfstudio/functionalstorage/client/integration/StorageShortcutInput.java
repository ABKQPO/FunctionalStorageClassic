package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.container.StorageTransferMenu;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;
import com.hfstudio.functionalstorage.common.network.StorageTransferMessage;

public class StorageShortcutInput {

    private StorageShortcutInput() {}

    public static boolean supports(GuiContainer gui) {
        return gui instanceof StorageShortcutScreen && gui.inventorySlots instanceof StorageTransferMenu menu
            && menu.getTransferStorage() != null
            && menu.getTransferSlotCount() > 0;
    }

    public static void send(GuiContainer gui, Slot slot, Action action, int amount, boolean reverse) {
        if (supports(gui) && supportsSlot(gui, slot)) {
            FunctionalStorage.network.sendToServer(
                new StorageTransferMessage(gui.inventorySlots.windowId, slot.slotNumber, action, amount, reverse));
        }
    }

    public static boolean supportsSlot(GuiContainer gui, Slot slot) {
        if (slot == null || !(gui.inventorySlots instanceof StorageTransferMenu menu)) return false;
        return slot.slotNumber >= 0 && slot.slotNumber < menu.getTransferSlotCount()
            || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory && slot.getSlotIndex() >= 0
                && slot.getSlotIndex() < 36;
    }

    public static boolean handlesWheel(GuiContainer gui) {
        return Mods.NotEnoughItems.isModLoaded() && NEIStorageShortcuts.handlesWheel(gui)
            || Mods.MouseTweaks.isModLoaded() && MouseTweaksShortcuts.handlesWheel(gui);
    }
}
