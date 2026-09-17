package com.hfstudio.functionalstorage.common.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

/** Protects filled drawers from consumption and preserves state in frame conversions. */
public class DrawerCraftingRecipe extends ShapedOreRecipe {

    private final boolean preserveDrawer;

    public DrawerCraftingRecipe(ItemStack output, boolean preserveDrawer, Object... recipe) {
        super(output, recipe);
        this.preserveDrawer = preserveDrawer;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) return false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isDrawer(stack) && stack.hasTagCompound()
                && !stack.getTagCompound()
                    .hasNoTags()
                && !preserveDrawer) return false;
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = super.getCraftingResult(inventory);
        if (preserveDrawer) for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack source = inventory.getStackInSlot(slot);
            if (isDrawer(source) && source.hasTagCompound()) {
                result.setTagCompound(
                    (NBTTagCompound) source.getTagCompound()
                        .copy());
                break;
            }
        }
        return result;
    }

    private boolean isDrawer(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemBlock item && item.field_150939_a instanceof DrawerBlock;
    }
}
