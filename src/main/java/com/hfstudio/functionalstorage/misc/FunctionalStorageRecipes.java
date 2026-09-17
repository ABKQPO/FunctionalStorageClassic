package com.hfstudio.functionalstorage.misc;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.api.storage.WoodTypeRegistry;
import com.hfstudio.functionalstorage.common.FSItemList;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.block.WoodDrawerBlock;
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
            String name = wood.getName();
            for (int slots : new int[] { 1, 2, 4 }) {
                FSItemList drawer = FSItemList.byId(name + "_" + slots);
                if (registerDrawer(drawer, slots, planks)) {
                    registerFrameConversion(drawer, slots);
                }
            }
        }
    }

    private static boolean registerDrawer(FSItemList drawer, int slots, ItemStack planks) {
        if (drawer == null || !drawer.isRegistered()) {
            return false;
        }
        GameRegistry
            .addRecipe(new DrawerCraftingRecipe(drawer.get(slots), false, drawerRecipe(slots, "chestWood", planks)));
        return true;
    }

    private static void registerFluidDrawerRecipes() {
        registerOreRecipe(FSItemList.FluidDrawer1, 1, "PCP", 'C', oreOr("bucket", Items.bucket), 'P', "plankWood");
        registerOreRecipe(FSItemList.FluidDrawer2, 2, "PCP", 'C', oreOr("bucket", Items.bucket), 'P', "plankWood");
        registerOreRecipe(FSItemList.FluidDrawer4, 4, "PCP", 'C', oreOr("bucket", Items.bucket), 'P', "plankWood");
    }

    private static void registerFramedRecipes() {
        Object bucket = oreOr("bucket", Items.bucket);
        registerOreRecipe(FSItemList.FramedDrawer1, 1, "PCP", 'C', "chestWood", 'P', ORE_IRON_NUGGET);
        registerOreRecipe(FSItemList.FramedDrawer2, 2, "PCP", 'C', "chestWood", 'P', ORE_IRON_NUGGET);
        registerOreRecipe(FSItemList.FramedDrawer4, 4, "PCP", 'C', "chestWood", 'P', ORE_IRON_NUGGET);
        registerOreRecipe(FSItemList.FramedFluidDrawer1, 1, "PCP", 'C', bucket, 'P', ORE_IRON_NUGGET);
        registerOreRecipe(FSItemList.FramedFluidDrawer2, 2, "PCP", 'C', bucket, 'P', ORE_IRON_NUGGET);
        registerOreRecipe(FSItemList.FramedFluidDrawer4, 4, "PCP", 'C', bucket, 'P', ORE_IRON_NUGGET);
    }

    private static void registerEssentiaRecipes() {
        Object bottle = oreOr("bottleGlass", Items.glass_bottle);
        for (var block : RegistrationHandler.essentiaDrawers) {
            int count = block.getFaceLayout()
                .getSlotCount();
            GameRegistry.addRecipe(
                new DrawerCraftingRecipe(new ItemStack(block, count), false, drawerRecipe(count, "plankWood", bottle)));
        }
    }

    private static void registerMachineRecipes() {
        registerShapeless(FSItemList.NetheriteUpgrade, 1, FSItemList.DiamondUpgrade, ORE_NETHERITE_INGOT);
        registerOreRecipe(
            FSItemList.CompactingDrawer,
            1,
            "SSS",
            "PDP",
            "SIS",
            'D',
            "drawerFunctionalStorage",
            'I',
            oreOr("ingotIron", Items.iron_ingot),
            'P',
            Blocks.piston,
            'S',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.SimpleCompactingDrawer,
            1,
            "SSS",
            "SDP",
            "SIS",
            'D',
            "drawerFunctionalStorage",
            'I',
            oreOr("ingotIron", Items.iron_ingot),
            'P',
            Blocks.piston,
            'S',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.CompactingFramedDrawer,
            1,
            "SSS",
            "PDP",
            "SIS",
            'D',
            "drawerFunctionalStorage",
            'I',
            oreOr("ingotIron", Items.iron_ingot),
            'P',
            Blocks.piston,
            'S',
            ORE_IRON_NUGGET);
        registerOreRecipe(
            FSItemList.FramedSimpleCompactingDrawer,
            1,
            "SSS",
            "SDP",
            "SIS",
            'D',
            "drawerFunctionalStorage",
            'I',
            oreOr("ingotIron", Items.iron_ingot),
            'P',
            Blocks.piston,
            'S',
            ORE_IRON_NUGGET);
        registerOreRecipe(
            FSItemList.EnderDrawer,
            1,
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
            FSItemList.ArmoryCabinet,
            1,
            "ICI",
            "CDC",
            "IBI",
            'B',
            ORE_NETHERITE_INGOT,
            'C',
            "drawerFunctionalStorage",
            'D',
            Items.comparator,
            'I',
            Blocks.stone);
        registerControllerRecipe(FSItemList.StorageController, Items.comparator, Blocks.stone);
        registerControllerRecipe(FSItemList.ControllerExtension, Items.repeater, Blocks.stone);
        registerControllerRecipe(FSItemList.FramedStorageController, Items.comparator, ORE_IRON_NUGGET);
        registerControllerRecipe(FSItemList.FramedControllerExtension, Items.repeater, ORE_IRON_NUGGET);
    }

    private static void registerControllerRecipe(FSItemList controller, Object center, Object frame) {
        registerOreRecipe(
            controller,
            1,
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
            FSItemList.IronDowngrade,
            1,
            "III",
            "IDI",
            "III",
            'D',
            "drawerFunctionalStorage",
            'I',
            oreOr("ingotIron", Items.iron_ingot));
        registerOreRecipe(
            FSItemList.CopperUpgrade,
            1,
            "IBI",
            "CDC",
            "IBI",
            'B',
            ORE_COPPER_BLOCK,
            'C',
            "chestWood",
            'D',
            "drawerFunctionalStorage",
            'I',
            ORE_COPPER_INGOT);
        registerOreRecipe(
            FSItemList.GoldUpgrade,
            1,
            "IBI",
            "CDC",
            "BIB",
            'B',
            oreOr("blockGold", Blocks.gold_block),
            'C',
            "chestWood",
            'D',
            FSItemList.CopperUpgrade.get(1),
            'I',
            oreOr("ingotGold", Items.gold_ingot));
        registerOreRecipe(
            FSItemList.DiamondUpgrade,
            1,
            "IBI",
            "CDC",
            "IBI",
            'B',
            oreOr("blockDiamond", Blocks.diamond_block),
            'C',
            "chestWood",
            'D',
            FSItemList.GoldUpgrade.get(1),
            'I',
            oreOr("gemDiamond", Items.diamond));
        registerOreRecipe(
            FSItemList.MaxStorageUpgrade,
            1,
            "IBI",
            "CDC",
            "IBI",
            'B',
            Items.nether_star,
            'C',
            "chestWood",
            'D',
            FSItemList.NetheriteUpgrade.get(1),
            'I',
            oreOr("gemDiamond", Items.diamond));
        registerOreRecipe(
            FSItemList.CreativeVendingUpgrade,
            1,
            "IBI",
            "CDC",
            "IBI",
            'B',
            Blocks.beacon,
            'C',
            "chestWood",
            'D',
            FSItemList.NetheriteUpgrade.get(1),
            'I',
            oreOr("gemDiamond", Items.diamond));
    }

    private static void registerUtilityUpgradeRecipes() {
        registerOreRecipe(
            FSItemList.VoidUpgrade,
            1,
            "III",
            "IDI",
            "III",
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.obsidian);
        registerOreRecipe(
            FSItemList.RedstoneUpgrade,
            1,
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
            oreOr("dustRedstone", Items.redstone));
        registerOreRecipe(
            FSItemList.PullingUpgrade,
            1,
            "ICI",
            "IDI",
            "IBI",
            'B',
            oreOr("dustRedstone", Items.redstone),
            'C',
            Blocks.hopper,
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.stone);
        registerOreRecipe(
            FSItemList.PushingUpgrade,
            1,
            "IBI",
            "IDI",
            "IRI",
            'B',
            oreOr("dustRedstone", Items.redstone),
            'D',
            "drawerFunctionalStorage",
            'I',
            Blocks.stone,
            'R',
            Blocks.hopper);
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PullingUpgrade.get(1), FSItemList.PushingUpgrade.get(1)));
        GameRegistry
            .addRecipe(new UpgradeConversionRecipe(FSItemList.PushingUpgrade.get(1), FSItemList.PullingUpgrade.get(1)));
        registerShapeless(FSItemList.WirelessPullingUpgrade, 1, FSItemList.PullingUpgrade.get(1), Items.ender_pearl);
        registerShapeless(FSItemList.WirelessPushingUpgrade, 1, FSItemList.PushingUpgrade.get(1), Items.ender_pearl);
        registerOreRecipe(
            FSItemList.CollectorUpgrade,
            1,
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
            oreOr("dustRedstone", Items.redstone));
        registerOreRecipe(
            FSItemList.OreDictionaryUpgrade,
            1,
            "W W",
            " C ",
            "W W",
            'W',
            Items.writable_book,
            'C',
            Blocks.chest);
        registerOreRecipe(
            FSItemList.BreakerUpgrade,
            1,
            "RSR",
            "SDS",
            "RPR",
            'D',
            "drawerFunctionalStorage",
            'P',
            Items.iron_pickaxe,
            'R',
            oreOr("dustRedstone", Items.redstone),
            'S',
            Blocks.stonebrick);
        registerOreRecipe(
            FSItemList.PlacerUpgrade,
            1,
            "RdR",
            "dDd",
            "RtR",
            'D',
            "drawerFunctionalStorage",
            'R',
            oreOr("dustRedstone", Items.redstone),
            'd',
            Blocks.dispenser,
            't',
            Blocks.dirt);
        registerOreRecipe(
            FSItemList.RefillUpgrade,
            1,
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
            oreOr("dustRedstone", Items.redstone));
        registerOreRecipe(
            FSItemList.DimensionalRefillUpgrade,
            1,
            " W ",
            " U ",
            " P ",
            'P',
            Items.ender_eye,
            'U',
            FSItemList.RefillUpgrade.get(1),
            'W',
            new ItemStack(Items.skull, 1, 1));
        registerOreRecipe(
            FSItemList.SpeedUpgradeAugment,
            2,
            "RBR",
            "BDB",
            "RBR",
            'B',
            Items.blaze_powder,
            'D',
            "drawerFunctionalStorage",
            'R',
            oreOr("dustRedstone", Items.redstone));
    }

    private static void registerGenerationRecipes() {
        registerOreRecipe(
            FSItemList.DrippingUpgrade,
            1,
            "IBI",
            "IDI",
            "IRI",
            'B',
            Blocks.netherrack,
            'D',
            Blocks.cauldron,
            'I',
            Blocks.stone,
            'R',
            Items.lava_bucket);
        registerOreRecipe(
            FSItemList.WaterGeneratorUpgrade,
            1,
            "IBI",
            "IDI",
            "IBI",
            'B',
            Items.water_bucket,
            'D',
            Items.bucket,
            'I',
            Blocks.stone);
        registerShapeless(
            FSItemList.ObsidianUpgrade,
            1,
            FSItemList.DrippingUpgrade.get(1),
            FSItemList.DrippingUpgrade.get(1),
            FSItemList.DrippingUpgrade.get(1),
            FSItemList.DrippingUpgrade.get(1),
            FSItemList.WaterGeneratorUpgrade.get(1));
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
            FSItemList.ConfigurationTool,
            1,
            "PPG",
            "PDG",
            "PEP",
            'D',
            "drawerFunctionalStorage",
            'E',
            Items.emerald,
            'G',
            oreOr("ingotGold", Items.gold_ingot),
            'P',
            Items.paper);
        registerOreRecipe(
            FSItemList.LinkingTool,
            1,
            "PPG",
            "PDG",
            "PEP",
            'D',
            "drawerFunctionalStorage",
            'E',
            Items.diamond,
            'G',
            oreOr("ingotGold", Items.gold_ingot),
            'P',
            Items.paper);
    }

    private static void registerFrameConversion(FSItemList source, int slots) {
        if (source == null || !source.isRegistered()
            || OreDictionary.getOres(ORE_IRON_NUGGET)
                .isEmpty()) {
            return;
        }
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
                drawerRecipe(slots, source.get(1), ORE_IRON_NUGGET)));
    }

    private static void registerOreRecipe(FSItemList output, int count, Object... recipe) {
        if (output == null || !output.isRegistered()) {
            return;
        }
        Object[] resolved = new Object[recipe.length];
        for (int index = 0; index < recipe.length; index++) {
            Object ingredient = recipe[index];
            if (ingredient == null || missingOre(ingredient)) {
                return;
            }
            resolved[index] = resolve(ingredient);
        }
        GameRegistry.addRecipe(new ShapedOreRecipe(output.get(count), resolved));
    }

    private static void registerShapeless(FSItemList output, int count, Object... ingredients) {
        if (output == null || !output.isRegistered()) {
            return;
        }
        Object[] resolved = new Object[ingredients.length];
        for (int index = 0; index < ingredients.length; index++) {
            Object ingredient = ingredients[index];
            if (ingredient == null || missingOre(ingredient)) {
                return;
            }
            resolved[index] = resolve(ingredient);
        }
        GameRegistry.addRecipe(new ShapelessOreRecipe(output.get(count), resolved));
    }

    private static boolean missingOre(Object ingredient) {
        return ingredient instanceof String ore && OreDictionary.getOres(ore)
            .isEmpty();
    }

    private static Object resolve(Object ingredient) {
        return ingredient instanceof FSItemList entry ? entry.get(1) : ingredient;
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

    private static ItemStack drawer(String woodName, int slots, int count) {
        FSItemList entry = FSItemList.byId(woodName + "_" + slots);
        if (entry != null) {
            return entry.isRegistered() ? entry.get(count) : null;
        }
        return stack(FunctionalStorage.MOD_ID + ":" + woodName + "_" + slots, count, 0);
    }

    private static ItemStack stack(String id, int count, int metadata) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item != null) {
            return new ItemStack(item, count, metadata);
        }
        Block block = Block.getBlockFromName(id);
        return block == null ? null : new ItemStack(block, count, metadata);
    }
}
