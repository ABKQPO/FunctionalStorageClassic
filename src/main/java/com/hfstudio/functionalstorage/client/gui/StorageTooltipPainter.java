package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Drawing primitives shared by every storage tooltip and overlay: background
 * geometry, text blocks and resource icons.
 *
 * <p>
 * Item, fluid and essentia icons all render through the same icon path so a
 * resource looks identical whether it appears in a tooltip grid, a drawer face
 * or a recipe overlay.
 *
 * <p>
 * Attribute and matrix state is pushed once per drawing operation and popped in
 * a finally block, mirroring OKBackpack's content renderer. Individual icons and
 * amount labels therefore never restore state themselves, which is what keeps
 * lighting, depth mask and colour correct for the caller.
 */
@SideOnly(Side.CLIENT)
public class StorageTooltipPainter extends Gui {

    /** Icon edge length in pixels. */
    public static final int ICON_SIZE = 16;
    /** Horizontal pitch between two icons. */
    public static final int CELL_WIDTH = 18;
    /** Vertical pitch between two icon rows. */
    public static final int ROW_HEIGHT = 20;
    /** Extra horizontal room reserved so amounts never crowd a neighbour. */
    public static final int COUNT_PADDING = 2;
    /** Baseline inset mirroring vanilla's item-count placement. */
    public static final int COUNT_BASELINE = 9;
    /** Accent colour for section headings. */
    public static final int TITLE_COLOUR = 0xFFAA00;
    /** Default tooltip text colour. */
    public static final int TEXT_COLOUR = 0xFFFFFF;

    /** Maximum tooltip content width in pixels. */
    public static final int MAX_TOOLTIP_WIDTH = 300;
    /** Minimum tooltip content width in pixels. */
    public static final int MIN_TOOLTIP_WIDTH = 80;
    /** Gap kept between a tooltip and the screen edge. */
    public static final int SCREEN_MARGIN = 6;
    /** Z level icons draw at, matching vanilla's GUI item rendering. */
    public static final float ITEM_Z_LEVEL = 200F;
    /**
     * Z level of this painter's own quads while drawing inside a tooltip.
     * {@link Gui#zLevel} is per instance and starts at zero, so a painter used
     * from a tooltip would otherwise draw its fluid and essentia icons in front
     * of, or behind, the rest of the tooltip instead of with it.
     */
    public static final float TOOLTIP_Z_LEVEL = 300F;
    /** Shared renderer, since vanilla keeps one instance per GUI pass. */
    public static final RenderItem ITEM_RENDERER = new RenderItem();

    /** Shared instance, since the {@link Gui} drawing helpers need one. */
    public static final StorageTooltipPainter INSTANCE = new StorageTooltipPainter();

