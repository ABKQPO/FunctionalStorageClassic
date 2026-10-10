package com.hfstudio.functionalstorage.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.api.storage.WoodTypeRegistry;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeModifier;
import com.hfstudio.functionalstorage.common.FSItemList;
import com.hfstudio.functionalstorage.common.block.ArmoryCabinetBlock;
import com.hfstudio.functionalstorage.common.block.DrawerBlockProperties;
import com.hfstudio.functionalstorage.common.block.EnderDrawerBlock;
import com.hfstudio.functionalstorage.common.block.EssentiaDrawerBlock;
import com.hfstudio.functionalstorage.common.block.FluidDrawerBlock;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.block.FramedVariantBlock;
import com.hfstudio.functionalstorage.common.block.WoodDrawerBlock;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.block.compact.CompactingDrawerBlock;
import com.hfstudio.functionalstorage.common.block.compact.SimpleCompactingDrawerBlock;
import com.hfstudio.functionalstorage.common.block.controller.ControllerExtensionBlock;
import com.hfstudio.functionalstorage.common.block.controller.DrawerControllerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem;
import com.hfstudio.functionalstorage.common.item.DrawerBlockItem;
import com.hfstudio.functionalstorage.common.item.EnderDrawerBlockItem;
import com.hfstudio.functionalstorage.common.item.FluidDrawerBlockItem;
import com.hfstudio.functionalstorage.common.item.FramedDrawerBlockItem;
import com.hfstudio.functionalstorage.common.item.FramedFluidDrawerBlockItem;
import com.hfstudio.functionalstorage.common.item.LinkingToolItem;
import com.hfstudio.functionalstorage.common.item.upgrade.BreakerUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.CollectorUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.CreativeVendingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem.GenerationKind;
import com.hfstudio.functionalstorage.common.item.upgrade.MaxStorageUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PlacerUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PullingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PushingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RefillUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.ResourceGenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.StorageUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.StorageUpgradeItem.StorageTier;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeSettings;
import com.hfstudio.functionalstorage.common.item.upgrade.VoidUpgradeItem;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shared registered instances used by recipes, integrations, and the creative tab. */
public class RegistrationHandler {

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(FunctionalStorage.MOD_ID) {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return Item.getItemFromBlock(storageController);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void displayAllReleventItems(List<ItemStack> items) {
            for (WoodDrawerBlock block : woodDrawers) {
                block.getSubBlocks(Item.getItemFromBlock(block), CREATIVE_TAB, items);
            }
            super.displayAllReleventItems(items);
        }
    };

    public static final List<WoodDrawerBlock> woodDrawers = new ArrayList<>();
    public static final List<FluidDrawerBlock> fluidDrawers = new ArrayList<>();
    public static final List<EssentiaDrawerBlock> essentiaDrawers = new ArrayList<>();
    public static final List<FramedDrawerBlock> framedDrawers = new ArrayList<>();
    public static final List<DrawerBlock> specialDrawers = new ArrayList<>();

    public static DrawerBlock storageController;
    public static DrawerBlock controllerExtension;
    public static DrawerBlock compactingDrawer;
    public static DrawerBlock simpleCompactingDrawer;
    public static DrawerBlock enderDrawer;
    public static DrawerBlock armoryCabinet;

    public static UpgradeItem blankUpgrade;
    public static StorageUpgradeItem ironDowngrade;
    public static StorageUpgradeItem copperUpgrade;
    public static StorageUpgradeItem goldUpgrade;
    public static StorageUpgradeItem diamondUpgrade;
    public static StorageUpgradeItem netheriteUpgrade;
    public static MaxStorageUpgradeItem maxStorageUpgrade;
    public static VoidUpgradeItem voidUpgrade;
    public static RedstoneUpgradeItem redstoneUpgrade;
    public static PullingUpgradeItem pullingUpgrade;
    public static PushingUpgradeItem pushingUpgrade;
    public static CollectorUpgradeItem collectorUpgrade;
    public static PullingUpgradeItem wirelessPullingUpgrade;
    public static PushingUpgradeItem wirelessPushingUpgrade;

    public static final List<GenerationUpgradeItem> waterGenerationUpgrades = new ArrayList<>();
    public static final List<GenerationUpgradeItem> stoneGenerationUpgrades = new ArrayList<>();
    public static final List<GenerationUpgradeItem> universalGenerationUpgrades = new ArrayList<>();

