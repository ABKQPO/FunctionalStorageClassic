package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;

import codechicken.nei.NEIClientConfig;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import yalter.mousetweaks.ModCompatibility;
import yalter.mousetweaks.config.MTConfig;

public class MouseTweaksShortcuts {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "MouseTweaks")
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || !(minecraft.currentScreen instanceof GuiContainer gui)
            || !StorageShortcutInput.supports(gui)
            || !enabled()) return;
        Slot slot = hovered(gui);
        if (!StorageShortcutInput.supportsSlot(gui, slot)) return;
        int wheel = Mouse.getDWheel();
        if (wheel == 0) return;
        if (slot.getStack() == null || ((StorageShortcutScreen) gui).isTextInputFocused()) return;
        ItemStack cursor = minecraft.thePlayer.inventory.getItemStack();
        if (BigItemStack.matches(cursor, slot.getStack())) return;
        boolean push = wheel < 0;
        if ((MTConfig.WheelScrollDirection == 2 || MTConfig.WheelScrollDirection == 3)
            && slot.inventory == minecraft.thePlayer.inventory) push = !push;
        if (MTConfig.WheelScrollDirection == 1 || MTConfig.WheelScrollDirection == 3) push = !push;
        int amount = ModCompatibility.isLwjgl3Loaded() || MTConfig.ScrollItemScaling != 0 ? 1
            : Math.max(1, Math.min(64, Math.abs(wheel / 120)));
        StorageShortcutInput.send(gui, slot, push ? Action.PUSH : Action.PULL, amount, MTConfig.WheelSearchOrder != 0);
    }

    @Optional.Method(modid = "MouseTweaks")
    public static boolean handlesWheel(GuiContainer gui) {
        return StorageShortcutInput.supports(gui) && enabled() && StorageShortcutInput.supportsSlot(gui, hovered(gui));
    }

    @Optional.Method(modid = "MouseTweaks")
    private static boolean enabled() {
        return MTConfig.WheelTweak && !(Mods.NotEnoughItems.isModLoaded() && neiOwnsWheel());
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static boolean neiOwnsWheel() {
        return NEIClientConfig.isMouseScrollTransferEnabled();
    }

    private static Slot hovered(GuiContainer gui) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int x = Mouse.getX() * gui.width / minecraft.displayWidth;
        int y = gui.height - Mouse.getY() * gui.height / minecraft.displayHeight - 1;
        return ((StorageShortcutScreen) gui).getSlotAt(x, y);
    }
}
