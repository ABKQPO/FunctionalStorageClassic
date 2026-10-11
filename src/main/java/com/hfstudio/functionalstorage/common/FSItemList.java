package com.hfstudio.functionalstorage.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import lombok.Getter;

@SuppressWarnings("unused")
public class FSItemList {

    private static final Map<String, FSItemList> ENTRIES = new LinkedHashMap<>();

    public static final FSItemList OakDrawer1 = register("oak_1");
    public static final FSItemList OakDrawer2 = register("oak_2");
    public static final FSItemList OakDrawer4 = register("oak_4");
    public static final FSItemList SpruceDrawer1 = register("spruce_1");
    public static final FSItemList SpruceDrawer2 = register("spruce_2");
    public static final FSItemList SpruceDrawer4 = register("spruce_4");
    public static final FSItemList BirchDrawer1 = register("birch_1");
    public static final FSItemList BirchDrawer2 = register("birch_2");
    public static final FSItemList BirchDrawer4 = register("birch_4");
    public static final FSItemList JungleDrawer1 = register("jungle_1");
    public static final FSItemList JungleDrawer2 = register("jungle_2");
    public static final FSItemList JungleDrawer4 = register("jungle_4");
    public static final FSItemList AcaciaDrawer1 = register("acacia_1");
    public static final FSItemList AcaciaDrawer2 = register("acacia_2");
    public static final FSItemList AcaciaDrawer4 = register("acacia_4");
    public static final FSItemList DarkOakDrawer1 = register("dark_oak_1");
    public static final FSItemList DarkOakDrawer2 = register("dark_oak_2");
    public static final FSItemList DarkOakDrawer4 = register("dark_oak_4");
    public static final FSItemList MangroveDrawer1 = register("mangrove_1");
    public static final FSItemList MangroveDrawer2 = register("mangrove_2");
    public static final FSItemList MangroveDrawer4 = register("mangrove_4");
    public static final FSItemList CherryDrawer1 = register("cherry_1");
    public static final FSItemList CherryDrawer2 = register("cherry_2");
    public static final FSItemList CherryDrawer4 = register("cherry_4");
    public static final FSItemList CrimsonDrawer1 = register("crimson_1");
    public static final FSItemList CrimsonDrawer2 = register("crimson_2");
    public static final FSItemList CrimsonDrawer4 = register("crimson_4");
    public static final FSItemList WarpedDrawer1 = register("warped_1");
    public static final FSItemList WarpedDrawer2 = register("warped_2");
    public static final FSItemList WarpedDrawer4 = register("warped_4");

    public static final FSItemList FluidDrawer1 = register("fluid_1");
    public static final FSItemList FluidDrawer2 = register("fluid_2");
    public static final FSItemList FluidDrawer4 = register("fluid_4");

    public static final FSItemList FramedDrawer1 = register("framed_1");
    public static final FSItemList FramedDrawer2 = register("framed_2");
    public static final FSItemList FramedDrawer4 = register("framed_4");
    public static final FSItemList FramedFluidDrawer1 = register("framed_fluid_1");
    public static final FSItemList FramedFluidDrawer2 = register("framed_fluid_2");
    public static final FSItemList FramedFluidDrawer4 = register("framed_fluid_4");

    public static final FSItemList EssentiaDrawer1 = register("essentia_1");
    public static final FSItemList EssentiaDrawer2 = register("essentia_2");
    public static final FSItemList EssentiaDrawer4 = register("essentia_4");