    public static BreakerUpgradeItem breakerUpgrade;
    public static PlacerUpgradeItem placerUpgrade;
    public static RefillUpgradeItem refillUpgrade;
    public static RefillUpgradeItem dimensionalRefillUpgrade;
    public static UpgradeItem speedUpgradeAugment;
    public static UpgradeItem efficiencyUpgradeAugment;
    public static CreativeVendingUpgradeItem creativeVendingUpgrade;
    public static ResourceGenerationUpgradeItem drippingUpgrade;
    public static ResourceGenerationUpgradeItem obsidianUpgrade;
    public static StorageUpgradeItem turtleScuteUpgrade;
    public static StorageUpgradeItem netherStarUpgrade;
    public static StorageUpgradeItem heavyCoreUpgrade;

    public static final Map<String, List<ResourceGenerationUpgradeItem>> ITEM_GENERATION_UPGRADE_GROUPS = new LinkedHashMap<>();
    public static final Map<String, List<ResourceGenerationUpgradeItem>> FLUID_GENERATION_UPGRADE_GROUPS = new LinkedHashMap<>();
    public static final Map<String, List<GenerationUpgradeItem>> GENERATION_UPGRADE_GROUPS = new LinkedHashMap<>();

    public static ConfigurationToolItem configurationTool;
    public static LinkingToolItem linkingTool;

    public static void registerBlocks() {
        for (WoodDrawerBlock block : woodDrawerBlocks()) {
            woodDrawers.add(block);
            GameRegistry.registerBlock(block, DrawerBlockItem.class, block.getDrawerId());
            registerFsItemList(block.getDrawerId(), block);
        }
        for (FluidDrawerBlock block : fluidDrawerBlocks()) {
            fluidDrawers.add(block);
            String name = "fluid_" + block.getDrawerLayout()
                .getSlotCount();
            GameRegistry.registerBlock(block, FluidDrawerBlockItem.class, name);
            registerFsItemList(name, block);
        }

        compactingDrawer = new CompactingDrawerBlock();
        GameRegistry.registerBlock(compactingDrawer, DrawerBlockItem.class, "compacting_drawer");
        specialDrawers.add(compactingDrawer);
        FSItemList.CompactingDrawer.set(compactingDrawer);

        simpleCompactingDrawer = new SimpleCompactingDrawerBlock();
        GameRegistry.registerBlock(simpleCompactingDrawer, DrawerBlockItem.class, "simple_compacting_drawer");
        specialDrawers.add(simpleCompactingDrawer);
        FSItemList.SimpleCompactingDrawer.set(simpleCompactingDrawer);

        enderDrawer = new EnderDrawerBlock();
        GameRegistry.registerBlock(enderDrawer, EnderDrawerBlockItem.class, "ender_drawer");
        specialDrawers.add(enderDrawer);
        FSItemList.EnderDrawer.set(enderDrawer);

        armoryCabinet = new ArmoryCabinetBlock();
        GameRegistry.registerBlock(armoryCabinet, DrawerBlockItem.class, "armory_cabinet");
        specialDrawers.add(armoryCabinet);
        FSItemList.ArmoryCabinet.set(armoryCabinet);

        storageController = new DrawerControllerBlock();
        GameRegistry.registerBlock(storageController, DrawerBlockItem.class, "storage_controller");
        specialDrawers.add(storageController);
        FSItemList.StorageController.set(storageController);

        controllerExtension = new ControllerExtensionBlock();
        GameRegistry.registerBlock(controllerExtension, DrawerBlockItem.class, "controller_extension");
        specialDrawers.add(controllerExtension);
        FSItemList.ControllerExtension.set(controllerExtension);

        for (DrawerLayout layout : DrawerLayout.values()) {
            FramedDrawerBlock block = new FramedDrawerBlock(layout);
            framedDrawers.add(block);
            GameRegistry.registerBlock(block, FramedDrawerBlockItem.class, block.getDrawerId());
            registerFsItemList(block.getDrawerId(), block);
        }

        registerFramed(
            "compacting_framed_drawer",
            FSItemList.CompactingFramedDrawer,
            compactingDrawer,
            FunctionalStorage.MOD_ID + ":framed_front_compacting");
        registerFramed(
            "framed_simple_compacting_drawer",
            FSItemList.FramedSimpleCompactingDrawer,
            simpleCompactingDrawer,
            FunctionalStorage.MOD_ID + ":framed_side");
        registerFramed(
            "framed_storage_controller",
            FSItemList.FramedStorageController,
            storageController,
            FunctionalStorage.MOD_ID + ":framed_controller_front");
        registerFramed(
            "framed_controller_extension",
            FSItemList.FramedControllerExtension,
            controllerExtension,
            FunctionalStorage.MOD_ID + ":framed_controller_extension");
        registerFramed(
            "framed_fluid_1",
            FSItemList.FramedFluidDrawer1,
            fluidDrawers.get(0),
            FunctionalStorage.MOD_ID + ":framed_side");
        registerFramed(
            "framed_fluid_2",
            FSItemList.FramedFluidDrawer2,
            fluidDrawers.get(1),
            FunctionalStorage.MOD_ID + ":framed_side");
        registerFramed(
            "framed_fluid_4",
            FSItemList.FramedFluidDrawer4,
            fluidDrawers.get(2),
            FunctionalStorage.MOD_ID + ":framed_side");

        if (FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Mods.Thaumcraft.isModLoaded()) {
            for (DrawerLayout layout : DrawerLayout.values()) {
                EssentiaDrawerBlock block = new EssentiaDrawerBlock(layout);
                essentiaDrawers.add(block);
                String name = "essentia_" + layout.getSlotCount();
                GameRegistry.registerBlock(block, DrawerBlockItem.class, name);
                registerFsItemList(name, block);
            }
        }
    }

