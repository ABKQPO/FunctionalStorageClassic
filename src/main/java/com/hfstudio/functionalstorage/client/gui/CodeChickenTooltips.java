package com.hfstudio.functionalstorage.client.gui;

import static codechicken.lib.gui.GuiDraw.TOOLTIP_HANDLER;
import static codechicken.lib.gui.GuiDraw.getTipLineId;

import java.awt.Dimension;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Section;
import com.hfstudio.functionalstorage.common.integration.Mods;

import codechicken.lib.gui.GuiDraw;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CodeChickenTooltips {

    public static boolean isAvailable() {
        return Mods.CodeChickenCore.isModLoaded();
    }

    public static void appendPlaceholderIfAbsent(ItemStack stack, List<String> tooltip) {
        if (!isAvailable() || containsPlaceholder(tooltip)) {
            return;
        }
        appendPlaceholder(stack, tooltip);
    }

    public static void appendPlaceholder(ItemStack stack, List<String> tooltip) {
        if (!isAvailable()) {
            return;
        }
        List<Section> sections = sectionsFor(stack);
        if (!sections.isEmpty()) {
            tooltip.add(placeholder(sections));
        }
    }

    @Optional.Method(modid = "CodeChickenCore")
    public static List<Section> sectionsFor(ItemStack stack) {
        return stack == null ? List.of() : DrawerTooltipData.read(stack);
    }

    @Optional.Method(modid = "CodeChickenCore")
    public static String placeholder(@Nonnull List<Section> sections) {
        return TOOLTIP_HANDLER + getTipLineId(new PreviewLineHandler(sections));
    }

    @Optional.Method(modid = "CodeChickenCore")
    public static boolean containsPlaceholder(List<String> tooltip) {
        for (String line : tooltip) {
            if (line != null && line.startsWith(TOOLTIP_HANDLER)) {
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Optional.Interface(iface = "codechicken.lib.gui.GuiDraw$ITooltipLineHandler", modid = "CodeChickenCore")
    public static class PreviewLineHandler implements GuiDraw.ITooltipLineHandler {

        /** Vertical space a section heading occupies. */
        public static final int TITLE_HEIGHT = 11;
        /** Icons per row, matching the drawer face width. */
        public static final int COLUMNS = 9;

        public final List<Section> sections;

        public PreviewLineHandler(List<Section> sections) {
            this.sections = List.copyOf(sections);
        }

        @Optional.Method(modid = "CodeChickenCore")
        @Override
        public Dimension getSize() {
            Minecraft minecraft = Minecraft.getMinecraft();
            int width = 0;
            int height = 0;
            for (Section section : sections) {
                height += TITLE_HEIGHT;
                width = Math
                    .max(width, minecraft.fontRenderer.getStringWidth(StatCollector.translateToLocal(section.title())));
                Dimension grid = gridSize(section.entries());
                width = Math.max(width, grid.width);
                height += grid.height;
            }
            return new Dimension(width, height);
        }

        @Optional.Method(modid = "CodeChickenCore")
        @Override
        public void draw(int x, int y) {
            Minecraft minecraft = Minecraft.getMinecraft();
            int cursorY = y;
            for (Section section : sections) {
                StorageTooltipPainter.drawTitle(minecraft.fontRenderer, section.title(), x, cursorY);
                cursorY += TITLE_HEIGHT;
                StorageTooltipPainter.INSTANCE.drawGrid(minecraft.fontRenderer, section.entries(), x, cursorY, COLUMNS);
                cursorY += gridSize(section.entries()).height;
            }
        }

        public static Dimension gridSize(List<Entry> entries) {
            if (entries.isEmpty()) {
                return new Dimension(0, 0);
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            int width = 0;
            for (int index = 0; index < Math.min(entries.size(), COLUMNS); index++) {
                width += StorageTooltipPainter.cellWidth(
                    minecraft.fontRenderer,
                    entries.get(index)
                        .amount());
            }
            int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
            return new Dimension(width, rows * StorageTooltipPainter.ROW_HEIGHT);
        }
    }
}
