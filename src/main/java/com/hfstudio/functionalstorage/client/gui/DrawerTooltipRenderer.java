package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gtnewhorizon.gtnhlib.client.event.RenderTooltipEvent;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Section;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DrawerTooltipRenderer extends Gui {

    private ItemStack cachedStack;
    private NBTTagCompound cachedTag;
    private List<Section> cachedSections = List.of();
    private final RenderItem itemRenderer = new RenderItem();

    @SubscribeEvent
    public void onTooltip(RenderTooltipEvent event) {
        List<Section> sections = sectionsFor(event.itemStack);
        if (!sections.isEmpty() && event.alternativeRenderer == null) {
            event.alternativeRenderer = lines -> draw(event, lines, sections);
        }
    }

    private List<Section> sectionsFor(ItemStack stack) {
        if (!(stack.getItem() instanceof AutomationUpgradeItem)
            && !(stack.getItem() instanceof ItemBlock item && item.field_150939_a instanceof DrawerBlock)) {
            return List.of();
        }
        if (cachedStack != stack || !Objects.equals(cachedTag, stack.getTagCompound())) {
            cachedStack = stack;
            cachedTag = cachedStack.hasTagCompound() ? (NBTTagCompound) cachedStack.getTagCompound()
                .copy() : null;
            cachedSections = DrawerTooltipData.read(cachedStack);
        }
        return cachedSections;
    }

    public void draw(RenderTooltipEvent event, List<String> original, List<Section> sections) {
        List<String> lines = new ArrayList<>();
        int maxWidth = Math.clamp(event.gui.width - 20, 80, 300);
        int width = 0;
        for (String line : original) {
            for (String wrapped : event.font.listFormattedStringToWidth(line, maxWidth)) {
                lines.add(wrapped);
                width = Math.max(width, event.font.getStringWidth(wrapped));
            }
        }
        int previewIndex = lines.size();
        int columns = Math.max(1, Math.min(9, maxWidth / 20));
        int remaining = Math.max(0, event.gui.height - 16 - lines.size() * 10 - sections.size() * 13 - 12);
        List<Section> visible = new ArrayList<>(sections.size());
        int hidden = 0;
        for (Section section : sections) {
            int count = Math.min(
                section.entries()
                    .size(),
                remaining / 20 * columns);
            if (count > 0) {
                visible.add(
                    new Section(
                        section.title(),
                        section.entries()
                            .subList(0, count)));
                remaining -= ((count + columns - 1) / columns) * 20;
            }
            hidden += section.entries()
                .size() - count;
        }
        sections = visible;
        String overflow = hidden > 0
            ? StatCollector.translateToLocalFormatted("gui.functionalstorage.preview_more", hidden)
            : "";
        if (hidden > 0) width = Math.max(width, event.font.getStringWidth(overflow));
        int height = lines.size() * 10 + 2;
        for (Section section : sections) {
            width = Math.max(
                width,
                Math.max(
                    event.font.getStringWidth(StatCollector.translateToLocal(section.title())),
                    Math.min(
                        columns,
                        section.entries()
                            .size())
                        * 20));
            height += 13 + ((section.entries()
                .size() + columns
                - 1) / columns) * 20;
        }
        if (hidden > 0) height += 12;
        int x = event.x + 12;
        if (x + width + 4 > event.gui.width) {
            x = Math.max(4, event.x - width - 16);
        }
        int y = Math.max(4, Math.min(event.y - 12, event.gui.height - height - 6));
        float oldZ = itemRenderer.zLevel;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            zLevel = 300F;
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            drawGradientRect(x - 4, y - 4, x + width + 4, y + height + 4, event.backgroundStart, event.backgroundEnd);
            drawGradientRect(x - 3, y - 3, x + width + 3, y - 2, event.borderStart, event.borderStart);
            drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, event.borderEnd, event.borderEnd);
            drawGradientRect(x - 3, y - 2, x - 2, y + height + 2, event.borderStart, event.borderEnd);
            drawGradientRect(x + width + 2, y - 2, x + width + 3, y + height + 2, event.borderStart, event.borderEnd);
            GL11.glTranslatef(0F, 0F, 310F);
            for (int index = 0; index < previewIndex; index++) {
                event.font.drawStringWithShadow(lines.get(index), x, y, 0xFFFFFF);
                y += 10;
            }
            y += 2;
            for (Section section : sections) {
                GL11.glDisable(GL11.GL_LIGHTING);
                event.font.drawStringWithShadow(StatCollector.translateToLocal(section.title()), x, y, 0xFFAA00);
                y += 13;
                for (int index = 0; index < section.entries()
                    .size(); index++) {
                    drawEntry(
                        section.entries()
                            .get(index),
                        x + index % columns * 20,
                        y + index / columns * 20);
                }
                y += ((section.entries()
                    .size() + columns
                    - 1) / columns) * 20;
            }
            if (hidden > 0) {
                event.font.drawStringWithShadow(overflow, x, y + 2, 0xAAAAAA);
                y += 12;
            }
            for (int index = previewIndex; index < lines.size(); index++) {
                event.font.drawStringWithShadow(lines.get(index), x, y, 0xFFFFFF);
                y += 10;
            }
        } finally {
            itemRenderer.zLevel = oldZ;
            zLevel = 0F;
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    public void drawEntry(Entry entry, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float oldZ = itemRenderer.zLevel;
        try {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            if (entry.item() != null) {
                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                itemRenderer.zLevel = 200F;
                itemRenderer.renderItemAndEffectIntoGUI(
                    minecraft.fontRenderer,
                    minecraft.getTextureManager(),
                    entry.item(),
                    x,
                    y);
            } else {
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                if (entry.fluid() != null) {
                    IIcon icon = entry.fluid()
                        .getFluid()
                        .getStillIcon();
                    minecraft.getTextureManager()
                        .bindTexture(TextureMap.locationBlocksTexture);
                    tint(
                        entry.fluid()
                            .getFluid()
                            .getColor(entry.fluid()));
                    if (icon != null) {
                        drawTexturedModelRectFromIcon(x, y, icon, 16, 16);
                    }
                } else {
                    minecraft.getTextureManager()
                        .bindTexture(
                            entry.aspect()
                                .texture());
                    tint(
                        entry.aspect()
                            .color());
                    func_146110_a(x, y, 0, 0, 16, 16, 16, 16);
                }
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            if (!entry.amount()
                .isEmpty()) {
                GL11.glTranslatef(0, 0, 250);
                int textWidth = minecraft.fontRenderer.getStringWidth(entry.amount());
                float scale = Math.min(1F, 18F / Math.max(1, textWidth));
                GL11.glTranslatef(x + 17, y + 17 - 8 * scale, 0);
                GL11.glScalef(scale, scale, 1);
                minecraft.fontRenderer.drawStringWithShadow(entry.amount(), -textWidth, 0, 0xFFFFFF);
            }
        } finally {
            itemRenderer.zLevel = oldZ;
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void tint(int color) {
        GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
    }
}
