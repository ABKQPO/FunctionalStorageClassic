package com.hfstudio.functionalstorage.common.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.api.storage.WoodTypeRegistry;

/**
 * Wood variants a wooden drawer can use. Metadata values match vanilla plank
 * metadata so crafting recipes can use plain plank items.
 */
public class DrawerWoodType implements IWoodType {

    private static final Map<String, DrawerWoodType> BY_ID = new LinkedHashMap<>();

    public static final DrawerWoodType OAK = define(new DrawerWoodType("oak", null, null, 0, 0));
    public static final DrawerWoodType SPRUCE = define(new DrawerWoodType("spruce", null, null, 1, 1));
    public static final DrawerWoodType BIRCH = define(new DrawerWoodType("birch", null, null, 2, 2));
    public static final DrawerWoodType JUNGLE = define(new DrawerWoodType("jungle", null, null, 3, 3));
    public static final DrawerWoodType ACACIA = define(
        new DrawerWoodType("acacia", "minecraft:log2", "minecraft:planks", 0, 4));
    public static final DrawerWoodType DARK_OAK = define(
        new DrawerWoodType("dark_oak", "minecraft:log2", "minecraft:planks", 1, 5));

    public static final DrawerWoodType CRIMSON = define(
        new DrawerWoodType("crimson", "etfuturum:crimson_stem", "etfuturum:wood_planks", 0, 0));
    public static final DrawerWoodType WARPED = define(
        new DrawerWoodType("warped", "etfuturum:warped_stem", "etfuturum:wood_planks", 0, 1));
    public static final DrawerWoodType MANGROVE = define(
        new DrawerWoodType("mangrove", "etfuturum:mangrove_log", "etfuturum:wood_planks", 0, 2));
    public static final DrawerWoodType CHERRY = define(
        new DrawerWoodType("cherry", "etfuturum:cherry_log", "etfuturum:wood_planks", 0, 3));

    private final String id;
    @Nullable
    private final String logName;
    @Nullable
    private final String planksName;
    private final int logMetadata;
    private final int plankMetadata;

    public DrawerWoodType(String id, @Nullable String logName, @Nullable String planksName, int logMetadata,
        int plankMetadata) {
        this.id = id;
        this.logName = logName;
        this.planksName = planksName;
        this.logMetadata = logMetadata;
        this.plankMetadata = plankMetadata;
    }

    @Nonnull
    public static DrawerWoodType fromId(@Nullable String id) {
        DrawerWoodType woodType = id == null ? null : BY_ID.get(id);
        return woodType == null ? OAK : woodType;
    }

    @Nonnull
    public static DrawerWoodType fromMetadata(int metadata) {
        DrawerWoodType[] values = values();
        return metadata < 0 || metadata >= values.length ? OAK : values[metadata];
    }

    @Nonnull
    public static List<DrawerWoodType> available() {
        List<DrawerWoodType> woodTypes = new ArrayList<>();
        for (DrawerWoodType woodType : values()) {
            if (woodType.isAvailable()) {
                woodTypes.add(woodType);
            }
        }
        return woodTypes;
    }

    public static boolean isOptionalDrawerId(@Nullable String registryName) {
        if (registryName == null) {
            return false;
        }
        for (DrawerWoodType woodType : values()) {
            if (woodType.logName == null || woodType.isAvailable()) {
                continue;
            }
            for (DrawerLayout layout : DrawerLayout.values()) {
                if ((FunctionalStorage.MOD_ID + ":" + woodType.id + "_" + layout.getSlotCount()).equals(registryName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static DrawerWoodType define(DrawerWoodType wood) {
        if (BY_ID.putIfAbsent(wood.id, wood) != null) {
            throw new IllegalArgumentException("Wood identifier already defined: " + wood.id);
        }
        return wood;
    }

    public static synchronized DrawerWoodType register(DrawerWoodType wood) {
        DrawerWoodType existing = BY_ID.get(wood.id);
        if (existing != null && existing != wood) {
            throw new IllegalArgumentException("Wood identifier already defined: " + wood.id);
        }
        WoodTypeRegistry.add(wood);
        BY_ID.put(wood.id, wood);
        return wood;
    }

    public static synchronized DrawerWoodType[] values() {
        return BY_ID.values()
            .toArray(new DrawerWoodType[0]);
    }

    public static void registerBuiltIns() {
        for (DrawerWoodType woodType : values()) {
            WoodTypeRegistry.add(woodType);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return id;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    public boolean isAvailable() {
        return getLog() != null && getPlanks() != null;
    }

    @Nullable
    @Override
    public Block getLog() {
        return logName == null ? vanillaLog() : resolve(logName);
    }

    @Nullable
    @Override
    public Block getPlanks() {
        return planksName == null ? Blocks.planks : resolve(planksName);
    }

    @Override
    public int getPlankMetadata() {
        return plankMetadata;
    }

    @Override
    public int getLogMetadata() {
        return logMetadata;
    }

    @Nullable
    public ItemStack getPlankStack() {
        Block planks = getPlanks();
        return planks == null ? null : new ItemStack(planks, 1, plankMetadata);
    }

    @Nullable
    public ItemStack getLogStack() {
        Block log = getLog();
        return log == null ? null : new ItemStack(log, 1, logMetadata);
    }

    @Nullable
    private Block vanillaLog() {
        return logMetadata < 4 ? Blocks.log : Blocks.log2;
    }

    @Nullable
    private static Block resolve(@Nonnull String name) {
        Object block = Block.blockRegistry.getObject(name);
        return block instanceof Block ? (Block) block : null;
    }
}
