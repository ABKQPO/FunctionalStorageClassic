package com.hfstudio.functionalstorage.common.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class UpgradeConversionRecipe extends ShapedOreRecipe {

    private final ItemStack source;

    public UpgradeConversionRecipe(ItemStack output, ItemStack input, Object... recipe) {
        super(output, recipe);
        source = input.copy();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = super.getCraftingResult(inventory);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.isItemEqual(source)) {
                if (stack.hasTagCompound()) result.setTagCompound(
                    (NBTTagCompound) stack.getTagCompound()
                        .copy());
                break;
            }
        }
        return result;
    }
}