    private static void registerFsItemList(String name, DrawerBlock block) {
        FSItemList.register(name)
            .set(block);
    }

    private static void registerFramed(String id, FSItemList entry, DrawerBlock original, String defaultTexture) {
        FramedVariantBlock block = new FramedVariantBlock(id, original, defaultTexture);
        Class<? extends DrawerBlockItem> itemClass = original instanceof FluidDrawerBlock
            ? FramedFluidDrawerBlockItem.class
            : FramedDrawerBlockItem.class;
        GameRegistry.registerBlock(block, itemClass, id);
        specialDrawers.add(block);
        entry.set(block);
    }

    private static List<WoodDrawerBlock> woodDrawerBlocks() {
        List<WoodDrawerBlock> blocks = new ArrayList<>();
        for (IWoodType wood : WoodTypeRegistry.available()) {
            for (DrawerLayout layout : DrawerLayout.values()) {
                blocks.add(new WoodDrawerBlock(wood, layout));
            }
        }
        return blocks;
    }

    private static List<FluidDrawerBlock> fluidDrawerBlocks() {
        List<FluidDrawerBlock> blocks = new ArrayList<>();
        for (DrawerLayout layout : DrawerLayout.values()) {
            blocks.add(new FluidDrawerBlock(layout));
        }
        return blocks;
    }

