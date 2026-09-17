package com.hfstudio.functionalstorage.common.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class UpgradeConversionRecipe extends ShapelessOreRecipe {

    public UpgradeConversionRecipe(ItemStack output, ItemStack input) {
        super(output, input);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = super.getCraftingResult(inventory);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.hasTagCompound()) result.setTagCompound(
                (NBTTagCompound) stack.getTagCompound()
                    .copy());
        }
        return result;
    }
}
