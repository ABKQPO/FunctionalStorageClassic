package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.StorageTransferMenu;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;
import com.hfstudio.functionalstorage.common.network.StorageTransferMessage;

public class StorageShortcutInput {

    public static boolean supports(GuiContainer gui) {
        return gui instanceof StorageShortcutScreen && gui.inventorySlots instanceof StorageTransferMenu menu
            && menu.hasTransferStorage()
            && menu.getTransferSlotCount() > 0;
    }

    public static void send(GuiContainer gui, Slot slot, Action action, int amount, boolean reverse) {
        if (supports(gui) && supportsSlot(gui, slot)) {
            ContainerDrawer menu = gui.inventorySlots instanceof ContainerDrawer drawer ? drawer : null;
            FunctionalStorage.network.sendToServer(
                new StorageTransferMessage(
                    gui.inventorySlots.windowId,
                    slot.slotNumber,
                    action,
                    amount,
                    reverse,
                    menu == null ? 0L : menu.getSession(),
                    menu == null ? 0L : menu.getViewRevision()));
        }
    }

    public static boolean supportsSlot(GuiContainer gui, Slot slot) {
        if (slot == null || !(gui.inventorySlots instanceof StorageTransferMenu menu)) return false;
        return slot.slotNumber >= 0 && slot.slotNumber < menu.getTransferSlotCount()
            || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory && slot.getSlotIndex() >= 0
                && slot.getSlotIndex() < 36;
    }

    public static boolean hasTransferResource(GuiContainer gui, Slot slot) {
        if (!supportsSlot(gui, slot)) return false;
        if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) return slot.getStack() != null;
        StorageTransferMenu menu = (StorageTransferMenu) gui.inventorySlots;
        int index = slot.getSlotIndex();
        if (menu.isTransferViewVirtual()) {
            return menu.hasTransferStorage() && (menu.getTransferItemSnapshot(index)
                .hasTemplate()
                || menu.getTransferFluidSnapshot(index)
                    .hasTemplate()
                || menu.getTransferAspectSnapshot(index)
                    .hasTemplate());
        }
        if (menu.getTransferFluidStorage() != null) {
            BigFluidStack fluid = menu.getTransferFluidSnapshot(index);
            return fluid.hasTemplate();
        }
        if (menu.getTransferAspectStorage() != null) {
            BigAspectStack aspect = menu.getTransferAspectSnapshot(index);
            return aspect.hasTemplate();
        }
        BigItemStack item = menu.getTransferItemSnapshot(index);
        return item.hasTemplate();
    }

    public static boolean handlesWheel(GuiContainer gui) {
        return Mods.NotEnoughItems.isModLoaded() && NEIStorageShortcuts.handlesWheel(gui)
            || Mods.MouseTweaks.isModLoaded() && MouseTweaksShortcuts.handlesWheel(gui);
    }
}
