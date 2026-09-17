package com.hfstudio.functionalstorage.misc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.FunctionalStorage;
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
import com.hfstudio.functionalstorage.common.integration.ae2.AE2Integration;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem;
import com.hfstudio.functionalstorage.common.item.LinkingToolItem;
import com.hfstudio.functionalstorage.common.item.upgrade.BreakerUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.CollectorUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.CreativeVendingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.GenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.MaxStorageUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.OreDictionaryUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PlacerUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PullingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.PushingUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RefillUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.ResourceGenerationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.StorageUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.VoidUpgradeItem;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.DrawerWoodType;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

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
    public static OreDictionaryUpgradeItem oreDictionaryUpgrade;
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
    public static CreativeVendingUpgradeItem creativeVendingUpgrade;
    public static ResourceGenerationUpgradeItem drippingUpgrade;
    public static ResourceGenerationUpgradeItem waterGeneratorUpgrade;
    public static ResourceGenerationUpgradeItem obsidianUpgrade;

    public static ConfigurationToolItem configurationTool;
    public static LinkingToolItem linkingTool;

    /** Registry names must match the blockstate and model resource paths. */
    public static void registerBlocks() {
        for (WoodDrawerBlock block : woodDrawerBlocks()) {
            woodDrawers.add(block);
            GameRegistry.registerBlock(block, block.getDrawerId());
        }
        for (FluidDrawerBlock block : fluidDrawerBlocks()) {
            fluidDrawers.add(block);
            GameRegistry.registerBlock(
                block,
                "fluid_" + block.getDrawerLayout()
                    .getSlotCount());
        }

        compactingDrawer = new CompactingDrawerBlock();
        GameRegistry.registerBlock(compactingDrawer, "compacting_drawer");
        specialDrawers.add(compactingDrawer);

        simpleCompactingDrawer = new SimpleCompactingDrawerBlock();
        GameRegistry.registerBlock(simpleCompactingDrawer, "simple_compacting_drawer");
        specialDrawers.add(simpleCompactingDrawer);

        enderDrawer = new EnderDrawerBlock();
        GameRegistry.registerBlock(enderDrawer, "ender_drawer");
        specialDrawers.add(enderDrawer);

        armoryCabinet = new ArmoryCabinetBlock();
        GameRegistry.registerBlock(armoryCabinet, "armory_cabinet");
        specialDrawers.add(armoryCabinet);

        storageController = new DrawerControllerBlock();
        GameRegistry.registerBlock(storageController, "storage_controller");
        specialDrawers.add(storageController);

        controllerExtension = new ControllerExtensionBlock();
        GameRegistry.registerBlock(controllerExtension, "controller_extension");
        specialDrawers.add(controllerExtension);

        for (DrawerLayout layout : DrawerLayout.values()) {
            FramedDrawerBlock block = new FramedDrawerBlock(layout);
            framedDrawers.add(block);
            GameRegistry.registerBlock(block, block.getDrawerId());
        }

        registerFramed("compacting_framed_drawer", compactingDrawer);
        registerFramed("framed_simple_compacting_drawer", simpleCompactingDrawer);
        registerFramed("framed_storage_controller", storageController);
        registerFramed("framed_controller_extension", controllerExtension);
        for (FluidDrawerBlock block : fluidDrawers) registerFramed(
            "framed_fluid_" + block.getDrawerLayout()
                .getSlotCount(),
            block);

        if (FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Mods.Thaumcraft.isModLoaded()) {
            for (DrawerLayout layout : DrawerLayout.values()) {
                EssentiaDrawerBlock block = new EssentiaDrawerBlock(layout);
                essentiaDrawers.add(block);
                GameRegistry.registerBlock(block, "essentia_" + layout.getSlotCount());
            }
        }
    }

    private static void registerFramed(String id, DrawerBlock original) {
        FramedVariantBlock block = new FramedVariantBlock(id, original);
        GameRegistry.registerBlock(block, id);
        specialDrawers.add(block);
    }

    private static List<WoodDrawerBlock> woodDrawerBlocks() {
        List<WoodDrawerBlock> blocks = new ArrayList<>();
        for (DrawerWoodType wood : DrawerWoodType.values()) {
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
        ironDowngrade = registerUpgrade(new StorageUpgradeItem(StorageUpgradeItem.StorageTier.IRON), "iron_downgrade");
        copperUpgrade = registerUpgrade(
            new StorageUpgradeItem(StorageUpgradeItem.StorageTier.COPPER),
            "copper_upgrade");
        goldUpgrade = registerUpgrade(new StorageUpgradeItem(StorageUpgradeItem.StorageTier.GOLD), "gold_upgrade");
        diamondUpgrade = registerUpgrade(
            new StorageUpgradeItem(StorageUpgradeItem.StorageTier.DIAMOND),
            "diamond_upgrade");
        netheriteUpgrade = registerUpgrade(
            new StorageUpgradeItem(StorageUpgradeItem.StorageTier.NETHERITE),
            "netherite_upgrade");
        maxStorageUpgrade = registerUpgrade(new MaxStorageUpgradeItem(), "max_storage_upgrade");
        creativeVendingUpgrade = registerUpgrade(new CreativeVendingUpgradeItem(), "creative_vending_upgrade");
        drippingUpgrade = registerUpgrade(
            new ResourceGenerationUpgradeItem("dripping_upgrade", 20, null, new FluidStack(FluidRegistry.LAVA, 20)),
            "dripping_upgrade");
        waterGeneratorUpgrade = registerUpgrade(
            new ResourceGenerationUpgradeItem(
                "water_generator_upgrade",
                1,
                null,
                new FluidStack(FluidRegistry.WATER, 2000)),
            "water_generator_upgrade");
        obsidianUpgrade = registerUpgrade(
            new ResourceGenerationUpgradeItem("obsidian_upgrade", 300, new ItemStack(Blocks.obsidian), null),
            "obsidian_upgrade");

        voidUpgrade = registerUpgrade(new VoidUpgradeItem(), "void_upgrade");
        redstoneUpgrade = registerUpgrade(new RedstoneUpgradeItem(), "redstone_upgrade");
        pullingUpgrade = registerUpgrade(new PullingUpgradeItem(false), "pulling_upgrade");
        pushingUpgrade = registerUpgrade(new PushingUpgradeItem(false), "pushing_upgrade");
        collectorUpgrade = registerUpgrade(new CollectorUpgradeItem(), "collector_upgrade");
        oreDictionaryUpgrade = registerUpgrade(new OreDictionaryUpgradeItem(), "ore_dictionary_upgrade");
        wirelessPullingUpgrade = registerUpgrade(new PullingUpgradeItem(true), "wireless_pulling_upgrade");
        wirelessPushingUpgrade = registerUpgrade(new PushingUpgradeItem(true), "wireless_pushing_upgrade");

        registerGenerationUpgrades();

        breakerUpgrade = registerUpgrade(new BreakerUpgradeItem(), "breaker_upgrade");
        placerUpgrade = registerUpgrade(new PlacerUpgradeItem(), "placer_upgrade");
        refillUpgrade = registerUpgrade(new RefillUpgradeItem(false), "refill_upgrade");
        dimensionalRefillUpgrade = registerUpgrade(new RefillUpgradeItem(true), "dimensional_refill_upgrade");
        speedUpgradeAugment = registerUpgrade(new UpgradeItem("speed_upgrade_augment"), "speed_upgrade_augment");
        speedUpgradeAugment.setMaxStackSize(64);

        configurationTool = new ConfigurationToolItem();
        GameRegistry.registerItem(configurationTool, "configuration_tool");

        linkingTool = new LinkingToolItem();
        GameRegistry.registerItem(linkingTool, "linking_tool");
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

    public static void registerCommonIntegrations() {
        AE2Integration.register();
    }

    public static void registerBlockProperties() {
        DrawerBlockProperties.register();
    }

    private static void registerGenerationUpgrades() {
        for (int tier = 1; tier <= 4; tier++) {
            waterGenerationUpgrades.add(
                registerUpgrade(
                    new GenerationUpgradeItem(GenerationUpgradeItem.GenerationKind.WATER, tier),
                    GenerationUpgradeItem.registryName(GenerationUpgradeItem.GenerationKind.WATER, tier)));
            stoneGenerationUpgrades.add(
                registerUpgrade(
                    new GenerationUpgradeItem(GenerationUpgradeItem.GenerationKind.STONE, tier),
                    GenerationUpgradeItem.registryName(GenerationUpgradeItem.GenerationKind.STONE, tier)));
            universalGenerationUpgrades.add(
                registerUpgrade(
                    new GenerationUpgradeItem(GenerationUpgradeItem.GenerationKind.UNIVERSAL, tier),
                    GenerationUpgradeItem.registryName(GenerationUpgradeItem.GenerationKind.UNIVERSAL, tier)));
        }
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
        return item;
    }
}