    public static final FSItemList CompactingDrawer = register("compacting_drawer");
    public static final FSItemList SimpleCompactingDrawer = register("simple_compacting_drawer");
    public static final FSItemList EnderDrawer = register("ender_drawer");
    public static final FSItemList ArmoryCabinet = register("armory_cabinet");
    public static final FSItemList StorageController = register("storage_controller");
    public static final FSItemList ControllerExtension = register("controller_extension");
    public static final FSItemList AdaptiveMatrix = register("adaptive_matrix");
    public static final FSItemList AdaptiveDrawer = register("adaptive_drawer");
    public static final FSItemList AdaptiveOakDrawer = register("adaptive_oak_drawer");
    public static final FSItemList AdaptiveBirchDrawer = register("adaptive_birch_drawer");
    public static final FSItemList AdaptiveJungleDrawer = register("adaptive_jungle_drawer");
    public static final FSItemList AdaptiveAcaciaDrawer = register("adaptive_acacia_drawer");
    public static final FSItemList AdaptiveDarkOakDrawer = register("adaptive_dark_oak_drawer");
    public static final FSItemList AdaptiveMangroveDrawer = register("adaptive_mangrove_drawer");
    public static final FSItemList AdaptiveCherryDrawer = register("adaptive_cherry_drawer");
    public static final FSItemList AdaptiveCrimsonDrawer = register("adaptive_crimson_drawer");
    public static final FSItemList AdaptiveWarpedDrawer = register("adaptive_warped_drawer");
    public static final FSItemList AdaptiveEssentiaDrawer = register("adaptive_essentia_drawer");
    public static final FSItemList AdaptiveGrid = register("adaptive_grid");
    public static final FSItemList AdaptiveCraftingGrid = register("adaptive_crafting_grid");
    public static final FSItemList AdaptiveDeposit = register("adaptive_deposit");
    public static final FSItemList AdaptiveUpgrade = register("adaptive_upgrade");
    public static final FSItemList AdaptiveConfigurationTool = register("adaptive_configuration_tool");

    public static final FSItemList CompactingFramedDrawer = register("compacting_framed_drawer");
    public static final FSItemList FramedSimpleCompactingDrawer = register("framed_simple_compacting_drawer");
    public static final FSItemList FramedStorageController = register("framed_storage_controller");
    public static final FSItemList FramedControllerExtension = register("framed_controller_extension");

    public static final FSItemList BlankUpgrade = register("base");
    public static final FSItemList IronDowngrade = register("iron_downgrade");
    public static final FSItemList CopperUpgrade = register("copper_upgrade");
    public static final FSItemList GoldUpgrade = register("gold_upgrade");
    public static final FSItemList DiamondUpgrade = register("diamond_upgrade");
    public static final FSItemList NetheriteUpgrade = register("netherite_upgrade");
    public static final FSItemList MaxStorageUpgrade = register("max_storage_upgrade");
    public static final FSItemList CreativeVendingUpgrade = register("creative_vending_upgrade");
    public static final FSItemList TurtleScuteUpgrade = register("turtle_scute_upgrade");
    public static final FSItemList NetherStarUpgrade = register("nether_star_upgrade");
    public static final FSItemList HeavyCoreUpgrade = register("heavy_core_upgrade");

