package com.hfstudio.functionalstorage.misc;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.api.storage.WoodTypeRegistry;
import com.hfstudio.functionalstorage.common.FSItemList;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.block.WoodDrawerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.ResourceGenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.recipe.AdaptiveDrawerRecipe;
import com.hfstudio.functionalstorage.common.recipe.DrawerCleansingRecipe;
import com.hfstudio.functionalstorage.common.recipe.DrawerCraftingRecipe;
import com.hfstudio.functionalstorage.common.recipe.FramedDrawerStyleRecipe;
import com.hfstudio.functionalstorage.common.recipe.UpgradeConversionRecipe;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.registry.GameRegistry;

public class FunctionalStorageRecipes {

    private static final String ORE_IRON_NUGGET = "nuggetIron";
    private static final String ORE_NETHERITE_INGOT = "ingotNetherite";
    private static final String ORE_COPPER_INGOT = "ingotCopper";
    private static final String ORE_COPPER_BLOCK = "blockCopper";
    private static final String DRAWER_ORE = "drawerFunctionalStorage";

    public static void registerRecipes() {
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            OreDictionary.registerOre(DRAWER_ORE, new ItemStack(block));
        }
        for (FramedDrawerBlock block : RegistrationHandler.framedDrawers) {
            OreDictionary.registerOre(DRAWER_ORE, new ItemStack(block));
        }
        GameRegistry.addRecipe(new FramedDrawerStyleRecipe());

