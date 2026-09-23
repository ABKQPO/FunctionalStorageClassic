package com.hfstudio.functionalstorage.client.integration;

import java.util.function.Predicate;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.gui.GuiDrawer;
import com.hfstudio.functionalstorage.client.gui.GuiUpgrade;
import com.hfstudio.functionalstorage.common.network.GhostFilterMessage;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PanelWidget;
import codechicken.nei.SearchField;
import codechicken.nei.api.API;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.guihook.GuiContainerManager;
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

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int x, int y, ItemStack stack, int button) {
        // NEI also calls this hook with real cursor stacks; those keep their normal click behavior.
        return isDragging() && markFilter(gui, x, y, stack, button);
    }

    @Optional.Method(modid = "NotEnoughItems")
    public static boolean isDragging() {
        return NEIClientConfig.isEnabled() && !NEIClientConfig.isHidden()
            && (ItemPanels.itemPanel.draggedStack != null || ItemPanels.bookmarkPanel.draggedStack != null);
    }

    /** Handles custom controls that do not enter NEI's vanilla container hooks. */
    @Optional.Method(modid = "NotEnoughItems")
    public static boolean dropDraggedStack(GuiContainer gui, int x, int y, int button) {
        if (!isDragging()) return false;
        PanelWidget panel = ItemPanels.itemPanel.draggedStack != null ? ItemPanels.itemPanel : ItemPanels.bookmarkPanel;
        if (!markFilter(gui, x, y, panel.draggedStack, button)) return false;
        panel.draggedStack = null;
        return true;
    }

    @Optional.Method(modid = "NotEnoughItems")
    public static void releaseMouse(GuiContainer gui, int x, int y, int button) {
        GuiContainerManager manager = GuiContainerManager.getManager(gui);
        if (manager != null && !manager.overrideMouseUp(x, y, button)) manager.mouseUp(x, y, button);
    }

    private static boolean markFilter(GuiContainer gui, int x, int y, ItemStack stack, int button) {
        if (stack == null || stack.getItem() == null || (button != 0 && button != 1)) return false;
        int slot = gui instanceof GuiUpgrade upgrade ? upgrade.getFilterSlotAt(x, y)
            : gui instanceof GuiDrawer drawer ? drawer.getFilterSlotAt(x, y) : -1;
        if (slot < 0) return false;
        FunctionalStorage.network.sendToServer(new GhostFilterMessage(gui.inventorySlots.windowId, slot, stack));
        stack.stackSize = 0;
        return true;
    }
}
