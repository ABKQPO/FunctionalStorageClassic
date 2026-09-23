package com.hfstudio.functionalstorage.common.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.FramedBlock;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;

public class FramedDrawerStyleRecipe implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return findPattern(inventory) >= 0;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        int anchor = findPattern(inventory);
        if (anchor < 0) return null;
        int width = inventory.getSizeInventory() == 4 ? 2 : 3;
        ItemStack result = inventory.getStackInSlot(anchor + width)
            .copy();
        result.stackSize = 1;
        new FramedDrawerStyle(
            inventory.getStackInSlot(anchor),
            inventory.getStackInSlot(anchor + 1),
            inventory.getStackInSlot(anchor + width + 1)).applyDrawerStyle(result);
        return result;
    }

    private static int findPattern(InventoryCrafting inventory) {
        int width = inventory.getSizeInventory() == 4 ? 2 : inventory.getSizeInventory() == 9 ? 3 : 0;
        for (int top = 0; top < width - 1; top++) {
            for (int left = 0; left < width - 1; left++) {
                ItemStack exterior = inventory.getStackInRowAndColumn(left, top);
                ItemStack front = inventory.getStackInRowAndColumn(left + 1, top);
                ItemStack drawer = inventory.getStackInRowAndColumn(left, top + 1);
                ItemStack divider = inventory.getStackInRowAndColumn(left + 1, top + 1);
                if (!isMaterial(exterior) || !isMaterial(front)
                    || !isFramedDrawer(drawer)
                    || divider != null && !isMaterial(divider)) {
                    continue;
                }
                boolean extra = false;
                for (int row = 0; row < width; row++) {
                    for (int column = 0; column < width; column++) {
                        if ((column < left || column > left + 1 || row < top || row > top + 1)
                            && inventory.getStackInRowAndColumn(column, row) != null) {
                            extra = true;
                        }
                    }
                }
                if (!extra) {
                    return left + top * width;
                }
            }
        }
        return -1;
    }

    @Override
    public int getRecipeSize() {
        return 4;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }

    private static boolean isMaterial(ItemStack stack) {
        return FramedDrawerStyle.materialBlock(stack) != null;
    }

    private static boolean isFramedDrawer(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemBlock item && item.field_150939_a instanceof FramedBlock;
    }
}