    public static final FSItemList DrippingUpgrade = register("dripping_upgrade_1");
    public static final FSItemList ObsidianUpgrade = register("obsidian_upgrade_1");
    public static final FSItemList DrippingUpgrade2 = register("dripping_upgrade_2");
    public static final FSItemList DrippingUpgrade3 = register("dripping_upgrade_3");
    public static final FSItemList DrippingUpgrade4 = register("dripping_upgrade_4");
    public static final FSItemList DrippingUpgrade5 = register("dripping_upgrade_5");
    public static final FSItemList ObsidianUpgrade2 = register("obsidian_upgrade_2");
    public static final FSItemList ObsidianUpgrade3 = register("obsidian_upgrade_3");
    public static final FSItemList ObsidianUpgrade4 = register("obsidian_upgrade_4");
    public static final FSItemList ObsidianUpgrade5 = register("obsidian_upgrade_5");
    public static final FSItemList GravelGeneratorUpgrade1 = register("gravel_generator_upgrade_1");
    public static final FSItemList GravelGeneratorUpgrade2 = register("gravel_generator_upgrade_2");
    public static final FSItemList GravelGeneratorUpgrade3 = register("gravel_generator_upgrade_3");
    public static final FSItemList GravelGeneratorUpgrade4 = register("gravel_generator_upgrade_4");
    public static final FSItemList GravelGeneratorUpgrade5 = register("gravel_generator_upgrade_5");
    public static final FSItemList DirtGeneratorUpgrade1 = register("dirt_generator_upgrade_1");
    public static final FSItemList DirtGeneratorUpgrade2 = register("dirt_generator_upgrade_2");
    public static final FSItemList DirtGeneratorUpgrade3 = register("dirt_generator_upgrade_3");
    public static final FSItemList DirtGeneratorUpgrade4 = register("dirt_generator_upgrade_4");
    public static final FSItemList DirtGeneratorUpgrade5 = register("dirt_generator_upgrade_5");
    public static final FSItemList SandGeneratorUpgrade1 = register("sand_generator_upgrade_1");
    public static final FSItemList SandGeneratorUpgrade2 = register("sand_generator_upgrade_2");
    public static final FSItemList SandGeneratorUpgrade3 = register("sand_generator_upgrade_3");
    public static final FSItemList SandGeneratorUpgrade4 = register("sand_generator_upgrade_4");
    public static final FSItemList SandGeneratorUpgrade5 = register("sand_generator_upgrade_5");
    public static final FSItemList AndesiteGeneratorUpgrade1 = register("andesite_generator_upgrade_1");
    public static final FSItemList AndesiteGeneratorUpgrade2 = register("andesite_generator_upgrade_2");
    public static final FSItemList AndesiteGeneratorUpgrade3 = register("andesite_generator_upgrade_3");
    public static final FSItemList AndesiteGeneratorUpgrade4 = register("andesite_generator_upgrade_4");
    public static final FSItemList AndesiteGeneratorUpgrade5 = register("andesite_generator_upgrade_5");
    public static final FSItemList DioriteGeneratorUpgrade1 = register("diorite_generator_upgrade_1");
    public static final FSItemList DioriteGeneratorUpgrade2 = register("diorite_generator_upgrade_2");
    public static final FSItemList DioriteGeneratorUpgrade3 = register("diorite_generator_upgrade_3");
    public static final FSItemList DioriteGeneratorUpgrade4 = register("diorite_generator_upgrade_4");
    public static final FSItemList DioriteGeneratorUpgrade5 = register("diorite_generator_upgrade_5");
    public static final FSItemList GraniteGeneratorUpgrade1 = register("granite_generator_upgrade_1");
    public static final FSItemList GraniteGeneratorUpgrade2 = register("granite_generator_upgrade_2");
    public static final FSItemList GraniteGeneratorUpgrade3 = register("granite_generator_upgrade_3");
    public static final FSItemList GraniteGeneratorUpgrade4 = register("granite_generator_upgrade_4");
    public static final FSItemList GraniteGeneratorUpgrade5 = register("granite_generator_upgrade_5");
    public static final FSItemList NetherrackGeneratorUpgrade1 = register("netherrack_generator_upgrade_1");
    public static final FSItemList NetherrackGeneratorUpgrade2 = register("netherrack_generator_upgrade_2");
    public static final FSItemList NetherrackGeneratorUpgrade3 = register("netherrack_generator_upgrade_3");
    public static final FSItemList NetherrackGeneratorUpgrade4 = register("netherrack_generator_upgrade_4");
    public static final FSItemList NetherrackGeneratorUpgrade5 = register("netherrack_generator_upgrade_5");
    public static final FSItemList EndStoneGeneratorUpgrade1 = register("end_stone_generator_upgrade_1");
    public static final FSItemList EndStoneGeneratorUpgrade2 = register("end_stone_generator_upgrade_2");
    public static final FSItemList EndStoneGeneratorUpgrade3 = register("end_stone_generator_upgrade_3");
    public static final FSItemList EndStoneGeneratorUpgrade4 = register("end_stone_generator_upgrade_4");
    public static final FSItemList EndStoneGeneratorUpgrade5 = register("end_stone_generator_upgrade_5");
    public static final FSItemList SoulSandGeneratorUpgrade1 = register("soul_sand_generator_upgrade_1");
    public static final FSItemList SoulSandGeneratorUpgrade2 = register("soul_sand_generator_upgrade_2");
    public static final FSItemList SoulSandGeneratorUpgrade3 = register("soul_sand_generator_upgrade_3");
    public static final FSItemList SoulSandGeneratorUpgrade4 = register("soul_sand_generator_upgrade_4");
    public static final FSItemList SoulSandGeneratorUpgrade5 = register("soul_sand_generator_upgrade_5");
    public static final FSItemList ClayGeneratorUpgrade1 = register("clay_generator_upgrade_1");
    public static final FSItemList ClayGeneratorUpgrade2 = register("clay_generator_upgrade_2");
    public static final FSItemList ClayGeneratorUpgrade3 = register("clay_generator_upgrade_3");
    public static final FSItemList ClayGeneratorUpgrade4 = register("clay_generator_upgrade_4");
    public static final FSItemList ClayGeneratorUpgrade5 = register("clay_generator_upgrade_5");
    public static final FSItemList CobbledDeepslateGeneratorUpgrade1 = register(
        "cobbled_deepslate_generator_upgrade_1");
    public static final FSItemList CobbledDeepslateGeneratorUpgrade2 = register(
        "cobbled_deepslate_generator_upgrade_2");
    public static final FSItemList CobbledDeepslateGeneratorUpgrade3 = register(
        "cobbled_deepslate_generator_upgrade_3");
    public static final FSItemList CobbledDeepslateGeneratorUpgrade4 = register(
        "cobbled_deepslate_generator_upgrade_4");
    public static final FSItemList CobbledDeepslateGeneratorUpgrade5 = register(
        "cobbled_deepslate_generator_upgrade_5");
    public static final FSItemList BlackstoneGeneratorUpgrade1 = register("blackstone_generator_upgrade_1");
    public static final FSItemList BlackstoneGeneratorUpgrade2 = register("blackstone_generator_upgrade_2");
    public static final FSItemList BlackstoneGeneratorUpgrade3 = register("blackstone_generator_upgrade_3");
    public static final FSItemList BlackstoneGeneratorUpgrade4 = register("blackstone_generator_upgrade_4");
    public static final FSItemList BlackstoneGeneratorUpgrade5 = register("blackstone_generator_upgrade_5");
    public static final FSItemList TuffGeneratorUpgrade1 = register("tuff_generator_upgrade_1");
    public static final FSItemList TuffGeneratorUpgrade2 = register("tuff_generator_upgrade_2");
    public static final FSItemList TuffGeneratorUpgrade3 = register("tuff_generator_upgrade_3");
    public static final FSItemList TuffGeneratorUpgrade4 = register("tuff_generator_upgrade_4");
    public static final FSItemList TuffGeneratorUpgrade5 = register("tuff_generator_upgrade_5");
    public static final FSItemList BasaltGeneratorUpgrade1 = register("basalt_generator_upgrade_1");
    public static final FSItemList BasaltGeneratorUpgrade2 = register("basalt_generator_upgrade_2");
    public static final FSItemList BasaltGeneratorUpgrade3 = register("basalt_generator_upgrade_3");
    public static final FSItemList BasaltGeneratorUpgrade4 = register("basalt_generator_upgrade_4");
    public static final FSItemList BasaltGeneratorUpgrade5 = register("basalt_generator_upgrade_5");
    public static final FSItemList DripstoneGeneratorUpgrade1 = register("dripstone_generator_upgrade_1");
    public static final FSItemList DripstoneGeneratorUpgrade2 = register("dripstone_generator_upgrade_2");
    public static final FSItemList DripstoneGeneratorUpgrade3 = register("dripstone_generator_upgrade_3");
    public static final FSItemList DripstoneGeneratorUpgrade4 = register("dripstone_generator_upgrade_4");
    public static final FSItemList DripstoneGeneratorUpgrade5 = register("dripstone_generator_upgrade_5");
    public static final FSItemList PrismarineGeneratorUpgrade1 = register("prismarine_generator_upgrade_1");
    public static final FSItemList PrismarineGeneratorUpgrade2 = register("prismarine_generator_upgrade_2");
    public static final FSItemList PrismarineGeneratorUpgrade3 = register("prismarine_generator_upgrade_3");
    public static final FSItemList PrismarineGeneratorUpgrade4 = register("prismarine_generator_upgrade_4");
    public static final FSItemList PrismarineGeneratorUpgrade5 = register("prismarine_generator_upgrade_5");
    public static final FSItemList SculkGeneratorUpgrade1 = register("sculk_generator_upgrade_1");
    public static final FSItemList SculkGeneratorUpgrade2 = register("sculk_generator_upgrade_2");
    public static final FSItemList SculkGeneratorUpgrade3 = register("sculk_generator_upgrade_3");
    public static final FSItemList SculkGeneratorUpgrade4 = register("sculk_generator_upgrade_4");
    public static final FSItemList SculkGeneratorUpgrade5 = register("sculk_generator_upgrade_5");
    public static final FSItemList MilkGeneratorUpgrade1 = register("milk_generator_upgrade_1");
    public static final FSItemList MilkGeneratorUpgrade2 = register("milk_generator_upgrade_2");
    public static final FSItemList MilkGeneratorUpgrade3 = register("milk_generator_upgrade_3");
    public static final FSItemList MilkGeneratorUpgrade4 = register("milk_generator_upgrade_4");
    public static final FSItemList MilkGeneratorUpgrade5 = register("milk_generator_upgrade_5");
    public static final FSItemList HoneyGeneratorUpgrade1 = register("honey_generator_upgrade_1");
    public static final FSItemList HoneyGeneratorUpgrade2 = register("honey_generator_upgrade_2");
    public static final FSItemList HoneyGeneratorUpgrade3 = register("honey_generator_upgrade_3");
    public static final FSItemList HoneyGeneratorUpgrade4 = register("honey_generator_upgrade_4");
    public static final FSItemList HoneyGeneratorUpgrade5 = register("honey_generator_upgrade_5");

