package com.hfstudio.functionalstorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class DrawerInfoPanel extends Gui {

    private final ControllableDrawerTile tile;
    private final DrawerFaceLayout face;
    private final StorageTooltipPainter previews = StorageTooltipPainter.INSTANCE;

    public DrawerInfoPanel(ControllableDrawerTile tile) {
        this.tile = tile;
        face = ((DrawerBlock) tile.getBlockType()).getFaceLayout();
    }

    public int size() {
        return tile.getActiveStorage() == null ? 0
            : Math.min(
                face.getSlotCount(),
                tile.getActiveStorage()
                    .getStorageCount());
    }

    public int x(int slot) {
        return Math.round(face.getSlotX(slot) * 48) - 8;
    }

    public int y(int slot) {
        return Math.round(face.getSlotY(slot) * 48) - 8;
    }

    public int slotAt(int x, int y) {
        for (int slot = 0; slot < size(); slot++) {
            if (x >= x(slot) && x < x(slot) + 16 && y >= y(slot) && y < y(slot) + 16) return slot;
        }
        return -1;
    }

    public void draw(int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            RenderHelper.disableStandardItemLighting();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawFluid(x, y);
            DrawerGuiTextures.INSTANCE.front(tile, x, y);
            for (int slot = 0; slot < size(); slot++) {
                Entry entry = entry(slot);
                if (entry != null) previews.drawEntry(entry, x + x(slot), y + y(slot));
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    public void outline(int x, int y, int slot, int color) {
        int left = x + x(slot) - 1;
        int top = y + y(slot) - 1;
        drawRect(left, top, left + 18, top + 1, color);
        drawRect(left, top + 17, left + 18, top + 18, color);
        drawRect(left, top, left + 1, top + 18, color);
        drawRect(left + 17, top, left + 18, top + 18, color);
    }

    public void drawFluid(int x, int y) {
        if (tile.getFluidHandler() == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            minecraft.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            for (int slot = 0; slot < size(); slot++) {
                BigFluidStack stored = tile.getFluidHandler()
                    .getSnapshot(slot);
                if (!stored.hasTemplate()) continue;
                IIcon icon = stored.getTemplate()
                    .getFluid()
                    .getStillIcon();
                if (icon == null) continue;
                int color = stored.getTemplate()
                    .getFluid()
                    .getColor(stored.getTemplate());
                GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
                int left = size() < 4 ? 6 : x(slot);
                int top = size() == 1 ? 6 : y(slot);
                int width = size() < 4 ? 36 : 16;
                int height = size() == 1 ? 36 : 16;
                for (int dx = 0; dx < width; dx += 16)
                    for (int dy = 0; dy < height; dy += 16) drawTexturedModelRectFromIcon(
                        x + left + dx,
                        y + top + dy,
                        icon,
                        Math.min(16, width - dx),
                        Math.min(16, height - dy));
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    public Entry entry(int slot) {
        if (tile.getItemHandler() != null) {
            BigItemStack stored = tile.getItemHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(stored.getTemplate(), null, null, NumberFormatUtil.formatNumberCompact(stored.getAmount()))
                : null;
        }
        if (tile.getFluidHandler() != null) {
            BigFluidStack stored = tile.getFluidHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, stored.getTemplate(), null, NumberFormatUtil.formatFluid(stored.getAmount()))
                : null;
        }
        if (tile.getAspectHandler() != null) {
            BigAspectStack stored = tile.getAspectHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    null,
                    AspectIcon.of(stored.getAspect()),
                    NumberFormatUtil.formatNumberCompact(stored.getAmount()))
                : null;
        }
        return null;
    }
}
