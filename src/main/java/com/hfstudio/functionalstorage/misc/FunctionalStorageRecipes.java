package com.hfstudio.functionalstorage.misc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.hfstudio.functionalstorage.common.block.FluidDrawerBlock;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.block.WoodDrawerBlock;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.recipe.FramedDrawerStyleRecipe;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.DrawerWoodType;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.registry.GameRegistry;

/** Registers vanilla crafting recipes and compacting rules. */
public class FunctionalStorageRecipes {

    private FunctionalStorageRecipes() {}

    public static void registerEarlyRecipes() {
        registerDrawerRecipes();
        registerFramedRecipes(new ItemStack(Blocks.planks));
        GameRegistry.addRecipe(new FramedDrawerStyleRecipe());
        registerUpgradeRecipes();
        registerToolRecipes();
    }

    public static void registerLateRecipes() {
        if (FunctionalStorageConfig.GENERAL.registerExtraCompactingRules) {
            registerExtraCompactingRules();
        }
    }

    private static void registerDrawerRecipes() {
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            ItemStack planks = block.getWoodType()
                .getPlankStack();
            DrawerLayout layout = block.getDrawerLayout();
            ItemStack result = new ItemStack(block, 1, 0);

            if (layout == DrawerLayout.X_1) {
                GameRegistry.addRecipe(new ShapedOreRecipe(result, "PPP", "PCP", "PPP", 'P', planks, 'C', "chestWood"));
            } else if (layout == DrawerLayout.X_2) {
                GameRegistry.addRecipe(new ShapedOreRecipe(result, "PCP", "PPP", "PCP", 'P', planks, 'C', "chestWood"));
            } else {
                GameRegistry.addRecipe(new ShapedOreRecipe(result, "CPC", "PPP", "CPC", 'P', planks, 'C', "chestWood"));
            }
        }

