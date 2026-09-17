package com.hfstudio.functionalstorage.misc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.common.recipe.DrawerCraftingRecipe;
import com.hfstudio.functionalstorage.common.recipe.UpgradeConversionRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

/** Modern upstream recipe patterns with explicit 1.7.10 ingredient mappings. */
public class ModernStorageRecipes {

    private ModernStorageRecipes() {}

    public static void register() {
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:acacia_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:acacia_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:acacia_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:armory_cabinet", 1, 0),
                false,
                "ICI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:nether_star", 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack("minecraft:comparator", 1, 0),
                'I',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:birch_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:birch_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:birch_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:collector_upgrade", 1, 0),
                false,
                "IBI",
                "RDR",
                "IBI",
                'B',
                stack("minecraft:hopper", 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack("minecraft:stone", 1, 0),
                'R',
                "dustRedstone"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:compacting_drawer", 1, 0),
                false,
                "SSS",
                "PDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack("minecraft:piston", 1, 0),
                'S',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:compacting_framed_drawer", 1, 0),
                false,
                "SSS",
                "PDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack("minecraft:piston", 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:compacting_framed_drawer", 1, 0),
                true,
                "S S",
                "SDS",
                " S ",
                'D',
                stack("functionalstorage:compacting_drawer", 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:configuration_tool", 1, 0),
                false,
                "PPG",
                "PDG",
                "PEP",
                'D',
                "drawerFunctionalStorage",
                'E',
                stack("minecraft:emerald", 1, 0),
                'G',
                "ingotGold",
                'P',
                stack("minecraft:paper", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:controller_extension", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:quartz_block", 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack("minecraft:repeater", 1, 0),
                'I',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:copper_upgrade", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                "blockCopper",
                'C',
                "chestWood",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotCopper"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:dark_oak_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:dark_oak_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:dark_oak_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:diamond_upgrade", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                "blockDiamond",
                'C',
                "chestWood",
                'D',
                stack("functionalstorage:gold_upgrade", 1, 0),
                'I',
                "gemDiamond"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:dripping_upgrade", 1, 0),
                false,
                "IBI",
                "IDI",
                "IRI",
                'B',
                stack("minecraft:netherrack", 1, 0),
                'D',
                stack("minecraft:cauldron", 1, 0),
                'I',
                stack("minecraft:stone", 1, 0),
                'R',
                stack("minecraft:lava_bucket", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:ender_drawer", 1, 0),
                false,
                "PPP",
                "LCL",
                "PPP",
                'C',
                stack("minecraft:ender_chest", 1, 0),
                'L',
                "drawerFunctionalStorage",
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:fluid_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:fluid_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:fluid_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:oak_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:spruce_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:birch_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:jungle_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:acacia_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_1", 1, 0),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("functionalstorage:dark_oak_1", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:oak_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:spruce_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:birch_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:jungle_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:acacia_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_2", 1, 0),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                stack("functionalstorage:dark_oak_2", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:oak_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:spruce_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:birch_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:jungle_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:acacia_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_4", 1, 0),
                true,
                "PCP",
                'C',
                stack("functionalstorage:dark_oak_4", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_controller_extension", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:quartz_block", 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack("minecraft:repeater", 1, 0),
                'I',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_fluid_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_fluid_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_fluid_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                stack("minecraft:bucket", 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_simple_compacting_drawer", 1, 0),
                false,
                "SSS",
                "SDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack("minecraft:piston", 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_simple_compacting_drawer", 1, 0),
                true,
                "SSS",
                "SDS",
                " S ",
                'D',
                stack("functionalstorage:simple_compacting_drawer", 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:framed_storage_controller", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:quartz_block", 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack("minecraft:comparator", 1, 0),
                'I',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:gold_upgrade", 1, 0),
                false,
                "IBI",
                "CDC",
                "BIB",
                'B',
                "blockGold",
                'C',
                "chestWood",
                'D',
                stack("functionalstorage:copper_upgrade", 1, 0),
                'I',
                "ingotGold"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:iron_downgrade", 1, 0),
                false,
                "III",
                "IDI",
                "III",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:jungle_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:jungle_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:jungle_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:linking_tool", 1, 0),
                false,
                "PPG",
                "PDG",
                "PEP",
                'D',
                "drawerFunctionalStorage",
                'E',
                stack("minecraft:diamond", 1, 0),
                'G',
                "ingotGold",
                'P',
                stack("minecraft:paper", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:oak_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:oak_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:oak_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 0)));
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                stack("functionalstorage:obsidian_upgrade", 1, 0),
                stack("functionalstorage:dripping_upgrade", 1, 0),
                stack("functionalstorage:dripping_upgrade", 1, 0),
                stack("functionalstorage:dripping_upgrade", 1, 0),
                stack("functionalstorage:dripping_upgrade", 1, 0),
                stack("functionalstorage:water_generator_upgrade", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:pulling_upgrade", 1, 0),
                false,
                "ICI",
                "IDI",
                "IBI",
                'B',
                "dustRedstone",
                'C',
                stack("minecraft:hopper", 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new UpgradeConversionRecipe(
                stack("functionalstorage:pulling_upgrade", 1, 0),
                stack("functionalstorage:pushing_upgrade", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:pushing_upgrade", 1, 0),
                false,
                "IBI",
                "IDI",
                "IRI",
                'B',
                "dustRedstone",
                'D',
                "drawerFunctionalStorage",
                'I',
                stack("minecraft:stone", 1, 0),
                'R',
                stack("minecraft:hopper", 1, 0)));
        GameRegistry.addRecipe(
            new UpgradeConversionRecipe(
                stack("functionalstorage:pushing_upgrade", 1, 0),
                stack("functionalstorage:pulling_upgrade", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:redstone_upgrade", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:redstone_block", 1, 0),
                'C',
                stack("minecraft:comparator", 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack("minecraft:redstone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:simple_compacting_drawer", 1, 0),
                false,
                "SSS",
                "SDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack("minecraft:piston", 1, 0),
                'S',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:spruce_1", 1, 0),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:spruce_2", 2, 0),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:spruce_4", 4, 0),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack("minecraft:planks", 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:storage_controller", 1, 0),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack("minecraft:quartz_block", 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack("minecraft:comparator", 1, 0),
                'I',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:void_upgrade", 1, 0),
                false,
                "III",
                "IDI",
                "III",
                'D',
                "drawerFunctionalStorage",
                'I',
                stack("minecraft:obsidian", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:water_generator_upgrade", 1, 0),
                false,
                "IBI",
                "IDI",
                "IBI",
                'B',
                stack("minecraft:water_bucket", 1, 0),
                'D',
                stack("minecraft:bucket", 1, 0),
                'I',
                stack("minecraft:stone", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:breaker_upgrade", 1, 0),
                false,
                "RSR",
                "SDS",
                "RPR",
                'D',
                "drawerFunctionalStorage",
                'P',
                stack("minecraft:iron_pickaxe", 1, 0),
                'R',
                "dustRedstone",
                'S',
                stack("minecraft:stonebrick", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:dimensional_refill_upgrade", 1, 0),
                false,
                " W ",
                " U ",
                " P ",
                'P',
                stack("minecraft:ender_eye", 1, 0),
                'U',
                stack("functionalstorage:refill_upgrade", 1, 0),
                'W',
                stack("minecraft:skull", 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:placer_upgrade", 1, 0),
                false,
                "RdR",
                "dDd",
                "RtR",
                'D',
                "drawerFunctionalStorage",
                'R',
                "dustRedstone",
                'd',
                stack("minecraft:dispenser", 1, 0),
                't',
                stack("minecraft:dirt", 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:refill_upgrade", 1, 0),
                false,
                "RPR",
                "PDP",
                "RCR",
                'C',
                "chestWood",
                'D',
                "drawerFunctionalStorage",
                'P',
                stack("minecraft:ender_pearl", 1, 0),
                'R',
                "dustRedstone"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                stack("functionalstorage:speed_upgrade_augment", 2, 0),
                false,
                "RBR",
                "BDB",
                "RBR",
                'B',
                stack("minecraft:blaze_powder", 1, 0),
                'D',
                "drawerFunctionalStorage",
                'R',
                "dustRedstone"));
    }

    private static ItemStack stack(String id, int count, int metadata) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item == null) throw new IllegalArgumentException("Unregistered recipe item: " + id);
        return new ItemStack(item, count, metadata);
    }
}
