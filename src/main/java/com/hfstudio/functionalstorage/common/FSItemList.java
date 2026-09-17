package com.hfstudio.functionalstorage.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.FunctionalStorage;

import cpw.mods.fml.common.registry.GameRegistry;
import lombok.Getter;

@SuppressWarnings("unused")
public enum FSItemList {

    OakDrawer1("oak_1"),
    OakDrawer2("oak_2"),
    OakDrawer4("oak_4"),
    SpruceDrawer1("spruce_1"),
    SpruceDrawer2("spruce_2"),
    SpruceDrawer4("spruce_4"),
    BirchDrawer1("birch_1"),
    BirchDrawer2("birch_2"),
    BirchDrawer4("birch_4"),
    JungleDrawer1("jungle_1"),
    JungleDrawer2("jungle_2"),
    JungleDrawer4("jungle_4"),
    AcaciaDrawer1("acacia_1"),
    AcaciaDrawer2("acacia_2"),
    AcaciaDrawer4("acacia_4"),
    DarkOakDrawer1("dark_oak_1"),
    DarkOakDrawer2("dark_oak_2"),
    DarkOakDrawer4("dark_oak_4"),
    MangroveDrawer1("mangrove_1"),
    MangroveDrawer2("mangrove_2"),
    MangroveDrawer4("mangrove_4"),
    CherryDrawer1("cherry_1"),
    CherryDrawer2("cherry_2"),
    CherryDrawer4("cherry_4"),
    CrimsonDrawer1("crimson_1"),
    CrimsonDrawer2("crimson_2"),
    CrimsonDrawer4("crimson_4"),
    WarpedDrawer1("warped_1"),
    WarpedDrawer2("warped_2"),
    WarpedDrawer4("warped_4"),

    FluidDrawer1("fluid_1"),
    FluidDrawer2("fluid_2"),
    FluidDrawer4("fluid_4"),

    FramedDrawer1("framed_1"),
    FramedDrawer2("framed_2"),
    FramedDrawer4("framed_4"),
    FramedFluidDrawer1("framed_fluid_1"),
    FramedFluidDrawer2("framed_fluid_2"),
    FramedFluidDrawer4("framed_fluid_4"),

    EssentiaDrawer1("essentia_1"),
    EssentiaDrawer2("essentia_2"),
    EssentiaDrawer4("essentia_4"),

    CompactingDrawer("compacting_drawer"),
    SimpleCompactingDrawer("simple_compacting_drawer"),
    EnderDrawer("ender_drawer"),
    ArmoryCabinet("armory_cabinet"),
    StorageController("storage_controller"),
    ControllerExtension("controller_extension"),

    CompactingFramedDrawer("compacting_framed_drawer"),
    FramedSimpleCompactingDrawer("framed_simple_compacting_drawer"),
    FramedStorageController("framed_storage_controller"),
    FramedControllerExtension("framed_controller_extension"),

    IronDowngrade("iron_downgrade"),
    CopperUpgrade("copper_upgrade"),
    GoldUpgrade("gold_upgrade"),
    DiamondUpgrade("diamond_upgrade"),
    NetheriteUpgrade("netherite_upgrade"),
    MaxStorageUpgrade("max_storage_upgrade"),
    CreativeVendingUpgrade("creative_vending_upgrade"),

    DrippingUpgrade("dripping_upgrade"),
    WaterGeneratorUpgrade("water_generator_upgrade"),
    ObsidianUpgrade("obsidian_upgrade"),

    VoidUpgrade("void_upgrade"),
    RedstoneUpgrade("redstone_upgrade"),
    PullingUpgrade("pulling_upgrade"),
    PushingUpgrade("pushing_upgrade"),
    CollectorUpgrade("collector_upgrade"),
    OreDictionaryUpgrade("ore_dictionary_upgrade"),
    WirelessPullingUpgrade("wireless_pulling_upgrade"),
    WirelessPushingUpgrade("wireless_pushing_upgrade"),

    WaterGenerationUpgrade1("water_generation_upgrade_t1"),
    WaterGenerationUpgrade2("water_generation_upgrade_t2"),
    WaterGenerationUpgrade3("water_generation_upgrade_t3"),
    WaterGenerationUpgrade4("water_generation_upgrade_t4"),
    StoneGenerationUpgrade1("stone_generation_upgrade_t1"),
    StoneGenerationUpgrade2("stone_generation_upgrade_t2"),
    StoneGenerationUpgrade3("stone_generation_upgrade_t3"),
    StoneGenerationUpgrade4("stone_generation_upgrade_t4"),
    UniversalItemGeneration1("universal_item_generation_t1"),
    UniversalItemGeneration2("universal_item_generation_t2"),
    UniversalItemGeneration3("universal_item_generation_t3"),
    UniversalItemGeneration4("universal_item_generation_t4"),

    BreakerUpgrade("breaker_upgrade"),
    PlacerUpgrade("placer_upgrade"),
    RefillUpgrade("refill_upgrade"),
    DimensionalRefillUpgrade("dimensional_refill_upgrade"),
    SpeedUpgradeAugment("speed_upgrade_augment"),

    ConfigurationTool("configuration_tool"),
    LinkingTool("linking_tool"),;

    public static final Map<String, FSItemList> BY_ID = new HashMap<>();

    static {
        for (FSItemList entry : values()) {
            BY_ID.put(entry.id, entry);
        }
    }

    @Getter
    private final String id;

    FSItemList(String id) {
        this.id = id;
    }

    public String getRegistryName() {
        return FunctionalStorage.MOD_ID + ":" + id;
    }

    public Block getBlock() {
        return GameRegistry.findBlock(FunctionalStorage.MOD_ID, id);
    }

    public Item getItem() {
        Item item = GameRegistry.findItem(FunctionalStorage.MOD_ID, id);
        if (item != null) {
            return item;
        }
        Block block = getBlock();
        return block == null ? null : Item.getItemFromBlock(block);
    }

    public ItemStack getStack() {
        Item item = getItem();
        return item == null ? null : new ItemStack(item, 1, 0);
    }

    public boolean isRegistered() {
        return getItem() != null;
    }

    public ItemStack get() {
        return get(1);
    }

    public ItemStack get(int amount) {
        ItemStack stack = getStack();
        if (stack == null) {
            throw new IllegalStateException("The entry '" + name() + "' has not been registered yet");
        }
        ItemStack copy = stack.copy();
        copy.stackSize = amount;
        return copy;
    }

    public static FSItemList byId(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }
}
