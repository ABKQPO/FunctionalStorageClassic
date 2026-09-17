package com.hfstudio.functionalstorage.misc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.common.block.WoodDrawerBlock;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.recipe.DrawerCraftingRecipe;
import com.hfstudio.functionalstorage.common.recipe.FramedDrawerStyleRecipe;
import com.hfstudio.functionalstorage.common.storage.DrawerWoodType;

import cpw.mods.fml.common.registry.GameRegistry;

public class FunctionalStorageRecipes {

    private FunctionalStorageRecipes() {}

    public static void registerEarlyRecipes() {
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers)
            OreDictionary.registerOre("drawerFunctionalStorage", new ItemStack(block));
        for (var block : RegistrationHandler.framedDrawers)
            OreDictionary.registerOre("drawerFunctionalStorage", new ItemStack(block));
        ModernStorageRecipes.register();
        GameRegistry.addRecipe(new FramedDrawerStyleRecipe());
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                new ItemStack(RegistrationHandler.netheriteUpgrade),
                RegistrationHandler.diamondUpgrade,
                Items.nether_star));
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(RegistrationHandler.oreDictionaryUpgrade),
                "W W",
                " C ",
                "W W",
                'W',
                Items.writable_book,
                'C',
                Blocks.chest));
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                new ItemStack(RegistrationHandler.wirelessPullingUpgrade),
                RegistrationHandler.pullingUpgrade,
                Items.ender_pearl));
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                new ItemStack(RegistrationHandler.wirelessPushingUpgrade),
                RegistrationHandler.pushingUpgrade,
                Items.ender_pearl));
        for (var block : RegistrationHandler.essentiaDrawers) {
            int count = block.getFaceLayout()
                .getSlotCount();
            String[] pattern = count == 1 ? new String[] { "PPP", "PCP", "PPP" }
                : count == 2 ? new String[] { "PCP", "PPP", "PCP" } : new String[] { "CPC", "PPP", "CPC" };
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(
                    new ItemStack(block, count),
                    false,
                    pattern,
                    'P',
                    "plankWood",
                    'C',
                    Items.glass_bottle));
        }
        registerGenerationTier(RegistrationHandler.waterGenerationUpgrades, new ItemStack(Items.water_bucket));
        registerGenerationTier(RegistrationHandler.stoneGenerationUpgrades, new ItemStack(Blocks.cobblestone));
        registerGenerationTier(RegistrationHandler.universalGenerationUpgrades, new ItemStack(Items.nether_star));
    }

    public static void registerLateRecipes() {
        if (OreDictionary.getOres("nuggetIron")
            .isEmpty()) {
            List<ItemStack> missingNuggets = OreDictionary.getOres("nuggetIron");
            for (Object recipe : CraftingManager.getInstance()
                .getRecipeList()) {
                if (!(recipe instanceof DrawerCraftingRecipe shaped)) continue;
                Object[] input = shaped.getInput();
                for (int slot = 0; slot < input.length; slot++) {
                    if (input[slot] == missingNuggets) input[slot] = new ItemStack(Items.iron_ingot);
                }
            }
        }
    }

    private static void registerGenerationTier(List<GenerationUpgradeItem> upgrades, ItemStack resource) {
        ItemStack core = new ItemStack(Blocks.chest);
        for (GenerationUpgradeItem upgrade : upgrades) {
            GameRegistry
                .addRecipe(new ShapedOreRecipe(new ItemStack(upgrade), "RRR", "RCR", "RRR", 'R', resource, 'C', core));
            core = new ItemStack(upgrade);
        }
    }

    public static List<Block> blocksOf(@Nonnull DrawerWoodType woodType) {
        List<Block> blocks = new ArrayList<>();
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers)
            if (block.getWoodType() == woodType) blocks.add(block);
        return blocks;
    }
}