        for (FluidDrawerBlock block : RegistrationHandler.fluidDrawers) {
            ItemStack planks = new ItemStack(Blocks.planks, 1, 0);
            DrawerLayout layout = block.getDrawerLayout();
            ItemStack result = new ItemStack(block, 1, 0);
            if (layout == DrawerLayout.X_1) {
                GameRegistry
                    .addRecipe(new ShapedOreRecipe(result, "PPP", "PGP", "PPP", 'P', planks, 'G', "blockGlass"));
            } else if (layout == DrawerLayout.X_2) {
                GameRegistry
                    .addRecipe(new ShapedOreRecipe(result, "PGP", "PPP", "PGP", 'P', planks, 'G', "blockGlass"));
            } else {
                GameRegistry
                    .addRecipe(new ShapedOreRecipe(result, "GPG", "PPP", "GPG", 'P', planks, 'G', "blockGlass"));
            }
        }
    }

    private static void registerFramedRecipes(ItemStack material) {
        for (FramedDrawerBlock framed : RegistrationHandler.framedDrawers) {
            ItemStack wooden = matchingWoodenDrawer(framed.getDrawerLayout());
            if (wooden == null) {
                continue;
            }
            GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(framed), "MM", "MW", 'M', material, 'W', wooden));
        }
    }

    @Nullable
    private static ItemStack matchingWoodenDrawer(DrawerLayout layout) {
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            if (block.getDrawerLayout() == layout && block.getWoodType() == DrawerWoodType.OAK) {
                return new ItemStack(block);
            }
        }
        return null;
    }

    private static void registerUpgradeRecipes() {
        ItemStack chest = new ItemStack(Blocks.chest);
        registerTieredUpgrade(RegistrationHandler.ironDowngrade, "ingotIron", chest);
        registerTieredUpgrade(RegistrationHandler.copperUpgrade, "ingotCopper", chest);
        registerTieredUpgrade(RegistrationHandler.goldUpgrade, "ingotGold", chest);
        registerTieredUpgrade(RegistrationHandler.diamondUpgrade, "gemDiamond", chest);

        if (RegistrationHandler.voidUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.voidUpgrade),
                    "O O",
                    " C ",
                    "O O",
                    'O',
                    Blocks.obsidian,
                    'C',
                    chest));
        }
        if (RegistrationHandler.redstoneUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.redstoneUpgrade),
                    " R ",
                    "RCR",
                    " R ",
                    'R',
                    Items.redstone,
                    'C',
                    chest));
        }
        if (RegistrationHandler.pullingUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.pullingUpgrade),
                    "H H",
                    " C ",
                    "H H",
                    'H',
                    Blocks.hopper,
                    'C',
                    chest));
        }
        if (RegistrationHandler.pushingUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.pushingUpgrade),
                    " H ",
                    "HCH",
                    " H ",
                    'H',
                    Blocks.hopper,
                    'C',
                    chest));
        }
        if (RegistrationHandler.collectorUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.collectorUpgrade),
                    "B B",
                    " C ",
                    "B B",
                    'B',
                    Blocks.hopper,
                    'C',
                    Items.bucket));
        }
        if (RegistrationHandler.oreDictionaryUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.oreDictionaryUpgrade),
                    "W W",
                    " C ",
                    "W W",
                    'W',
                    Items.writable_book,
                    'C',
                    chest));
        }

        registerAutomationRecipes(chest);
        registerGenerationRecipes(chest);
    }

    private static void registerGenerationRecipes(ItemStack chest) {
        registerGenerationTier(RegistrationHandler.waterGenerationUpgrades, new ItemStack(Items.water_bucket), chest);
        registerGenerationTier(
            RegistrationHandler.stoneGenerationUpgrades,
            new ItemStack(Blocks.cobblestone, 8),
            chest);
        registerGenerationTier(
            RegistrationHandler.universalGenerationUpgrades,
            new ItemStack(Items.nether_star),
            chest);
    }

    private static void registerGenerationTier(List<GenerationUpgradeItem> upgrades, ItemStack resource,
        ItemStack chest) {
        int amount = 1;
        for (GenerationUpgradeItem upgrade : upgrades) {
            int copies = Math.min(8, amount);
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(upgrade),
                    "RRR",
                    "RCR",
                    "RRR",
                    'R',
                    new ItemStack(resource.getItem(), copies, resource.getItemDamage()),
                    'C',
                    chest));
            amount *= 2;
        }
    }

    private static void registerAutomationRecipes(ItemStack chest) {
        if (RegistrationHandler.breakerUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.breakerUpgrade),
                    "PIP",
                    " C ",
                    "PIP",
                    'P',
                    Items.iron_pickaxe,
                    'I',
                    "ingotIron",
                    'C',
                    chest));
        }
        if (RegistrationHandler.placerUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.placerUpgrade),
                    "PBP",
                    " C ",
                    "PBP",
                    'P',
                    Blocks.piston,
                    'B',
                    Blocks.dispenser,
                    'C',
                    chest));
        }
        if (RegistrationHandler.refillUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.refillUpgrade),
                    "B B",
                    " C ",
                    "B B",
                    'B',
                    Items.bowl,
                    'C',
                    chest));
        }
        if (RegistrationHandler.dimensionalRefillUpgrade != null && RegistrationHandler.refillUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.dimensionalRefillUpgrade),
                    " E ",
                    "EUE",
                    " E ",
                    'E',
                    Items.ender_pearl,
                    'U',
                    new ItemStack(RegistrationHandler.refillUpgrade)));
        }
        if (RegistrationHandler.stonecuttingUpgrade != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.stonecuttingUpgrade),
                    "S S",
                    " C ",
                    "S S",
                    'S',
                    Blocks.stone,
                    'C',
                    chest));
        }
    }

    private static void registerTieredUpgrade(Item upgrade, String material, ItemStack core) {
        if (upgrade == null) {
            return;
        }
        GameRegistry
            .addRecipe(new ShapedOreRecipe(new ItemStack(upgrade), "MMM", "MCM", "MMM", 'M', material, 'C', core));
    }

    private static void registerToolRecipes() {
        if (RegistrationHandler.configurationTool != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.configurationTool),
                    "S S",
                    " I ",
                    "S S",
                    'S',
                    Items.stick,
                    'I',
                    "ingotIron"));
        }
        if (RegistrationHandler.linkingTool != null) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(RegistrationHandler.linkingTool),
                    " IE",
                    " SI",
                    "S  ",
                    'S',
                    Items.stick,
                    'I',
                    "ingotIron",
                    'E',
                    Items.ender_pearl));
        }
    }

    private static void registerExtraCompactingRules() {
        List<String> parsed = new ArrayList<>();
        for (String rule : FunctionalStorageConfig.GENERAL.extraCompactingRules) {
            if (rule != null && !rule.trim()
                .isEmpty()) {
                parsed.add(rule.trim());
            }
        }
    }

    public static List<Block> blocksOf(@Nonnull DrawerWoodType woodType) {
        List<Block> blocks = new ArrayList<>();
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            if (block.getWoodType() == woodType) {
                blocks.add(block);
            }
        }
        return blocks;
    }
}