    /**
     * Draws the vanilla tooltip background and border around a box.
     *
     * @param x             left edge of the content box
     * @param y             top edge of the content box
     * @param width         content width
     * @param height        content height
     * @param backgroundTop top background colour
     * @param backgroundEnd bottom background colour
     * @param borderStart   top border colour
     * @param borderEnd     bottom border colour
     */
    public void drawBackground(int x, int y, int width, int height, int backgroundTop, int backgroundEnd,
        int borderStart, int borderEnd) {
        drawGradientRect(x - 3, y - 4, x + width + 3, y - 3, backgroundTop, backgroundTop);
        drawGradientRect(x - 3, y + height + 3, x + width + 3, y + height + 4, backgroundEnd, backgroundEnd);
        drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, backgroundTop, backgroundEnd);
        drawGradientRect(x - 4, y - 3, x - 3, y + height + 3, backgroundTop, backgroundEnd);
        drawGradientRect(x + width + 3, y - 3, x + width + 4, y + height + 3, backgroundTop, backgroundEnd);
        drawGradientRect(x - 3, y - 3, x + width + 3, y - 2, borderStart, borderStart);
        drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, borderEnd, borderEnd);
        drawGradientRect(x - 3, y - 2, x - 2, y + height + 2, borderStart, borderEnd);
        drawGradientRect(x + width + 2, y - 2, x + width + 3, y + height + 2, borderStart, borderEnd);
    }

    /**
     * Draws one resource icon with its compacted amount.
     *
     * @param entry resource to draw
     * @param x     left edge
     * @param y     top edge
     */
    public void drawEntry(Entry entry, int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float previousItemZ = ITEM_RENDERER.zLevel;
        float previousGuiZ = zLevel;
        try {
            beginIcons();
            drawIcon(entry, x, y);
            drawCount(Minecraft.getMinecraft().fontRenderer, entry.amount(), x, y);
        } finally {
            endIcons(previousItemZ, previousGuiZ);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Renders an icon grid with a heading-free cell layout.
     *
     * <p>
     * State is pushed once for the whole grid and popped in a finally block, so
     * every icon and amount label draws inside one protected scope.
     *
     * @param font    font to draw with
     * @param entries resources to draw
     * @param x       left edge
     * @param y       top edge
     * @param columns resolved column count
     */
    public void drawGrid(FontRenderer font, List<Entry> entries, int x, int y, int columns) {
        if (entries.isEmpty()) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float previousItemZ = ITEM_RENDERER.zLevel;
        float previousGuiZ = zLevel;
        try {
            beginIcons();
            int drawX = x;
            for (int index = 0; index < entries.size(); index++) {
                if (index > 0 && index % columns == 0) {
                    drawX = x;
                }
                Entry entry = entries.get(index);
                int cellWidth = cellWidth(font, entry.amount());
                int renderX = drawX + cellWidth - CELL_WIDTH;
                int renderY = y + index / columns * ROW_HEIGHT;
                drawIcon(entry, renderX, renderY);
                drawCount(font, entry.amount(), renderX, renderY);
                drawX += cellWidth;
            }
        } finally {
            endIcons(previousItemZ, previousGuiZ);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Draws a section heading in the drawer accent colour.
     *
     * <p>
     * State is pushed and popped so a heading drawn between two icon grids never
     * inherits their lighting or depth settings.
     *
     * @param font  font to draw with
     * @param title translation key of the heading
     * @param x     left edge
     * @param y     top edge
     */
    public static void drawTitle(FontRenderer font, String title, int x, int y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            font.drawStringWithShadow(StatCollector.translateToLocal(title), x, y, TITLE_COLOUR);
        } finally {
            GL11.glPopAttrib();
        }
    }

    /**
     * Wraps lines to a maximum width, preserving order.
     *
     * @param font     font used for measurement
     * @param lines    source lines
     * @param maxWidth maximum line width
     * @return wrapped lines
     */
    public static List<String> wrap(FontRenderer font, List<String> lines, int maxWidth) {
        List<String> wrapped = new ArrayList<>(lines.size());
        for (String line : lines) {
            wrapped.addAll(font.listFormattedStringToWidth(line, maxWidth));
        }
        return wrapped;
    }

    /**
     * @param font  font used for measurement
     * @param lines wrapped lines
     * @return the widest line in pixels
     */
    public static int textWidth(FontRenderer font, List<String> lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.getStringWidth(line));
        }
        return width;
    }

    /**
     * @param guiWidth screen width
     * @return the widest content box that still leaves a screen margin
     */
    public static int maxContentWidth(int guiWidth) {
        return Math.max(MIN_TOOLTIP_WIDTH, Math.min(MAX_TOOLTIP_WIDTH, guiWidth - SCREEN_MARGIN * 2 - 8));
    }

    /**
     * @param amount text to measure
     * @param font   font to measure with
     * @return the cell width needed to show this amount without overlap
     */
    public static int cellWidth(FontRenderer font, String amount) {
        return amount.isEmpty() ? CELL_WIDTH : Math.max(CELL_WIDTH, font.getStringWidth(amount) + COUNT_PADDING);
    }

    /**
     * Enables the state required to draw GUI icons. The caller must have pushed
     * the attribute stack so this is undone on exit.
     */
    private static void beginIcons() {
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        ITEM_RENDERER.zLevel = ITEM_Z_LEVEL;
        INSTANCE.zLevel = TOOLTIP_Z_LEVEL;
    }

    /**
     * Restores the shared renderer and this painter to the levels the caller had.
     *
     * @param previousItemZ item renderer z level to restore
     * @param previousGuiZ  painter z level to restore
     */
    private static void endIcons(float previousItemZ, float previousGuiZ) {
        ITEM_RENDERER.zLevel = previousItemZ;
        INSTANCE.zLevel = previousGuiZ;
    }

    /**
     * Renders one resource icon: an item model, a tinted fluid icon, or a tinted
     * essentia icon.
     *
     * <p>
     * Leaves the GL state as it found it, so consecutive icons stay consistent.
     *
     * @param entry resource to draw
     * @param x     left edge
     * @param y     top edge
     */
    private void drawIcon(Entry entry, int x, int y) {
        if (entry.item() == null && entry.fluid() == null && entry.aspect() == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            if (entry.item() != null) {
                ITEM_RENDERER.renderItemAndEffectIntoGUI(
                    minecraft.fontRenderer,
                    minecraft.getTextureManager(),
                    entry.item(),
                    x,
                    y);
            } else if (entry.fluid() != null) {
                drawFluidIcon(entry.fluid(), x, y);
            } else {
                drawAspectIcon(entry.aspect(), x, y);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void drawFluidIcon(FluidStack fluid, int x, int y) {
        IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        minecraft.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        tint(
            fluid.getFluid()
                .getColor(fluid));
        drawTexturedModelRectFromIcon(x, y, icon, ICON_SIZE, ICON_SIZE);
    }

    private void drawAspectIcon(AspectIcon aspect, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        minecraft.getTextureManager()
            .bindTexture(aspect.texture());
        tint(aspect.color());
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + ICON_SIZE, zLevel, 0D, 1D);
        tessellator.addVertexWithUV(x + ICON_SIZE, y + ICON_SIZE, zLevel, 1D, 1D);
        tessellator.addVertexWithUV(x + ICON_SIZE, y, zLevel, 1D, 0D);
        tessellator.addVertexWithUV(x, y, zLevel, 0D, 0D);
        tessellator.draw();
    }

    /**
     * Draws a compacted amount in the bottom-right corner of a cell.
     *
     * <p>
     * Vanilla's item-count overlay reads the count from an {@code ItemStack},
     * which fluids and essentia do not have, so the text is drawn directly using
     * vanilla's own placement. State is pushed and popped so the caller's
     * lighting and depth settings survive.
     *
     * @param font   font to draw with
     * @param amount compacted amount text
     * @param x      cell left edge
     * @param y      cell top edge
     */
    private static void drawCount(FontRenderer font, String amount, int x, int y) {
        if (amount.isEmpty()) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            font.drawStringWithShadow(
                amount,
                x + ICON_SIZE + 3 - 2 - font.getStringWidth(amount),
                y + COUNT_BASELINE,
                TEXT_COLOUR);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void tint(int color) {
        GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
    }
}