        registerWoodDrawerRecipes();
        registerFluidDrawerRecipes();
        registerFramedRecipes();
        registerEssentiaRecipes();
        registerMachineRecipes();
        registerStorageUpgradeRecipes();
        registerUtilityUpgradeRecipes();
        registerGenerationRecipes();
        registerToolRecipes();
        registerCleansingRecipes();
    }

    private static void registerCleansingRecipes() {
        ItemStack display = null;
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            display = new ItemStack(block);
            break;
        }
        if (display == null) {
            for (FramedDrawerBlock block : RegistrationHandler.framedDrawers) {
                display = new ItemStack(block);
                break;
            }
        }
        if (display == null) {
            return;
        }
        GameRegistry.addRecipe(new DrawerCleansingRecipe(DRAWER_ORE, display));
    }

    private static void registerWoodDrawerRecipes() {
        for (IWoodType wood : WoodTypeRegistry.available()) {
            if (!wood.isAvailable()) {
                continue;
            }
            ItemStack planks = wood.getPlankStack();
            if (planks == null) {
                continue;
            }
            for (WoodDrawerBlock drawer : RegistrationHandler.woodDrawers) {
                if (drawer.getWoodType() == wood) {
                    registerWoodDrawer(
                        wood,
                        drawer.getDrawerLayout()
                            .getSlotCount(),
                        planks);
                }
            }
        }
    }

    private static void registerWoodDrawer(IWoodType wood, int slots, ItemStack planks) {
        FSItemList entry = drawerEntry(wood, slots);
        if (entry == null || !entry.hasBeenSet()) {
            return;
        }
        ItemStack drawer = entry.get();
        GameRegistry.addRecipe(new DrawerCraftingRecipe(drawer, false, drawerRecipe(slots, "chestWood", planks)));
        registerFrameConversion(drawer, slots);
    }

    private static FSItemList drawerEntry(IWoodType wood, int slots) {
        return FSItemList.byName(wood.getName() + "_" + slots);
    }

    private static void registerFluidDrawerRecipes() {
        registerOreRecipe(FSItemList.FluidDrawer1.get(), "PPP", "PCP", "PPP", 'C', Items.bucket, 'P', "plankWood");
        registerOreRecipe(FSItemList.FluidDrawer2.get(), "PCP", "PPP", "PCP", 'C', Items.bucket, 'P', "plankWood");
        registerOreRecipe(FSItemList.FluidDrawer4.get(), "CPC", "PCP", "CPC", 'C', Items.bucket, 'P', "plankWood");
    }

    private static void registerFramedRecipes() {
        Object ironNugget = oreOr(ORE_IRON_NUGGET, Items.iron_ingot);
        registerOreRecipe(FSItemList.FramedDrawer1.get(), "PPP", "PCP", "PPP", 'C', "chestWood", 'P', ironNugget);
        registerOreRecipe(FSItemList.FramedDrawer2.get(), "PCP", "PPP", "PCP", 'C', "chestWood", 'P', ironNugget);
        registerOreRecipe(FSItemList.FramedDrawer4.get(), "CPC", "PCP", "CPC", 'C', "chestWood", 'P', ironNugget);
        registerOreRecipe(FSItemList.FramedFluidDrawer1.get(), "PPP", "PCP", "PPP", 'C', Items.bucket, 'P', ironNugget);
        registerOreRecipe(FSItemList.FramedFluidDrawer2.get(), "PCP", "PPP", "PCP", 'C', Items.bucket, 'P', ironNugget);
        registerOreRecipe(FSItemList.FramedFluidDrawer4.get(), "CPC", "PCP", "CPC", 'C', Items.bucket, 'P', ironNugget);
    }

    private static void registerEssentiaRecipes() {
        for (var block : RegistrationHandler.essentiaDrawers) {
            int count = block.getFaceLayout()
                .getSlotCount();
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(
                    new ItemStack(block, count),
                    false,
                    drawerRecipe(
                        count,
                        Item.itemRegistry.getObject(Mods.Thaumcraft.modid + ":BlockJarFilledItem"),
                        "plankWood")));
        }
    }

    private static void registerMachineRecipes() {
        Object ironNugget = oreOr(ORE_IRON_NUGGET, Items.iron_ingot);
        registerOreRecipe(
            FSItemList.NetheriteUpgrade.get(),
            "GGG",
            "GDG",
            "GNG",
            'G',
            "gemDiamond",
            'D',
            FSItemList.DiamondUpgrade.get(),
            'N',
            oreOr(ORE_NETHERITE_INGOT, Items.nether_star));
        registerOreRecipe(
            FSItemList.CompactingDrawer.get(),
            "SSS",
            "PDP",
            "SIS",
            'D',
            DRAWER_ORE,
            'I',
            "ingotIron",
            'P',
            Blocks.piston,
            'S',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.SimpleCompactingDrawer.get(),
            "SSS",
            "SDP",
            "SIS",
            'D',
            DRAWER_ORE,
            'I',
            "ingotIron",
            'P',
            Blocks.piston,
            'S',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.CompactingFramedDrawer.get(),
            "SSS",
            "PDP",
            "SIS",
            'D',
            DRAWER_ORE,
            'I',
            "ingotIron",
            'P',
            Blocks.piston,
            'S',
            ironNugget);
        registerOreRecipe(
            FSItemList.FramedSimpleCompactingDrawer.get(),
            "SSS",
            "SDP",
            "SIS",
            'D',
            DRAWER_ORE,
            'I',
            "ingotIron",
            'P',
            Blocks.piston,
            'S',
            ironNugget);
        registerOreRecipe(
            FSItemList.EnderDrawer.get(),
            "PLP",
            "LCL",
            "PLP",
            'C',
            Blocks.ender_chest,
            'L',
            DRAWER_ORE,
            'P',
            "plankWood");
        registerOreRecipe(
            FSItemList.ArmoryCabinet.get(),
            "ICI",
            "CDC",
            "IBI",
            'B',
            oreOr(ORE_NETHERITE_INGOT, Items.nether_star),
            'C',
            DRAWER_ORE,
            'D',
            Items.comparator,
            'I',
            Blocks.stone);
        registerControllerRecipe(FSItemList.StorageController.get(), Items.comparator, Blocks.stone);
        registerControllerRecipe(FSItemList.ControllerExtension.get(), Items.repeater, Blocks.stone);
        registerControllerRecipe(FSItemList.FramedStorageController.get(), Items.comparator, ironNugget);
        registerControllerRecipe(FSItemList.FramedControllerExtension.get(), Items.repeater, ironNugget);
        registerAdaptiveRecipes();
    }

    private static void registerAdaptiveRecipes() {
        if (!FunctionalStorageConfig.ADAPTIVE.enabled) return;
        registerOreRecipe(
            FSItemList.AdaptiveUpgrade.get(),
            "RGR",
            "GDG",
            "RGR",
            'R',
            "dustRedstone",
            'G',
            Blocks.glass_pane,
            'D',
            "gemDiamond");
        GameRegistry.addRecipe(
            new ShapelessOreRecipe(
                FSItemList.AdaptiveConfigurationTool.get(),
                FSItemList.ConfigurationTool.get(),
                Items.diamond));
        registerOreRecipe(
            FSItemList.AdaptiveMatrix.get(),
            "DGI",
            "GBG",
            "IGD",
            'D',
            "gemDiamond",
            'G',
            Blocks.glass,
            'I',
            "ingotIron",
            'B',
            FSItemList.StorageController.get());
        registerOreRecipe(
            FSItemList.AdaptiveDeposit.get(),
            "IHI",
            "RMR",
            "III",
            'I',
            "ingotIron",
            'H',
            Blocks.hopper,
            'R',
            "dustRedstone",
            'M',
            FSItemList.AdaptiveMatrix.get());
        registerOreRecipe(
            FSItemList.AdaptiveGrid.get(),
            "IRI",
            "RMR",
            "IRI",
            'I',
            "ingotIron",
            'R',
            Items.redstone,
            'M',
            FSItemList.AdaptiveMatrix.get());
        registerOreRecipe(
            FSItemList.AdaptiveCraftingGrid.get(),
            "ICI",
            "RGR",
            "III",
            'I',
            "ingotIron",
            'C',
            Blocks.crafting_table,
            'R',
            "dustRedstone",
            'G',
            FSItemList.AdaptiveGrid.get());
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveDrawer,
            FSItemList.SpruceDrawer1,
            FSItemList.SpruceDrawer2,
            FSItemList.SpruceDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveOakDrawer,
            FSItemList.OakDrawer1,
            FSItemList.OakDrawer2,
            FSItemList.OakDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveBirchDrawer,
            FSItemList.BirchDrawer1,
            FSItemList.BirchDrawer2,
            FSItemList.BirchDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveJungleDrawer,
            FSItemList.JungleDrawer1,
            FSItemList.JungleDrawer2,
            FSItemList.JungleDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveAcaciaDrawer,
            FSItemList.AcaciaDrawer1,
            FSItemList.AcaciaDrawer2,
            FSItemList.AcaciaDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveDarkOakDrawer,
            FSItemList.DarkOakDrawer1,
            FSItemList.DarkOakDrawer2,
            FSItemList.DarkOakDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveMangroveDrawer,
            FSItemList.MangroveDrawer1,
            FSItemList.MangroveDrawer2,
            FSItemList.MangroveDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveCherryDrawer,
            FSItemList.CherryDrawer1,
            FSItemList.CherryDrawer2,
            FSItemList.CherryDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveCrimsonDrawer,
            FSItemList.CrimsonDrawer1,
            FSItemList.CrimsonDrawer2,
            FSItemList.CrimsonDrawer4);
        registerAdaptiveDrawerRecipes(
            FSItemList.AdaptiveWarpedDrawer,
            FSItemList.WarpedDrawer1,
            FSItemList.WarpedDrawer2,
            FSItemList.WarpedDrawer4);
        registerAdaptiveDrawerRecipes(FSItemList.AdaptiveFluidDrawer1, FSItemList.FluidDrawer1);
        registerAdaptiveDrawerRecipes(FSItemList.AdaptiveFluidDrawer2, FSItemList.FluidDrawer2);
        registerAdaptiveDrawerRecipes(FSItemList.AdaptiveFluidDrawer4, FSItemList.FluidDrawer4);
        if (FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Mods.Thaumcraft.isModLoaded()) {
            registerAdaptiveDrawerRecipes(FSItemList.AdaptiveEssentiaDrawer1, FSItemList.EssentiaDrawer1);
            registerAdaptiveDrawerRecipes(FSItemList.AdaptiveEssentiaDrawer2, FSItemList.EssentiaDrawer2);
            registerAdaptiveDrawerRecipes(FSItemList.AdaptiveEssentiaDrawer4, FSItemList.EssentiaDrawer4);
        }
    }

    private static void registerAdaptiveDrawerRecipes(FSItemList output, FSItemList... inputs) {
        for (FSItemList input : inputs) {
            GameRegistry
                .addRecipe(new AdaptiveDrawerRecipe(output.get(), input.get(), FSItemList.AdaptiveUpgrade.get()));
        }
    }

    private static void registerControllerRecipe(ItemStack controller, Object center, Object frame) {
        registerOreRecipe(
            controller,
            "IBI",
            "CDC",
            "IBI",
            'B',
            Blocks.quartz_block,
            'C',
            DRAWER_ORE,
            'D',
            center,
            'I',
            frame);
    }

    private static void registerStorageUpgradeRecipes() {
        registerOreRecipe(FSItemList.BlankUpgrade.get(), "ISI", "S S", "ISI", 'I', "ingotIron", 'S', "stickWood");
        registerOreRecipe(
            FSItemList.IronDowngrade.get(),
            "III",
            "IAI",
            "III",
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            "ingotIron");
        registerOreRecipe(
            FSItemList.CopperUpgrade.get(),
            "IBI",
            "CAC",
            "IBI",
            'B',
            oreOr(ORE_COPPER_BLOCK, Blocks.iron_block),
            'C',
            "chestWood",
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            oreOr(ORE_COPPER_INGOT, Items.iron_ingot));
        registerOreRecipe(
            FSItemList.GoldUpgrade.get(),
            "IBI",
            "CDC",
            "BIB",
            'B',
            "blockGold",
            'C',
            "chestWood",
            'D',
            FSItemList.CopperUpgrade.get(),
            'I',
            "ingotGold");
        registerOreRecipe(
            FSItemList.DiamondUpgrade.get(),
            "BIB",
            "CDC",
            "BIB",
            'B',
            "blockDiamond",
            'C',
            "chestWood",
            'D',
            FSItemList.GoldUpgrade.get(),
            'I',
            "gemDiamond");
        registerOreRecipe(
            FSItemList.NetherStarUpgrade.get(),
            "CDC",
            "DTD",
            "CDC",
            'C',
            "blockDiamond",
            'D',
            Items.nether_star,
            'T',
            FSItemList.NetheriteUpgrade.get());
        registerOreRecipe(
            FSItemList.MaxStorageUpgrade.get(),
            "IDI",
            "BNB",
            "IBI",
            'B',
            Items.nether_star,
            'D',
            FSItemList.NetheriteUpgrade.get(),
            'I',
            "gemDiamond",
            'N',
            FSItemList.NetherStarUpgrade.get());
    }

    private static void registerUtilityUpgradeRecipes() {
        registerOreRecipe(
            FSItemList.VoidUpgrade.get(),
            "III",
            "IAI",
            "III",
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            Blocks.obsidian);
        registerOreRecipe(
            FSItemList.RedstoneUpgrade.get(),
            "IBI",
            "CAC",
            "IBI",
            'B',
            Blocks.redstone_block,
            'C',
            Items.comparator,
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.PullingUpgrade.get(),
            "ICI",
            "IAI",
            "IBI",
            'B',
            "dustRedstone",
            'C',
            Blocks.hopper,
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.PushingUpgrade.get(),
            "IBI",
            "IAI",
            "IRI",
            'B',
            "dustRedstone",
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            Blocks.stone,
            'R',
            Blocks.hopper);
        GameRegistry.addRecipe(
            new UpgradeConversionRecipe(
                FSItemList.PullingUpgrade.get(),
                FSItemList.PushingUpgrade.get(),
                " U ",
                " A ",
                "   ",
                'U',
                FSItemList.PushingUpgrade.get(),
                'A',
                FSItemList.BlankUpgrade.get()));
        GameRegistry.addRecipe(
            new UpgradeConversionRecipe(
                FSItemList.PushingUpgrade.get(),
                FSItemList.PullingUpgrade.get(),
                " U ",
                " A ",
                "   ",
                'U',
                FSItemList.PullingUpgrade.get(),
                'A',
                FSItemList.BlankUpgrade.get()));
        registerOreRecipe(
            FSItemList.WirelessPullingUpgrade.get(),
            " E ",
            " U ",
            "   ",
            'U',
            FSItemList.PullingUpgrade.get(),
            'E',
            Items.ender_pearl);
        registerOreRecipe(
            FSItemList.WirelessPushingUpgrade.get(),
            " E ",
            " U ",
            'U',
            FSItemList.PushingUpgrade.get(),
            'E',
            Items.ender_pearl);
        registerOreRecipe(
            FSItemList.CollectorUpgrade.get(),
            "IBI",
            "RAR",
            "IBI",
            'B',
            Blocks.hopper,
            'A',
            FSItemList.BlankUpgrade.get(),
            'I',
            Blocks.stone,
            'R',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.BreakerUpgrade.get(),
            "RSR",
            "SAS",
            "RPR",
            'A',
            FSItemList.BlankUpgrade.get(),
            'P',
            Items.iron_pickaxe,
            'R',
            "dustRedstone",
            'S',
            Blocks.stonebrick);
        registerOreRecipe(
            FSItemList.PlacerUpgrade.get(),
            "RdR",
            "dAd",
            "RtR",
            'A',
            FSItemList.BlankUpgrade.get(),
            'R',
            "dustRedstone",
            'd',
            Blocks.dispenser,
            't',
            Blocks.dirt);
        registerOreRecipe(
            FSItemList.RefillUpgrade.get(),
            "RPR",
            "PAP",
            "RCR",
            'C',
            "chestWood",
            'A',
            FSItemList.BlankUpgrade.get(),
            'P',
            Items.ender_pearl,
            'R',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.DimensionalRefillUpgrade.get(),
            " W ",
            "PAP",
            " U ",
            'A',
            FSItemList.BlankUpgrade.get(),
            'P',
            Items.ender_eye,
            'U',
            FSItemList.RefillUpgrade.get(),
            'W',
            new ItemStack(Items.skull, 1, 1));
        registerOreRecipe(
            FSItemList.SpeedUpgradeAugment.get(),
            "RBR",
            "BAB",
            "RBR",
            'B',
            Items.blaze_powder,
            'A',
            FSItemList.BlankUpgrade.get(),
            'R',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.EfficiencyUpgradeAugment.get(),
            "DSD",
            "DAD",
            "DDD",
            'A',
            FSItemList.BlankUpgrade.get(),
            'D',
            "gemDiamond",
            'S',
            FSItemList.SpeedUpgradeAugment.get());
    }

    private static void registerGenerationRecipes() {
        registerOreRecipe(
            FSItemList.DrippingUpgrade.get(),
            "IBI",
            "DAD",
            "IRI",
            'A',
            FSItemList.BlankUpgrade.get(),
            'B',
            Blocks.netherrack,
            'D',
            Items.cauldron,
            'I',
            Blocks.stone,
            'R',
            Items.lava_bucket);
        registerOreRecipe(
            FSItemList.ObsidianUpgrade.get(),
            "DWD",
            " A ",
            "D D",
            'A',
            FSItemList.BlankUpgrade.get(),
            'D',
            FSItemList.DrippingUpgrade.get(),
            'W',
            FSItemList.WaterGenerationUpgrade1.get());
        registerGenerationTier(RegistrationHandler.waterGenerationUpgrades, new ItemStack(Items.water_bucket));
        registerGenerationTier(RegistrationHandler.stoneGenerationUpgrades, new ItemStack(Blocks.cobblestone));
        for (List<ResourceGenerationUpgradeItem> upgrades : RegistrationHandler.getItemGenerationUpgradeGroups()
            .values()) {
            ResourceGenerationUpgradeItem first = upgrades.get(0);
            registerResourceGenerationTier(
                upgrades,
                first.getGeneratedItem(),
                first == RegistrationHandler.obsidianUpgrade);
        }
        for (List<ResourceGenerationUpgradeItem> upgrades : RegistrationHandler.getFluidGenerationUpgradeGroups()
            .values()) {
            ResourceGenerationUpgradeItem first = upgrades.get(0);
            registerResourceGenerationTier(
                upgrades,
                findFluidContainer(first.getGeneratedFluid()),
                first == RegistrationHandler.drippingUpgrade);
        }
    }

    private static void registerGenerationTier(List<GenerationUpgradeItem> upgrades, ItemStack resource) {
        ItemStack core = FSItemList.BlankUpgrade.get();
        for (GenerationUpgradeItem upgrade : upgrades) {
            registerOreRecipe(new ItemStack(upgrade), "RRR", "RCR", "RRR", 'R', resource, 'C', core);
            core = new ItemStack(upgrade);
        }
    }

    private static void registerResourceGenerationTier(List<ResourceGenerationUpgradeItem> upgrades, ItemStack resource,
        boolean hasBaseRecipe) {
        if (resource == null || resource.getItem() == null) return;
        ItemStack core = hasBaseRecipe ? new ItemStack(upgrades.get(0)) : FSItemList.BlankUpgrade.get();
        for (int tier = hasBaseRecipe ? 1 : 0; tier < upgrades.size(); tier++) {
            ItemStack output = new ItemStack(upgrades.get(tier));
            registerOreRecipe(output, "RRR", "RCR", "RRR", 'R', resource, 'C', core);
            core = output;
        }
    }

    private static ItemStack findFluidContainer(FluidStack fluid) {
        if (fluid == null) return null;
        for (FluidContainerData container : FluidContainerRegistry.getRegisteredFluidContainerData()) {
            if (container.fluid != null && container.fluid.isFluidEqual(fluid)
                && container.filledContainer != null
                && container.filledContainer.getItem() != null) {
                return container.filledContainer.copy();
            }
        }
        return null;
    }

    private static void registerToolRecipes() {
        registerOreRecipe(
            FSItemList.ConfigurationTool.get(),
            "PPG",
            "PDG",
            "PEP",
            'D',
            DRAWER_ORE,
            'E',
            Items.emerald,
            'G',
            "ingotGold",
            'P',
            Items.paper);
        registerOreRecipe(
            FSItemList.LinkingTool.get(),
            "PPG",
            "PDG",
            "PEP",
            'D',
            DRAWER_ORE,
            'E',
            Items.diamond,
            'G',
            "ingotGold",
            'P',
            Items.paper);
    }

    private static void registerFrameConversion(ItemStack source, int slots) {
        FramedDrawerBlock frame = null;
        for (FramedDrawerBlock candidate : RegistrationHandler.framedDrawers) {
            if (candidate.getDrawerLayout()
                .getSlotCount() == slots) {
                frame = candidate;
                break;
            }
        }
        if (frame == null) {
            return;
        }
        GameRegistry.addRecipe(
            new DrawerCraftingRecipe(
                new ItemStack(frame, slots),
                true,
                drawerRecipe(slots, source, oreOr(ORE_IRON_NUGGET, Items.iron_ingot))));
    }

    private static void registerOreRecipe(ItemStack output, Object... recipe) {
        GameRegistry.addRecipe(new ShapedOreRecipe(output, recipe));
    }

    private static Object oreOr(String preferred, Object fallback) {
        return OreDictionary.getOres(preferred)
            .isEmpty() ? fallback : preferred;
    }

    private static Object[] drawerRecipe(int slots, Object center, Object frame) {
        DrawerLayout layout = DrawerLayout.bySlotCount(slots);
        if (layout == null) throw new IllegalArgumentException("No registered drawer layout for " + slots + " slots");
        String[] shape = layout.getRecipePattern();
        return new Object[] { shape[0], shape[1], shape[2], 'C', center, 'P', frame };
    }
}
