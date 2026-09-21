package com.hfstudio.functionalstorage.client.integration;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.client.gui.GuiDrawer;

import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerTooltipHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "codechicken.nei.guihook.IContainerTooltipHandler", modid = "NotEnoughItems")
public class NEIStorageTooltips implements IContainerTooltipHandler {

    @Optional.Method(modid = "NotEnoughItems")
    public static void register() {
        GuiContainerManager.addTooltipHandler(new NEIStorageTooltips());
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
        List<String> currenttip) {
        if (gui instanceof GuiDrawer drawer) {
            currenttip.addAll(drawer.storageTooltipLines(mousex, mousey));
        }
        return currenttip;
    }
}
