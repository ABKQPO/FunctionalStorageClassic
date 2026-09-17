package com.hfstudio.functionalstorage.client.integration;

import java.util.function.Predicate;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.client.gui.GuiUpgrade;

import codechicken.nei.SearchField;
import codechicken.nei.api.API;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.ItemFilter;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = "NotEnoughItems")
public class NEIGuiIntegration implements INEIGuiHandler {

    @Optional.Method(modid = "NotEnoughItems")
    public static void register() {
        API.registerNEIGuiHandler(new NEIGuiIntegration());
    }

    @Optional.Method(modid = "NotEnoughItems")
    public static Predicate<ItemStack> search(String query) {
        ItemFilter filter = SearchField.searchParser.getFilter(query, true);
        return filter::matches;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int width, int height) {
        return gui instanceof GuiUpgrade upgrade && upgrade.overlapsFilterPanel(x, y, width, height);
    }
}
