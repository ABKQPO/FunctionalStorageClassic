package com.hfstudio.functionalstorage.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutScreen;
import com.hfstudio.functionalstorage.common.integration.Mods;

public abstract class BaseGUI extends GuiContainer implements StorageShortcutScreen {

    protected static final int LABEL_COLOUR = 0x404040;
    private int consumedMouseButtons;

    protected BaseGUI(Container container) {
        super(container);
    }

    @Override
    public boolean isTextInputFocused() {
        return false;
    }

    @Override
    public Slot getSlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
    }

    protected void consumeMouseButton(int button) {
        consumedMouseButtons |= 1 << button;
        field_147007_t = false;
        field_147008_s.clear();
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long heldTime) {
        if ((consumedMouseButtons & 1 << button) == 0) super.mouseClickMove(x, y, button, heldTime);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        if (button >= 0) {
            boolean nei = Mods.NotEnoughItems.isModLoaded();
            boolean dropped = nei && NEIGuiIntegration.dropDraggedStack(this, x, y, button);
            if ((consumedMouseButtons & 1 << button) != 0 || dropped) {
                consumedMouseButtons &= ~(1 << button);
                field_147007_t = false;
                field_147008_s.clear();
                if (nei) NEIGuiIntegration.releaseMouse(this, x, y, button);
                onConsumedMouseReleased(x, y, button);
                return;
            }
        }
        super.mouseMovedOrUp(x, y, button);
    }

    protected void onConsumedMouseReleased(int x, int y, int button) {}

    protected void drawSlotAmount(String amount, int x, int y) {
        boolean unicode = fontRendererObj.getUnicodeFlag();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            fontRendererObj.setUnicodeFlag(false);
            int width = fontRendererObj.getStringWidth(amount);
            float scale = Math.min(0.85F, 18F / Math.max(1, width));
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glTranslatef(x + 17F, y + 16F - 7F * scale, 0F);
            GL11.glScalef(scale, scale, 1F);
            fontRendererObj.drawStringWithShadow(amount, -width, 0, 0xFFFFFF);
        } finally {
            fontRendererObj.setUnicodeFlag(unicode);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