    public static final FSItemList VoidUpgrade = register("void_upgrade");
    public static final FSItemList RedstoneUpgrade = register("redstone_upgrade");
    public static final FSItemList PullingUpgrade = register("pulling_upgrade");
    public static final FSItemList PushingUpgrade = register("pushing_upgrade");
    public static final FSItemList CollectorUpgrade = register("collector_upgrade");
    public static final FSItemList WirelessPullingUpgrade = register("wireless_pulling_upgrade");
    public static final FSItemList WirelessPushingUpgrade = register("wireless_pushing_upgrade");

    public static final FSItemList WaterGenerationUpgrade1 = register("water_generation_upgrade_1");
    public static final FSItemList WaterGenerationUpgrade2 = register("water_generation_upgrade_2");
    public static final FSItemList WaterGenerationUpgrade3 = register("water_generation_upgrade_3");
    public static final FSItemList WaterGenerationUpgrade4 = register("water_generation_upgrade_4");
    public static final FSItemList WaterGenerationUpgrade5 = register("water_generation_upgrade_5");
    public static final FSItemList StoneGenerationUpgrade1 = register("stone_generation_upgrade_1");
    public static final FSItemList StoneGenerationUpgrade2 = register("stone_generation_upgrade_2");
    public static final FSItemList StoneGenerationUpgrade3 = register("stone_generation_upgrade_3");
    public static final FSItemList StoneGenerationUpgrade4 = register("stone_generation_upgrade_4");
    public static final FSItemList StoneGenerationUpgrade5 = register("stone_generation_upgrade_5");
    public static final FSItemList UniversalItemGeneration1 = register("universal_item_generation_1");
    public static final FSItemList UniversalItemGeneration2 = register("universal_item_generation_2");
    public static final FSItemList UniversalItemGeneration3 = register("universal_item_generation_3");
    public static final FSItemList UniversalItemGeneration4 = register("universal_item_generation_4");
    public static final FSItemList UniversalItemGeneration5 = register("universal_item_generation_5");

