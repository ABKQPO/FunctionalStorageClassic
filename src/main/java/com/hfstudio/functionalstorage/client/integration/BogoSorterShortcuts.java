package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.cleanroommc.bogosorter.ClientEventHandler;
import com.cleanroommc.bogosorter.client.keybinds.KeyBind;
import com.cleanroommc.bogosorter.client.keybinds.control.BSKeybinds;
import com.cleanroommc.modularui.api.event.KeyboardInputEvent;
import com.cleanroommc.modularui.api.event.MouseInputEvent;
import com.cleanroommc.modularui.core.mixins.early.minecraft.GuiContainerAccessor;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BogoSorterShortcuts {

    private long lastAction;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "bogosorter")
    public void onKeyboard(KeyboardInputEvent.Pre event) {
        handle(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "bogosorter")
    public void onMouse(MouseInputEvent.Pre event) {
        handle(event);
    }

    @Optional.Method(modid = "bogosorter")
    private void handle(GuiScreenEvent event) {
        if (!(event.gui instanceof GuiContainer gui) || !StorageShortcutInput.supports(gui)
            || ((StorageShortcutScreen) gui).isTextInputFocused()) return;
        Slot slot = ((GuiContainerAccessor) gui).getHoveredSlot();
        if (!StorageShortcutInput.supportsSlot(gui, slot)) return;
        KeyBind.checkKeys(ClientEventHandler.getTicks());
        Action action = pressed(BSKeybinds.getActiveKeyBind(BSKeybinds.MOVE_ALL), false) ? Action.MOVE_ALL
            : pressed(BSKeybinds.getActiveKeyBind(BSKeybinds.MOVE_ALL_SAME), false) ? Action.MOVE_SAME
                : pressed(BSKeybinds.getActiveKeyBind(BSKeybinds.MOVE_SINGLE), true) ? Action.MOVE_ONE
                    : pressed(BSKeybinds.getActiveKeyBind(BSKeybinds.MOVE_SINGLE_EMPTY), true) ? Action.MOVE_ONE_EMPTY
                        : null;
        if (action == null) return;
        // An empty slot has nothing to move out of it, but it is still a valid
        // destination, so the actions that fill a slot stay available.
        if (!slot.getHasStack() && !fillsSlot(action)) return;
        event.setCanceled(true);
        long now = Minecraft.getSystemTime();
        if (now - lastAction < 50L) return;
        StorageShortcutInput.send(gui, slot, action, 1, false);
        lastAction = now;
    }

    @Optional.Method(modid = "bogosorter")
    private static boolean fillsSlot(Action action) {
        return action == Action.MOVE_ALL || action == Action.MOVE_SAME
            || action == Action.MOVE_ONE
            || action == Action.MOVE_ONE_EMPTY;
    }

    @Optional.Method(modid = "bogosorter")
    private boolean pressed(KeyBind key, boolean repeat) {
        return key != null && (repeat ? key.isFirstPressOrHeldLong(15) : key.isFirstPress());
    }
}
