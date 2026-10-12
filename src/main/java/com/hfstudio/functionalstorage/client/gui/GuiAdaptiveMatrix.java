package com.hfstudio.functionalstorage.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

public class GuiAdaptiveMatrix extends GuiController {

    public GuiAdaptiveMatrix(EntityPlayer player, AdaptiveMatrixTile tile) {
        super(player, tile);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        AdaptiveMatrixTile matrix = (AdaptiveMatrixTile) tile;
        fontRendererObj.drawString(
            StatCollector.translateToLocal(
                matrix.isPowered() ? "functionalstorage.gui.adaptive.online"
                    : "functionalstorage.gui.adaptive.offline"),
            10,
            25,
            matrix.isPowered() ? 0x327A38 : 0xAA3333);
        if (AdaptiveMatrixTile.usesRf()) {
            fontRendererObj.drawString(
                StorageAmountFormatter.formatDisplay(matrix.getEnergyStored(), false) + " RF",
                10,
                37,
                LABEL_COLOUR);
        }
    }
}