    public static final FSItemList BreakerUpgrade = register("breaker_upgrade");
    public static final FSItemList PlacerUpgrade = register("placer_upgrade");
    public static final FSItemList RefillUpgrade = register("refill_upgrade");
    public static final FSItemList DimensionalRefillUpgrade = register("dimensional_refill_upgrade");
    public static final FSItemList SpeedUpgradeAugment = register("speed_upgrade_augment");
    public static final FSItemList EfficiencyUpgradeAugment = register("efficiency_upgrade_augment");

    public static final FSItemList ConfigurationTool = register("configuration_tool");
    public static final FSItemList LinkingTool = register("linking_tool");

    @Getter
    private final String id;
    private ItemStack stack;

    private FSItemList(String id) {
        this.id = id;
    }

    public Item getItem() {
        return stack == null ? null : stack.getItem();
    }

    public Block getBlock() {
        Item item = getItem();
        return item == null ? null : Block.getBlockFromItem(item);
    }

    public FSItemList set(Item item) {
        return set(new ItemStack(item));
    }

    public FSItemList set(Block block) {
        return set(Item.getItemFromBlock(block));
    }

    public FSItemList set(ItemStack stack) {
        this.stack = stack.copy();
        this.stack.stackSize = 1;
        return this;
    }

    public boolean hasBeenSet() {
        return stack != null;
    }

    public ItemStack getStack() {
        return stack == null ? null : stack.copy();
    }

    public ItemStack get() {
        return get(1);
    }

    public ItemStack get(int amount) {
        if (stack == null) {
            throw new IllegalStateException("The entry '" + id + "' has not been registered yet");
        }
        ItemStack copy = stack.copy();
        copy.stackSize = amount;
        return copy;
    }

    public static synchronized FSItemList register(String id) {
        return ENTRIES.computeIfAbsent(Objects.requireNonNull(id, "id"), FSItemList::new);
    }

    public static synchronized FSItemList[] values() {
        return ENTRIES.values()
            .toArray(new FSItemList[0]);
    }

    public static FSItemList byName(String name) {
        return ENTRIES.get(name);
    }
}
