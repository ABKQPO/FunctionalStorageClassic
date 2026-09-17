package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.hfstudio.functionalstorage.client.gui.GuiUpgrade;

import codechicken.nei.api.API;
import codechicken.nei.api.INEIGuiAdapter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NEIGuiIntegration extends INEIGuiAdapter {

    public static void register() {
        API.registerNEIGuiHandler(new NEIGuiIntegration());
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int width, int height) {
        return gui instanceof GuiUpgrade upgrade && upgrade.overlapsFilterPanel(x, y, width, height);
    }
}
