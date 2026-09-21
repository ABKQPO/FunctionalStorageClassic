package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.common.Optional;

@Optional.Interface(
    iface = "codechicken.nei.guihook.IContainerInputHandler",
    modid = "NotEnoughItems",
    striprefs = true)
public class NEIStorageShortcuts implements IContainerInputHandler {

    @Optional.Method(modid = "NotEnoughItems")
    public static void register() {
        GuiContainerManager.inputHandlers.addFirst(new NEIStorageShortcuts());
    }

    @Optional.Method(modid = "NotEnoughItems")
    public static boolean handlesWheel(GuiContainer gui) {
        return StorageShortcutInput.supports(gui) && enabled()
            && StorageShortcutInput.supportsSlot(gui, GuiContainerManager.getSlotMouseOver(gui));
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static boolean enabled() {
        return NEIClientConfig.isEnabled() && NEIClientConfig.isMouseScrollTransferEnabled();
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public boolean mouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {
        if (!StorageShortcutInput.supports(gui) || !enabled()) return false;
        Slot slot = GuiContainerManager.getSlotMouseOver(gui);
        if (!StorageShortcutInput.supportsSlot(gui, slot)) return false;
        // An empty slot cannot be drained but is still a valid target, so the
        // server decides whether the scroll does anything instead of the client
        // rejecting it up front.
        if (scrolled != 0 && !((StorageShortcutScreen) gui).isTextInputFocused()) {
            boolean push = (scrolled > 0) != NEIClientConfig.shouldInvertMouseScrollTransfer();
            StorageShortcutInput.send(gui, slot, push ? Action.PUSH : Action.PULL, 1, false);
        }
        return true;
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char character, int key) {
        return false;
    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char character, int key) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int x, int y, int button) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char character, int key) {}

    @Override
    public void onMouseClicked(GuiContainer gui, int x, int y, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int x, int y, int button) {}

    @Override
    public void onMouseScrolled(GuiContainer gui, int x, int y, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int x, int y, int button, long heldTime) {}
}