    public static void registerItems() {
        blankUpgrade = registerUpgrade(new UpgradeItem("base"), "base");
        ironDowngrade = registerStorageUpgrade(
            new StorageTier(
                "iron_downgrade",
                Integer.MIN_VALUE,
                () -> 1,
                builder -> builder.addFeature(StorageFeature.IRON_DOWNGRADE)));
        copperUpgrade = registerCapacityUpgrade(
            "copper_upgrade",
            0,
            () -> FunctionalStorageConfig.STORAGE.copperMultiplier,
            true,
            false);
        goldUpgrade = registerCapacityUpgrade(
            "gold_upgrade",
            1,
            () -> FunctionalStorageConfig.STORAGE.goldMultiplier,
            true,
            false);
        diamondUpgrade = registerCapacityUpgrade(
            "diamond_upgrade",
            2,
            () -> FunctionalStorageConfig.STORAGE.diamondMultiplier,
            true,
            false);
        netheriteUpgrade = registerCapacityUpgrade(
            "netherite_upgrade",
            3,
            () -> FunctionalStorageConfig.STORAGE.netheriteMultiplier,
            true,
            false);
        maxStorageUpgrade = registerUpgrade(new MaxStorageUpgradeItem(), "max_storage_upgrade");
        creativeVendingUpgrade = registerUpgrade(new CreativeVendingUpgradeItem(), "creative_vending_upgrade");
        turtleScuteUpgrade = registerCapacityUpgrade(
            "turtle_scute_upgrade",
            4,
            () -> FunctionalStorageConfig.STORAGE.turtleScuteMultiplier,
            true,
            true);
        netherStarUpgrade = registerCapacityUpgrade(
            "nether_star_upgrade",
            5,
            () -> FunctionalStorageConfig.STORAGE.netherStarMultiplier,
            true,
            true);
        heavyCoreUpgrade = registerCapacityUpgrade(
            "heavy_core_upgrade",
            6,
            () -> FunctionalStorageConfig.STORAGE.heavyCoreMultiplier,
            true,
            true);

        voidUpgrade = registerUpgrade(new VoidUpgradeItem(), "void_upgrade");
        redstoneUpgrade = registerUpgrade(new RedstoneUpgradeItem(), "redstone_upgrade");
        pullingUpgrade = registerUpgrade(new PullingUpgradeItem(false), "pulling_upgrade");
        pushingUpgrade = registerUpgrade(new PushingUpgradeItem(false), "pushing_upgrade");
        collectorUpgrade = registerUpgrade(new CollectorUpgradeItem(), "collector_upgrade");
        wirelessPullingUpgrade = registerUpgrade(new PullingUpgradeItem(true), "wireless_pulling_upgrade");
        wirelessPushingUpgrade = registerUpgrade(new PushingUpgradeItem(true), "wireless_pushing_upgrade");

        registerGenerationUpgrades();
        registerResourceGenerationUpgrades();

        breakerUpgrade = registerUpgrade(new BreakerUpgradeItem(), "breaker_upgrade");
        placerUpgrade = registerUpgrade(new PlacerUpgradeItem(), "placer_upgrade");
        refillUpgrade = registerUpgrade(new RefillUpgradeItem(false), "refill_upgrade");
        dimensionalRefillUpgrade = registerUpgrade(new RefillUpgradeItem(true), "dimensional_refill_upgrade");
        speedUpgradeAugment = registerUpgrade(new UpgradeItem("speed_upgrade_augment"), "speed_upgrade_augment");
        efficiencyUpgradeAugment = registerUpgrade(
            new UpgradeItem("efficiency_upgrade_augment"),
            "efficiency_upgrade_augment");

        configurationTool = new ConfigurationToolItem();
        GameRegistry.registerItem(configurationTool, "configuration_tool");
        FSItemList.ConfigurationTool.set(configurationTool);

        linkingTool = new LinkingToolItem();
        GameRegistry.registerItem(linkingTool, "linking_tool");
        FSItemList.LinkingTool.set(linkingTool);
        MissingMappingsHandler.registerLegacyItems();
    }

    public static void registerTileEntities() {
        for (WoodDrawerBlock block : woodDrawers) {
            GameRegistry
                .registerTileEntity(block.getTileEntityClass(), FunctionalStorage.MOD_ID + "." + block.getDrawerId());
        }
        for (FluidDrawerBlock block : fluidDrawers) {
            GameRegistry.registerTileEntity(
                block.getTileEntityClass(),
                FunctionalStorage.MOD_ID + ".fluid_"
                    + block.getDrawerLayout()
                        .getSlotCount());
        }
        for (DrawerBlock block : specialDrawers) {
            if (block instanceof FramedVariantBlock) continue;
            String name = block.getVariantNames()
                .get(0);
            GameRegistry.registerTileEntity(block.getTileEntityClass(), FunctionalStorage.MOD_ID + "." + name);
        }
        for (FramedDrawerBlock block : framedDrawers) {
            GameRegistry
                .registerTileEntity(block.getTileEntityClass(), FunctionalStorage.MOD_ID + "." + block.getDrawerId());
        }
        for (EssentiaDrawerBlock block : essentiaDrawers) {
            GameRegistry.registerTileEntity(
                block.getTileEntityClass(),
                FunctionalStorage.MOD_ID + ".essentia_"
                    + block.getDrawerLayout()
                        .getSlotCount());
        }
    }

    public static void registerBlockProperties() {
        DrawerBlockProperties.register();
    }

