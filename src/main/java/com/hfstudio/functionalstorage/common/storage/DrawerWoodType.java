package com.hfstudio.functionalstorage.common.storage;

import java.util.ArrayList;
import java.util.HashMap;
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
public enum DrawerWoodType implements IWoodType {

    OAK("oak", null, null, 0, 0),
    SPRUCE("spruce", null, null, 1, 1),
    BIRCH("birch", null, null, 2, 2),
    JUNGLE("jungle", null, null, 3, 3),
    ACACIA("acacia", null, null, 4, 0),
    DARK_OAK("dark_oak", null, null, 5, 1),

    CRIMSON("crimson", "etfuturum:crimson_stem", "etfuturum:wood_planks", 0, 0),
    WARPED("warped", "etfuturum:warped_stem", "etfuturum:wood_planks", 0, 1),
    MANGROVE("mangrove", "etfuturum:mangrove_log", "etfuturum:wood_planks", 0, 2),
    CHERRY("cherry", "etfuturum:cherry_log", "etfuturum:wood_planks", 0, 3);

    private static final Map<String, DrawerWoodType> BY_ID = new HashMap<>();

    static {
        for (DrawerWoodType woodType : values()) {
            BY_ID.put(woodType.id, woodType);
        }
    }

    private final String id;
    @Nullable
    private final String logName;
    @Nullable
    private final String planksName;
    private final int logMetadata;
    private final int plankMetadata;

    DrawerWoodType(String id, @Nullable String logName, @Nullable String planksName, int logMetadata,
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
        return switch (this) {
            case OAK, SPRUCE, BIRCH, JUNGLE -> Blocks.log;
            case ACACIA, DARK_OAK -> Blocks.log2;
            default -> null;
        };
    }

    @Nullable
    private static Block resolve(@Nonnull String name) {
        Object block = Block.blockRegistry.getObject(name);
        return block instanceof Block ? (Block) block : null;
    }
}
