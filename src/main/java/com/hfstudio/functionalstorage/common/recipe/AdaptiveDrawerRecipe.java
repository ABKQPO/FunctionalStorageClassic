package com.hfstudio.functionalstorage.common.recipe;

import javax.annotation.Nonnull;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;

public class AdaptiveDrawerRecipe extends ShapelessOreRecipe {

    private static final String TILE_DATA = "TileData";
    private static final String DRAWER_LAYOUT = "DrawerLayout";

    public AdaptiveDrawerRecipe(@Nonnull ItemStack output, @Nonnull ItemStack sourceDrawer,
        @Nonnull ItemStack adaptiveUpgrade) {
        super(withSourceLayout(output, sourceDrawer), sourceDrawer, adaptiveUpgrade);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) return false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isDrawer(stack) && stack.hasTagCompound()
                && !stack.getTagCompound()
                    .hasNoTags())
                return false;
        }
        return true;
    }

    private static ItemStack withSourceLayout(ItemStack output, ItemStack sourceDrawer) {
        if (!(sourceDrawer.getItem() instanceof ItemBlock itemBlock)
            || !(itemBlock.field_150939_a instanceof DrawerBlock drawerBlock)) {
            throw new IllegalArgumentException("Adaptive drawer recipes require a Functional Storage drawer input");
        }
        DrawerLayout layout = drawerBlock.getDrawerLayout();
        if (layout == null) throw new IllegalArgumentException("Drawer input has no registered layout");

        NBTTagCompound tileData = new NBTTagCompound();
        tileData.setString(DRAWER_LAYOUT, layout.getId());
        NBTTagCompound itemData = new NBTTagCompound();
        itemData.setTag(TILE_DATA, tileData);
        output.setTagCompound(itemData);
        return output;
    }

    private boolean isDrawer(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemBlock itemBlock
            && itemBlock.field_150939_a instanceof DrawerBlock;
    }
}