    private static void registerGenerationUpgrades() {
        IntSupplier[] intervals = new IntSupplier[] { () -> FunctionalStorageConfig.UPGRADES.waterGenerationInterval,
            () -> FunctionalStorageConfig.UPGRADES.waterGenerationInterval,
            () -> FunctionalStorageConfig.UPGRADES.waterGenerationInterval,
            () -> FunctionalStorageConfig.UPGRADES.waterGenerationInterval,
            () -> FunctionalStorageConfig.UPGRADES.waterGenerationInterval };
        waterGenerationUpgrades.addAll(
            registerGenerationKind(
                new GenerationKind(
                    "water_generation_upgrade",
                    stack -> null,
                    new FluidStack(FluidRegistry.WATER, 1),
                    new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.waterGenerationTier1,
                        () -> FunctionalStorageConfig.UPGRADES.waterGenerationTier2,
                        () -> FunctionalStorageConfig.UPGRADES.waterGenerationTier3,
                        () -> FunctionalStorageConfig.UPGRADES.waterGenerationTier4,
                        () -> FunctionalStorageConfig.UPGRADES.waterGenerationTier5 },
                    intervals)));
        ItemStack cobblestone = new ItemStack(Blocks.cobblestone);
        stoneGenerationUpgrades.addAll(
            registerGenerationKind(
                new GenerationKind(
                    "stone_generation_upgrade",
                    stack -> cobblestone,
                    null,
                    new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier1,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier2,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier3,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier4,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationTier5 },
                    new IntSupplier[] { () -> FunctionalStorageConfig.UPGRADES.stoneGenerationInterval,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationInterval,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationInterval,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationInterval,
                        () -> FunctionalStorageConfig.UPGRADES.stoneGenerationInterval })));
        universalGenerationUpgrades.addAll(
            registerGenerationKind(
                new GenerationKind(
                    "universal_item_generation",
                    RegistrationHandler::resolveUniversalItem,
                    null,
                    new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.universalGenerationTier1,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTier2,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTier3,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTier4,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTier5 },
                    new IntSupplier[] { () -> FunctionalStorageConfig.UPGRADES.universalGenerationTick,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTick,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTick,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTick,
                        () -> FunctionalStorageConfig.UPGRADES.universalGenerationTick })));
    }

    public static List<GenerationUpgradeItem> registerGenerationKind(GenerationKind kind) {
        if (GENERATION_UPGRADE_GROUPS.containsKey(kind.getIdPrefix())) {
            throw new IllegalArgumentException("Generator already registered: " + kind.getIdPrefix());
        }
        List<GenerationUpgradeItem> upgrades = new ArrayList<>();
        for (int tier = 1; tier <= kind.getTierCount(); tier++) {
            upgrades.add(
                registerUpgrade(new GenerationUpgradeItem(kind, tier), GenerationUpgradeItem.registryName(kind, tier)));
        }
        List<GenerationUpgradeItem> registered = Collections.unmodifiableList(upgrades);
        GENERATION_UPGRADE_GROUPS.put(kind.getIdPrefix(), registered);
        return registered;
    }

    public static Map<String, List<GenerationUpgradeItem>> getGenerationUpgradeGroups() {
        return Collections.unmodifiableMap(GENERATION_UPGRADE_GROUPS);
    }

    public static StorageUpgradeItem registerStorageUpgrade(StorageTier tier) {
        return registerUpgrade(new StorageUpgradeItem(tier), tier.getId());
    }

    private static StorageUpgradeItem registerCapacityUpgrade(String id, int priority, IntSupplier multiplier,
        boolean aspects, boolean multiplyRange) {
        return registerStorageUpgrade(new StorageTier(id, priority, multiplier, builder -> {
            double amount = Math.max(1, multiplier.getAsInt());
            builder.addModifier(UpgradeAttribute.ITEM_CAPACITY, UpgradeModifier.multiply(amount))
                .addModifier(
                    UpgradeAttribute.FLUID_CAPACITY,
                    UpgradeModifier
                        .multiply(Math.max(1D, amount / Math.max(1, FunctionalStorageConfig.STORAGE.fluidDivisor))));
            if (aspects) {
                builder.addModifier(
                    UpgradeAttribute.ASPECT_CAPACITY,
                    UpgradeModifier
                        .multiply(Math.max(1D, amount / Math.max(1, FunctionalStorageConfig.STORAGE.aspectDivisor))));
            }
            double range = amount / Math.max(1, FunctionalStorageConfig.STORAGE.rangeDivisor);
            builder.addModifier(
                UpgradeAttribute.CONTROLLER_RANGE,
                multiplyRange ? UpgradeModifier.multiply(Math.max(1D, range)) : UpgradeModifier.addBase(range));
        }));
    }

    private static ItemStack resolveUniversalItem(ItemStack stack) {
        ItemStack filter = stack == null ? null : UpgradeSettings.getFilter(stack, 0);
        if (filter != null) return filter;
        if (!FunctionalStorageConfig.UPGRADES.universalGenerationRegistered) return null;
        String description = FunctionalStorageConfig.UPGRADES.universalGenerationItem;
        Item item = ItemUtil.itemFromDescription(description);
        return item == null ? null : new ItemStack(item, 1, ItemUtil.metadataFromDescription(description));
    }

    private static void registerResourceGenerationUpgrades() {
        registerItemGenerationGroup(
            "obsidian",
            new ItemStack(Blocks.obsidian),
            obsidianGenerationRates(),
            obsidianGenerationIntervals(),
            tier -> "obsidian_upgrade_" + tier);
        obsidianUpgrade = ITEM_GENERATION_UPGRADE_GROUPS.get("obsidian")
            .get(0);
        registerItemGenerationGroup(
            "gravel",
            new ItemStack(Blocks.gravel),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "dirt",
            new ItemStack(Blocks.dirt),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "sand",
            new ItemStack(Blocks.sand),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "netherrack",
            new ItemStack(Blocks.netherrack),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "end_stone",
            new ItemStack(Blocks.end_stone),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "soul_sand",
            new ItemStack(Blocks.soul_sand),
            resourceGenerationRates(),
            resourceGenerationIntervals());
        registerItemGenerationGroup(
            "clay",
            new ItemStack(Blocks.clay),
            resourceGenerationRates(),
            resourceGenerationIntervals());

        registerOptionalItemGenerationGroup("cobbled_deepslate", "cobbled_deepslate", 0);
        registerOptionalItemGenerationGroup("andesite", "stone", 5);
        registerOptionalItemGenerationGroup("diorite", "stone", 3);
        registerOptionalItemGenerationGroup("granite", "stone", 1);
        registerOptionalItemGenerationGroup("blackstone", "blackstone", 0);
        registerOptionalItemGenerationGroup("tuff", "tuff", 0);
        registerOptionalItemGenerationGroup("basalt", "basalt", 0);
        registerOptionalItemGenerationGroup("dripstone", "dripstone_block", 0);
        registerOptionalItemGenerationGroup("prismarine", "prismarine_block", 0);
        registerOptionalItemGenerationGroup("sculk", "sculk", 0);

        registerFluidGenerationGroup(
            "lava",
            FluidRegistry.getFluid("lava"),
            lavaGenerationRates(),
            tier -> "dripping_upgrade_" + tier);
        drippingUpgrade = FLUID_GENERATION_UPGRADE_GROUPS.get("lava")
            .get(0);
    }

    public static void registerOptionalFluidGenerationUpgrades() {
        registerFluidGenerationGroup("milk", FluidRegistry.getFluid("milk"), milkGenerationRates());
        registerFluidGenerationGroup("honey", FluidRegistry.getFluid("honey"), honeyGenerationRates());
    }

    private static void registerOptionalItemGenerationGroup(String idPrefix, String blockName, int metadata) {
        if (!Mods.Etfuturum.isModLoaded()) return;
        Block block = GameRegistry.findBlock(Mods.Etfuturum.modid, blockName);
        if (block == null) return;
        registerItemGenerationGroup(
            idPrefix,
            new ItemStack(block, 1, metadata),
            resourceGenerationRates(),
            resourceGenerationIntervals());
    }

    private static void registerItemGenerationGroup(String idPrefix, ItemStack resource, LongSupplier[] rates,
        IntSupplier[] intervals) {
        registerItemGenerationGroup(
            idPrefix,
            resource,
            rates,
            intervals,
            tier -> idPrefix + "_generator_upgrade_" + tier);
    }

    private static void registerItemGenerationGroup(String idPrefix, ItemStack resource, LongSupplier[] rates,
        IntSupplier[] intervals, IntFunction<String> registryNames) {
        if (resource == null || resource.getItem() == null || rates.length != intervals.length) return;
        ItemStack template = resource.copy();
        template.stackSize = 1;
        List<ResourceGenerationUpgradeItem> upgrades = new ArrayList<>(
            registerGenerationKind(
                new GenerationKind(idPrefix, stack -> template, null, rates, intervals, registryNames)));
        ITEM_GENERATION_UPGRADE_GROUPS.put(idPrefix, Collections.unmodifiableList(upgrades));
    }

    private static void registerFluidGenerationGroup(String idPrefix, Fluid fluid, LongSupplier[] rates) {
        registerFluidGenerationGroup(idPrefix, fluid, rates, tier -> idPrefix + "_generator_upgrade_" + tier);
    }

    private static void registerFluidGenerationGroup(String idPrefix, Fluid fluid, LongSupplier[] rates,
        IntFunction<String> registryNames) {
        if (fluid == null) return;
        List<ResourceGenerationUpgradeItem> upgrades = new ArrayList<>(
            registerGenerationKind(
                new GenerationKind(
                    idPrefix,
                    stack -> null,
                    new FluidStack(fluid, 1),
                    rates,
                    resourceGenerationIntervals(),
                    registryNames)));
        FLUID_GENERATION_UPGRADE_GROUPS.put(idPrefix, Collections.unmodifiableList(upgrades));
    }

    private static LongSupplier[] resourceGenerationRates() {
        return new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.resourceGenerationRateTier1,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationRateTier2,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationRateTier3,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationRateTier4,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationRateTier5 };
    }

    private static IntSupplier[] resourceGenerationIntervals() {
        return new IntSupplier[] { () -> FunctionalStorageConfig.UPGRADES.resourceGenerationIntervalTier1,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationIntervalTier2,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationIntervalTier3,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationIntervalTier4,
            () -> FunctionalStorageConfig.UPGRADES.resourceGenerationIntervalTier5 };
    }

    private static IntSupplier[] obsidianGenerationIntervals() {
        return new IntSupplier[] { () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationIntervalTier1,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationIntervalTier2,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationIntervalTier3,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationIntervalTier4,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationIntervalTier5 };
    }

    private static LongSupplier[] obsidianGenerationRates() {
        return new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationRateTier1,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationRateTier2,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationRateTier3,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationRateTier4,
            () -> FunctionalStorageConfig.UPGRADES.obsidianGenerationRateTier5 };
    }

    private static LongSupplier[] lavaGenerationRates() {
        return new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.lavaGenerationRateTier1,
            () -> FunctionalStorageConfig.UPGRADES.lavaGenerationRateTier2,
            () -> FunctionalStorageConfig.UPGRADES.lavaGenerationRateTier3,
            () -> FunctionalStorageConfig.UPGRADES.lavaGenerationRateTier4,
            () -> FunctionalStorageConfig.UPGRADES.lavaGenerationRateTier5 };
    }

    private static LongSupplier[] milkGenerationRates() {
        return new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.milkGenerationRateTier1,
            () -> FunctionalStorageConfig.UPGRADES.milkGenerationRateTier2,
            () -> FunctionalStorageConfig.UPGRADES.milkGenerationRateTier3,
            () -> FunctionalStorageConfig.UPGRADES.milkGenerationRateTier4,
            () -> FunctionalStorageConfig.UPGRADES.milkGenerationRateTier5 };
    }

    private static LongSupplier[] honeyGenerationRates() {
        return new LongSupplier[] { () -> FunctionalStorageConfig.UPGRADES.honeyGenerationRateTier1,
            () -> FunctionalStorageConfig.UPGRADES.honeyGenerationRateTier2,
            () -> FunctionalStorageConfig.UPGRADES.honeyGenerationRateTier3,
            () -> FunctionalStorageConfig.UPGRADES.honeyGenerationRateTier4,
            () -> FunctionalStorageConfig.UPGRADES.honeyGenerationRateTier5 };
    }

    @Nonnull
    public static Map<String, List<ResourceGenerationUpgradeItem>> getItemGenerationUpgradeGroups() {
        return Collections.unmodifiableMap(ITEM_GENERATION_UPGRADE_GROUPS);
    }

    @Nonnull
    public static Map<String, List<ResourceGenerationUpgradeItem>> getFluidGenerationUpgradeGroups() {
        return Collections.unmodifiableMap(FLUID_GENERATION_UPGRADE_GROUPS);
    }

    @Nonnull
    public static List<DrawerBlock> allDrawerBlocks() {
        List<DrawerBlock> blocks = new ArrayList<>();
        blocks.addAll(woodDrawers);
        blocks.addAll(fluidDrawers);
        blocks.addAll(framedDrawers);
        blocks.addAll(essentiaDrawers);
        blocks.addAll(specialDrawers);
        return blocks;
    }

    public static <T extends UpgradeItem> T registerUpgrade(T item, String name) {
        item.setUpgradeName(name);
        GameRegistry.registerItem(item, name);
        FSItemList.register(name)
            .set(item);
        return item;
    }
}
