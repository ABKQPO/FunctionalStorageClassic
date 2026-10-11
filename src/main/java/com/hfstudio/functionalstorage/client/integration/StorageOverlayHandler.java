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
import com.hfstudio.functionalstorage.client.gui.GuiAdaptiveCrafting;
import com.hfstudio.functionalstorage.client.gui.StorageContentEntry;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.container.ContainerAdaptiveCrafting;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.network.GhostFilterMessage;

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
            API.registerGuiOverlayHandler(GuiAdaptiveCrafting.class, handler, ident);
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
        if (!(firstGui.inventorySlots instanceof ContainerAdaptiveCrafting menu)
            || !"crafting".equals(recipe.getOverlayIdentifier())) return;
        ItemStack[] templates = new ItemStack[9];
        for (PositionedStack ingredient : recipe.getIngredientStacks(recipeIndex)) {
            int column = (ingredient.relx - 25) / 18;
            int row = (ingredient.rely - 6) / 18;
            if (column < 0 || column >= 3 || row < 0 || row >= 3) return;
            ItemStack selected = null;
            for (ItemStack candidate : ingredient.items) {
                if (candidate != null && available(menu, firstGui.mc.thePlayer, candidate)) {
                    selected = candidate.copy();
                    selected.stackSize = 1;
                    break;
                }
            }
            if (selected == null) return;
            templates[row * 3 + column] = selected;
        }
        FunctionalStorage.network.sendToServer(new GhostFilterMessage(menu.windowId, templates, maxTransfer));
    }

    private static boolean available(ContainerAdaptiveCrafting menu, EntityPlayer player, ItemStack candidate) {
        for (int index = 0; index < menu.getItemResourceCount(); index++) {
            if (menu.getResourceItemSnapshot(index).getAmount() > 0L
                && menu.getResourceItemSnapshot(index).isSameType(candidate)) return true;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.isItemEqual(candidate) && ItemStack.areItemStackTagsEqual(stack, candidate)) return true;
        }
        for (int index = 0; index < 9; index++) {
            ItemStack stack = menu.getCraftingInventory().getStackInSlot(index);
            if (stack != null && stack.isItemEqual(candidate) && ItemStack.areItemStackTagsEqual(stack, candidate)) return true;
        }
        return false;
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
