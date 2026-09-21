package com.hfstudio.functionalstorage.client.integration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.client.gui.DrawerContentCollector;
import com.hfstudio.functionalstorage.client.gui.GuiDrawer;
import com.hfstudio.functionalstorage.client.gui.StorageContentEntry;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.IUsageHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Optional.Interface(iface = "codechicken.nei.api.IOverlayHandler", modid = "NotEnoughItems")
public class StorageOverlayHandler implements IOverlayHandler {

    public static void register() {
        StorageOverlayHandler handler = new StorageOverlayHandler();
        for (String ident : knownOverlayIdents()) {
            API.registerGuiOverlayHandler(GuiDrawer.class, handler, ident);
        }
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static Set<String> knownOverlayIdents() {
        Set<String> idents = new LinkedHashSet<>();
        for (ICraftingHandler handler : GuiCraftingRecipe.craftinghandlers) {
            collect(handler, idents);
        }
        for (IUsageHandler handler : GuiUsageRecipe.usagehandlers) {
            collect(handler, idents);
        }
        return idents;
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static void collect(IRecipeHandler handler, Set<String> idents) {
        if (handler == null) {
            return;
        }
        String ident = handler.getOverlayIdentifier();
        if (ident != null && !ident.isEmpty()) {
            idents.add(ident);
        }
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean maxTransfer) {
        // Drawers expose stored contents but no crafting grid, so nothing is moved.
    }

    @Override
    @Optional.Method(modid = "NotEnoughItems")
    public List<ItemOverlayState> presenceOverlay(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex) {
        List<ItemOverlayState> states = new ArrayList<>();
        List<StorageContentEntry> owned = ownedContents(firstGui);
        for (PositionedStack ingredient : recipe.getIngredientStacks(recipeIndex)) {
            states.add(new ItemOverlayState(ingredient, contains(owned, ingredient)));
        }
        return states;
    }

    private static List<StorageContentEntry> ownedContents(GuiContainer gui) {
        if (!(gui instanceof GuiDrawer drawer)) {
            return List.of();
        }
        ControllableDrawerTile tile = drawer.getTile();
        if (tile.getWorldObj() == null) {
            return List.of();
        }
        EntityPlayer player = gui.mc == null ? null : gui.mc.thePlayer;
        return player == null ? DrawerContentCollector.collectStorageOnly(tile)
            : DrawerContentCollector.collectDrawer(tile, player);
    }

    @Optional.Method(modid = "NotEnoughItems")
    private static boolean contains(List<StorageContentEntry> owned, PositionedStack ingredient) {
        for (StorageContentEntry entry : owned) {
            ItemStack item = entry.item();
            if (item != null && ingredient.contains(item)) {
                return true;
            }
        }
        return false;
    }
}
