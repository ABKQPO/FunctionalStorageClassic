package com.hfstudio.functionalstorage.misc;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.api.storage.WoodTypeRegistry;
import com.hfstudio.functionalstorage.common.FSItemList;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.recipe.DrawerCraftingRecipe;
import com.hfstudio.functionalstorage.common.recipe.UpgradeConversionRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

/** Modern upstream recipe patterns with explicit 1.7.10 ingredient mappings. */
public class ModernStorageRecipes {

    public static void register() {
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.AcaciaDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.AcaciaDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.AcaciaDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 4)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.BirchDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.BirchDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.BirchDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 2)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.CompactingDrawer.get(1),
                false,
                "SSS",
                "PDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack(Blocks.piston, 1, 0),
                'S',
                stack(Blocks.stone, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.CompactingFramedDrawer.get(1),
                false,
                "SSS",
                "PDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack(Blocks.piston, 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.CompactingFramedDrawer.get(1),
                true,
                "S S",
                "SDS",
                " S ",
                'D',
                FSItemList.CompactingDrawer.get(1),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.ConfigurationTool.get(1),
                false,
                "PPG",
                "PDG",
                "PEP",
                'D',
                "drawerFunctionalStorage",
                'E',
                stack(Items.emerald, 1, 0),
                'G',
                "ingotGold",
                'P',
                stack(Items.paper, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.ControllerExtension.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack(Blocks.quartz_block, 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack(Items.repeater, 1, 0),
                'I',
                stack(Blocks.stone, 1, 0)));

        Object netherite = Mods.Etfuturum.isModLoaded() ? "ingotNetherite" : stack(Items.nether_star, 1, 0);

        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.ArmoryCabinet.get(1),
                false,
                "ICI",
                "CDC",
                "IBI",
                'B',
                netherite,
                'C',
                "drawerFunctionalStorage",
                'D',
                stack(Items.comparator, 1, 0),
                'I',
                stack(Blocks.stone, 1, 0)));

        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.CollectorUpgrade.get(1),
                false,
                "IBI",
                "RDR",
                "IBI",
                'B',
                stack(Blocks.hopper, 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack(Blocks.stone, 1, 0),
                'R',
                "dustRedstone"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.CopperUpgrade.get(1),
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
                FSItemList.DarkOakDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.DarkOakDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.DarkOakDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 5)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.DiamondUpgrade.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                "blockDiamond",
                'C',
                "chestWood",
                'D',
                FSItemList.GoldUpgrade.get(1),
                'I',
                "gemDiamond"));

        // TODO: Efr not ready
        // ItemStack dripstone = Mods.Etfuturum.isModLoaded() ? stack(Mods.Etfuturum.modid + ":pointed_dripstone", 1, 0)
        // : stack(Blocks.netherrack, 1, 0);

        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.DrippingUpgrade.get(1),
                false,
                "IBI",
                "IDI",
                "IRI",
                'B',
                stack(Blocks.netherrack, 1, 0),
                'D',
                stack(Blocks.cauldron, 1, 0),
                'I',
                stack(Blocks.stone, 1, 0),
                'R',
                stack(Items.lava_bucket, 1, 0)));

        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.EnderDrawer.get(1),
                false,
                "PPP",
                "LCL",
                "PPP",
                'C',
                stack(Blocks.ender_chest, 1, 0),
                'L',
                "drawerFunctionalStorage",
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FluidDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FluidDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FluidDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "plankWood"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
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
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.OakDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.SpruceDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.BirchDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.JungleDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.AcaciaDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer1.get(1),
                true,
                "PPP",
                "PCP",
                "PPP",
                'C',
                FSItemList.DarkOakDrawer1.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(2),
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
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.OakDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.SpruceDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.BirchDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.JungleDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.AcaciaDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer2.get(1),
                true,
                " P ",
                "PCP",
                " P ",
                'C',
                FSItemList.DarkOakDrawer2.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(4),
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
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.OakDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.SpruceDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.BirchDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.JungleDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.AcaciaDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedDrawer4.get(1),
                true,
                "PCP",
                'C',
                FSItemList.DarkOakDrawer4.get(1),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedControllerExtension.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack(Blocks.quartz_block, 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack(Items.repeater, 1, 0),
                'I',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedFluidDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedFluidDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedFluidDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                stack(Items.bucket, 1, 0),
                'P',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedSimpleCompactingDrawer.get(1),
                false,
                "SSS",
                "SDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack(Blocks.piston, 1, 0),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedSimpleCompactingDrawer.get(1),
                true,
                "SSS",
                "SDS",
                " S ",
                'D',
                FSItemList.SimpleCompactingDrawer.get(1),
                'S',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.FramedStorageController.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack(Blocks.quartz_block, 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack(Items.comparator, 1, 0),
                'I',
                "nuggetIron"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.GoldUpgrade.get(1),
                false,
                "IBI",
                "CDC",
                "BIB",
                'B',
                "blockGold",
                'C',
                "chestWood",
                'D',
                FSItemList.CopperUpgrade.get(1),
                'I',
                "ingotGold"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.IronDowngrade.get(1),
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
                FSItemList.JungleDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.JungleDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.JungleDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 3)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.LinkingTool.get(1),
                false,
                "PPG",
                "PDG",
                "PEP",
                'D',
                "drawerFunctionalStorage",
                'E',
                stack(Items.diamond, 1, 0),
                'G',
                "ingotGold",
                'P',
                stack(Items.paper, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.OakDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.OakDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.OakDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 0)));
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                FSItemList.ObsidianUpgrade.get(1),
                FSItemList.DrippingUpgrade.get(1),
                FSItemList.DrippingUpgrade.get(1),
                FSItemList.DrippingUpgrade.get(1),
                FSItemList.DrippingUpgrade.get(1),
                FSItemList.WaterGeneratorUpgrade.get(1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.PullingUpgrade.get(1),
                false,
                "ICI",
                "IDI",
                "IBI",
                'B',
                "dustRedstone",
                'C',
                stack(Blocks.hopper, 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack(Blocks.stone, 1, 0)));
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PullingUpgrade.get(1), FSItemList.PushingUpgrade.get(1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.PushingUpgrade.get(1),
                false,
                "IBI",
                "IDI",
                "IRI",
                'B',
                "dustRedstone",
                'D',
                "drawerFunctionalStorage",
                'I',
                stack(Blocks.stone, 1, 0),
                'R',
                stack(Blocks.hopper, 1, 0)));
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PushingUpgrade.get(1), FSItemList.PullingUpgrade.get(1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.RedstoneUpgrade.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack(Blocks.redstone_block, 1, 0),
                'C',
                stack(Items.comparator, 1, 0),
                'D',
                "drawerFunctionalStorage",
                'I',
                stack(Items.redstone, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.SimpleCompactingDrawer.get(1),
                false,
                "SSS",
                "SDP",
                "SIS",
                'D',
                "drawerFunctionalStorage",
                'I',
                "ingotIron",
                'P',
                stack(Blocks.piston, 1, 0),
                'S',
                stack(Blocks.stone, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.SpruceDrawer1.get(1),
                false,
                "PPP",
                "PCP",
                "PPP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.SpruceDrawer2.get(2),
                false,
                "PCP",
                "PPP",
                "PCP",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.SpruceDrawer4.get(4),
                false,
                "CPC",
                "PPP",
                "CPC",
                'C',
                "chestWood",
                'P',
                stack(Blocks.planks, 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.StorageController.get(1),
                false,
                "IBI",
                "CDC",
                "IBI",
                'B',
                stack(Blocks.quartz_block, 1, 0),
                'C',
                "drawerFunctionalStorage",
                'D',
                stack(Items.comparator, 1, 0),
                'I',
                stack(Blocks.stone, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.VoidUpgrade.get(1),
                false,
                "III",
                "IDI",
                "III",
                'D',
                "drawerFunctionalStorage",
                'I',
                stack(Blocks.obsidian, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.WaterGeneratorUpgrade.get(1),
                false,
                "IBI",
                "IDI",
                "IBI",
                'B',
                stack(Items.water_bucket, 1, 0),
                'D',
                stack(Items.bucket, 1, 0),
                'I',
                stack(Blocks.stone, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.BreakerUpgrade.get(1),
                false,
                "RSR",
                "SDS",
                "RPR",
                'D',
                "drawerFunctionalStorage",
                'P',
                stack(Items.iron_pickaxe, 1, 0),
                'R',
                "dustRedstone",
                'S',
                stack(Blocks.stonebrick, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.DimensionalRefillUpgrade.get(1),
                false,
                " W ",
                " U ",
                " P ",
                'P',
                stack(Items.ender_eye, 1, 0),
                'U',
                FSItemList.RefillUpgrade.get(1),
                'W',
                stack(Items.skull, 1, 1)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.PlacerUpgrade.get(1),
                false,
                "RdR",
                "dDd",
                "RtR",
                'D',
                "drawerFunctionalStorage",
                'R',
                "dustRedstone",
                'd',
                stack(Blocks.dispenser, 1, 0),
                't',
                stack(Blocks.dirt, 1, 0)));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.RefillUpgrade.get(1),
                false,
                "RPR",
                "PDP",
                "RCR",
                'C',
                "chestWood",
                'D',
                "drawerFunctionalStorage",
                'P',
                stack(Items.ender_pearl, 1, 0),
                'R',
                "dustRedstone"));
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                FSItemList.SpeedUpgradeAugment.get(2),
                false,
                "RBR",
                "BDB",
                "RBR",
                'B',
                stack(Items.blaze_powder, 1, 0),
                'D',
                "drawerFunctionalStorage",
                'R',
                "dustRedstone"));

        registerVariantWoodRecipes();
    }

    /**
     * Registers drawer recipes for woods contributed by other mods, such as the
     * Et Futurum Requiem woods. Each wood is skipped when its blocks are absent,
     * which happens when the mod is missing or a pack disabled that variant.
     */
    public static void registerVariantWoodRecipes() {
        for (IWoodType wood : WoodTypeRegistry.available()) {
            if (wood.getLog() == null || wood.getLog() == Blocks.log || wood.getLog() == Blocks.log2) {
                continue;
            }
            ItemStack planks = wood.getPlankStack();
            if (planks == null) {
                continue;
            }
            String woodName = wood.getName();
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(
                    drawer(woodName, 1, 1),
                    false,
                    "PPP",
                    "PCP",
                    "PPP",
                    'C',
                    "chestWood",
                    'P',
                    planks));
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(
                    drawer(woodName, 2, 2),
                    false,
                    "PCP",
                    "PPP",
                    "PCP",
                    'C',
                    "chestWood",
                    'P',
                    planks));
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(
                    drawer(woodName, 4, 4),
                    false,
                    "CPC",
                    "PPP",
                    "CPC",
                    'C',
                    "chestWood",
                    'P',
                    planks));
        }
    }

    private static ItemStack drawer(String woodName, int slots, int count) {
        FSItemList entry = FSItemList.byId(woodName + "_" + slots);
        return entry == null ? stack(FunctionalStorage.MOD_ID + ":" + woodName + "_" + slots, count, 0)
            : entry.get(count);
    }

    public static ItemStack stack(Item item, int count, int metadata) {
        return new ItemStack(item, count, metadata);
    }

    public static ItemStack stack(Block block, int count, int metadata) {
        return new ItemStack(block, count, metadata);
    }

    public static ItemStack stack(String id, int count, int metadata) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item != null) return new ItemStack(item, count, metadata);
        Block block = Block.getBlockFromName(id);
        return new ItemStack(block, count, metadata);
    }
}
