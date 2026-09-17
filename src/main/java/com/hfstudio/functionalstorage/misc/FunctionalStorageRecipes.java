package com.hfstudio.functionalstorage.misc;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import com.hfstudio.functionalstorage.common.recipe.DrawerCraftingRecipe;
import com.hfstudio.functionalstorage.common.recipe.FramedDrawerStyleRecipe;
import com.hfstudio.functionalstorage.common.recipe.UpgradeConversionRecipe;

import cpw.mods.fml.common.registry.GameRegistry;

public class FunctionalStorageRecipes {

    private static final String ORE_IRON_NUGGET = "nuggetIron";
    private static final String ORE_NETHERITE_INGOT = "ingotNetherite";
    private static final String ORE_COPPER_INGOT = "ingotCopper";
    private static final String ORE_COPPER_BLOCK = "blockCopper";

    public static void registerRecipes() {
        for (WoodDrawerBlock block : RegistrationHandler.woodDrawers) {
            OreDictionary.registerOre("drawerFunctionalStorage", new ItemStack(block));
        }
        for (FramedDrawerBlock block : RegistrationHandler.framedDrawers) {
            OreDictionary.registerOre("drawerFunctionalStorage", new ItemStack(block));
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
            registerWoodDrawer(wood, 1, planks);
            registerWoodDrawer(wood, 2, planks);
            registerWoodDrawer(wood, 4, planks);
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
        return switch (wood.getName()) {
            case "oak" -> slots == 1 ? FSItemList.OakDrawer1
                : slots == 2 ? FSItemList.OakDrawer2 : FSItemList.OakDrawer4;
            case "spruce" -> slots == 1 ? FSItemList.SpruceDrawer1
                : slots == 2 ? FSItemList.SpruceDrawer2 : FSItemList.SpruceDrawer4;
            case "birch" -> slots == 1 ? FSItemList.BirchDrawer1
                : slots == 2 ? FSItemList.BirchDrawer2 : FSItemList.BirchDrawer4;
            case "jungle" -> slots == 1 ? FSItemList.JungleDrawer1
                : slots == 2 ? FSItemList.JungleDrawer2 : FSItemList.JungleDrawer4;
            case "acacia" -> slots == 1 ? FSItemList.AcaciaDrawer1
                : slots == 2 ? FSItemList.AcaciaDrawer2 : FSItemList.AcaciaDrawer4;
            case "dark_oak" -> slots == 1 ? FSItemList.DarkOakDrawer1
                : slots == 2 ? FSItemList.DarkOakDrawer2 : FSItemList.DarkOakDrawer4;
            case "mangrove" -> slots == 1 ? FSItemList.MangroveDrawer1
                : slots == 2 ? FSItemList.MangroveDrawer2 : FSItemList.MangroveDrawer4;
            case "cherry" -> slots == 1 ? FSItemList.CherryDrawer1
                : slots == 2 ? FSItemList.CherryDrawer2 : FSItemList.CherryDrawer4;
            case "crimson" -> slots == 1 ? FSItemList.CrimsonDrawer1
                : slots == 2 ? FSItemList.CrimsonDrawer2 : FSItemList.CrimsonDrawer4;
            case "warped" -> slots == 1 ? FSItemList.WarpedDrawer1
                : slots == 2 ? FSItemList.WarpedDrawer2 : FSItemList.WarpedDrawer4;
            default -> null;
        };
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
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
            'D',
            Items.comparator,
            'I',
            Blocks.stone);
        registerControllerRecipe(FSItemList.StorageController.get(), Items.comparator, Blocks.stone);
        registerControllerRecipe(FSItemList.ControllerExtension.get(), Items.repeater, Blocks.stone);
        registerControllerRecipe(FSItemList.FramedStorageController.get(), Items.comparator, ironNugget);
        registerControllerRecipe(FSItemList.FramedControllerExtension.get(), Items.repeater, ironNugget);
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
            "drawerFunctionalStorage",
            'D',
            center,
            'I',
            frame);
    }

    private static void registerStorageUpgradeRecipes() {
        registerOreRecipe(
            FSItemList.IronDowngrade.get(),
            "III",
            "IDI",
            "III",
            'D',
            "drawerFunctionalStorage",
            'I',
            "ingotIron");
        registerOreRecipe(
            FSItemList.CopperUpgrade.get(),
            "IBI",
            "CDC",
            "IBI",
            'B',
            oreOr(ORE_COPPER_BLOCK, Blocks.iron_block),
            'C',
            "chestWood",
            'D',
            "drawerFunctionalStorage",
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
            "IBI",
            "CDC",
            "IBI",
            'B',
            "blockDiamond",
            'C',
            "chestWood",
            'D',
            FSItemList.GoldUpgrade.get(),
            'I',
            "gemDiamond");
        registerOreRecipe(
            FSItemList.MaxStorageUpgrade.get(),
            "IBI",
            "CDC",
            "IBI",
            'B',
            Items.nether_star,
            'C',
            "chestWood",
            'D',
            FSItemList.NetheriteUpgrade.get(),
            'I',
            "gemDiamond");
    }

    private static void registerUtilityUpgradeRecipes() {
        registerOreRecipe(
            FSItemList.VoidUpgrade.get(),
            "III",
            "IDI",
            "III",
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.obsidian);
        registerOreRecipe(
            FSItemList.RedstoneUpgrade.get(),
            "IBI",
            "CDC",
            "IBI",
            'B',
            Blocks.redstone_block,
            'C',
            Items.comparator,
            'D',
            "drawerFunctionalStorage",
            'I',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.PullingUpgrade.get(),
            "ICI",
            "IDI",
            "IBI",
            'B',
            "dustRedstone",
            'C',
            Blocks.hopper,
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.PushingUpgrade.get(),
            "IBI",
            "IDI",
            "IRI",
            'B',
            "dustRedstone",
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.stone,
            'R',
            Blocks.hopper);
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PullingUpgrade.get(), FSItemList.PushingUpgrade.get()));
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PushingUpgrade.get(), FSItemList.PullingUpgrade.get()));
        registerShapeless(FSItemList.WirelessPullingUpgrade.get(), FSItemList.PullingUpgrade.get(), Items.ender_pearl);
        registerShapeless(FSItemList.WirelessPushingUpgrade.get(), FSItemList.PushingUpgrade.get(), Items.ender_pearl);
        registerOreRecipe(
            FSItemList.CollectorUpgrade.get(),
            "IBI",
            "RDR",
            "IBI",
            'B',
            Blocks.hopper,
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.stone,
            'R',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.OreDictionaryUpgrade.get(),
            "W W",
            " C ",
            "W W",
            'W',
            Items.writable_book,
            'C',
            Blocks.chest);
        registerOreRecipe(
            FSItemList.BreakerUpgrade.get(),
            "RSR",
            "SDS",
            "RPR",
            'D',
            "drawerFunctionalStorage",
            'P',
            Items.iron_pickaxe,
            'R',
            "dustRedstone",
            'S',
            Blocks.stonebrick);
        registerOreRecipe(
            FSItemList.PlacerUpgrade.get(),
            "RdR",
            "dDd",
            "RtR",
            'D',
            "drawerFunctionalStorage",
            'R',
            "dustRedstone",
            'd',
            Blocks.dispenser,
            't',
            Blocks.dirt);
        registerOreRecipe(
            FSItemList.RefillUpgrade.get(),
            "RPR",
            "PDP",
            "RCR",
            'C',
            "chestWood",
            'D',
            "drawerFunctionalStorage",
            'P',
            Items.ender_pearl,
            'R',
            "dustRedstone");
        registerOreRecipe(
            FSItemList.DimensionalRefillUpgrade.get(),
            " W ",
            " U ",
            " P ",
            'P',
            Items.ender_eye,
            'U',
            FSItemList.RefillUpgrade.get(),
            'W',
            new ItemStack(Items.skull, 1, 1));
        registerOreRecipe(
            FSItemList.SpeedUpgradeAugment.get(),
            "RBR",
            "BDB",
            "RBR",
            'B',
            Items.blaze_powder,
            'D',
            "drawerFunctionalStorage",
            'R',
            "dustRedstone");
    }

    private static void registerGenerationRecipes() {
        registerOreRecipe(
            FSItemList.DrippingUpgrade.get(),
            "IBI",
            "IDI",
            "IRI",
            'B',
            Blocks.netherrack,
            'D',
            Items.cauldron,
            'I',
            Blocks.stone,
            'R',
            Items.lava_bucket);
        registerShapeless(
            FSItemList.ObsidianUpgrade.get(),
            FSItemList.DrippingUpgrade.get(),
            FSItemList.DrippingUpgrade.get(),
            FSItemList.DrippingUpgrade.get(),
            FSItemList.DrippingUpgrade.get(),
            FSItemList.WaterGenerationUpgrade1.get());
        registerGenerationTier(RegistrationHandler.waterGenerationUpgrades, new ItemStack(Items.water_bucket));
        registerGenerationTier(RegistrationHandler.stoneGenerationUpgrades, new ItemStack(Blocks.cobblestone));
    }

    private static void registerGenerationTier(List<GenerationUpgradeItem> upgrades, ItemStack resource) {
        ItemStack core = new ItemStack(Blocks.chest);
        for (GenerationUpgradeItem upgrade : upgrades) {
            GameRegistry
                .addRecipe(new ShapedOreRecipe(new ItemStack(upgrade), "RRR", "RCR", "RRR", 'R', resource, 'C', core));
            core = new ItemStack(upgrade);
        }
    }

    private static void registerToolRecipes() {
        registerOreRecipe(
            FSItemList.ConfigurationTool.get(),
            "PPG",
            "PDG",
            "PEP",
            'D',
            "drawerFunctionalStorage",
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
            "drawerFunctionalStorage",
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

    private static void registerShapeless(ItemStack output, Object... ingredients) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(output, ingredients));
    }

    private static Object oreOr(String preferred, Object fallback) {
        return OreDictionary.getOres(preferred)
            .isEmpty() ? fallback : preferred;
    }

    private static Object[] drawerRecipe(int slots, Object center, Object frame) {
        String[] shape = slots == 1 ? new String[] { "PPP", "PCP", "PPP" }
            : slots == 2 ? new String[] { "PCP", "PPP", "PCP" } : new String[] { "CPC", "PPP", "CPC" };
        return new Object[] { shape[0], shape[1], shape[2], 'C', center, 'P', frame };
    }
}
