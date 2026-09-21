package com.hfstudio.functionalstorage.common.recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

/**
 * Cleansing recipe. A drawer that already holds stored resources is placed in a
 * crafting grid on its own and comes back with its storage emptied, so a player
 * can recover a mis-filled drawer without spending anything.
 *
 * <p>
 * Everything that is not stored content survives: the upgrade slots, the utility
 * upgrade slots, the use-options, the framing style and the controller link are
 * all carried over untouched, so the upgrades invested in the drawer are kept.
 *
 * <p>
 * Extends {@code ShapelessOreRecipe} rather than implementing {@code IRecipe}
 * directly because recipe lookup mods only enumerate shapeless recipes that are
 * instances of the vanilla classes; a bare {@code IRecipe} would still work in a
 * crafting table but would never appear in the recipe list. The drawer is
 * declared through an ore dictionary entry so the recipe is reachable from every
 * drawer kind, and the representative drawer is shown as the result because the
 * real one depends on what the player places in the grid.
 */
public class DrawerCleansingRecipe extends ShapelessOreRecipe {

    private final ItemStack displayOutput;

    /**
     * @param drawerOreName ore dictionary name matching every drawer
     * @param displayOutput drawer shown as this recipe's result in recipe lists
     */
    public DrawerCleansingRecipe(@Nonnull String drawerOreName, @Nonnull ItemStack displayOutput) {
        // The superclass copies the output eagerly, so a real stack is required
        // here even though getCraftingResult produces the actual result.
        super(displayOutput, drawerOreName);
        this.displayOutput = displayOutput.copy();
    }

    @Override
    public boolean matches(@Nonnull InventoryCrafting inventory, @Nullable World world) {
        return findTarget(inventory) != null;
    }

    @Override
    @Nullable
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inventory) {
        ItemStack drawer = findTarget(inventory);
        return drawer == null ? null : DrawerBlock.cleanseContents(drawer);
    }

    /**
     * Reports the drawer shown as this recipe's result. The real result depends
     * on the drawer placed in the grid, which cannot be known in advance.
     *
     * @return the representative drawer
     */
    @Override
    public ItemStack getRecipeOutput() {
        return displayOutput.copy();
    }

    /**
     * Finds the single filled drawer in the grid. An empty grid, more than one
     * drawer, any other ingredient or an already empty drawer all fail to match,
     * so the recipe only fires on a drawer that actually has something to cleanse.
     *
     * @param inventory crafting grid
     * @return the filled drawer, or null when the grid does not match
     */
    @Nullable
    private static ItemStack findTarget(@Nonnull InventoryCrafting inventory) {
        ItemStack drawer = null;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            if (drawer != null || !isFilledDrawer(stack)) {
                return null;
            }
            drawer = stack;
        }
        return drawer;
    }

    /**
     * Reports whether a stack is a drawer that actually stores resources. An
     * already empty drawer has nothing to cleanse, so it must not match.
     *
     * @param stack candidate stack
     * @return whether the stack is a drawer with stored content
     */
    private static boolean isFilledDrawer(@Nonnull ItemStack stack) {
        return stack.getItem() instanceof ItemBlock item && item.field_150939_a instanceof DrawerBlock
            && DrawerBlock.hasStoredResource(stack);
    }

    @Override
    public int getRecipeSize() {
        return 1;
    }
}
