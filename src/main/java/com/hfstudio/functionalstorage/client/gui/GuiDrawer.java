package com.hfstudio.functionalstorage.client.gui;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Drawer screen. Shows the drawer's storage slots, its upgrade slots, and the
 * player inventory. The background is generated at runtime so the layout always
 * matches the container regardless of how many slots a drawer exposes.
 */
public class GuiDrawer extends GuiContainer {

    private static final int WINDOW_WIDTH = 176;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int SLOT_BORDER = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int LABEL_COLOUR = 0x404040;

    private final ControllableDrawerTile tile;

    public GuiDrawer(@Nonnull EntityPlayer player, @Nonnull ControllableDrawerTile tile) {
        super(new ContainerDrawer(tile, player));
        this.tile = tile;
        this.xSize = WINDOW_WIDTH;
        this.ySize = 114 + ((ContainerDrawer) inventorySlots).getVisibleStorageSlots() * 18;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL_FILL);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, PANEL_HIGHLIGHT);
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, PANEL_HIGHLIGHT);
        drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, PANEL_SHADOW);
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, PANEL_SHADOW);

        for (Object slotObject : inventorySlots.inventorySlots) {
            if (!(slotObject instanceof Slot slot)) {
                continue;
            }
            int left = guiLeft + slot.xDisplayPosition;
            int top = guiTop + slot.yDisplayPosition;
            drawRect(left - 1, top - 1, left + 17, top + 17, SLOT_BORDER);
            drawRect(left, top, left + 16, top + 16, SLOT_FILL);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(
            StatCollector.translateToLocal(
                tile.getBlockType()
                    .getUnlocalizedName() + ".name"),
            8,
            6,
            LABEL_COLOUR);
        fontRendererObj.drawString(
            StatCollector.translateToLocal("container.functionalstorage.upgrades"),
            8,
            20 + (((ContainerDrawer) inventorySlots).getVisibleStorageSlots() + 8) / 9 * 18,
            LABEL_COLOUR);
        fontRendererObj
            .drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, LABEL_COLOUR);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Region used if a texture replaces the generated background later.
     *
     * @return the drawer GUI texture location
     */
    @Nonnull
    public static ResourceLocation backgroundTexture() {
        return new ResourceLocation("functionalstorage", "textures/gui/background.png");
    }
}
