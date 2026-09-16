package com.hfstudio.functionalstorage.common.storage;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import lombok.Getter;

/**
 * Wood variants a wooden drawer can use. Metadata values match vanilla plank
 * metadata so crafting recipes can use plain plank items.
 */
public enum DrawerWoodType {

    OAK("oak", Blocks.log, 0, 0),
    SPRUCE("spruce", Blocks.log, 1, 1),
    BIRCH("birch", Blocks.log, 2, 2),
    JUNGLE("jungle", Blocks.log, 3, 3),
    ACACIA("acacia", Blocks.log2, 4, 0),
    DARK_OAK("dark_oak", Blocks.log2, 5, 1);

    private static final Map<String, DrawerWoodType> BY_ID = new HashMap<>();

    static {
        for (DrawerWoodType woodType : values()) {
            BY_ID.put(woodType.id, woodType);
        }
    }

    private final String id;
    private final Block log;
    @Getter
    private final int logMetadata;
    private final int plankMetadata;

    DrawerWoodType(String id, Block log, int logMetadata, int plankMetadata) {
        this.id = id;
        this.log = log;
        this.logMetadata = logMetadata;
        this.plankMetadata = plankMetadata;
    }

    /**
     * Resolves a wood type from its stable identifier.
     *
     * @param id serialized wood identifier
     * @return the matching wood type, or {@link #OAK} when unknown
     */
    @Nonnull
    public static DrawerWoodType fromId(@Nullable String id) {
        DrawerWoodType woodType = id == null ? null : BY_ID.get(id);
        return woodType == null ? OAK : woodType;
    }

    /**
     * Resolves a wood type from metadata.
     *
     * @param metadata metadata value
     * @return the matching wood type, or {@link #OAK} when out of range
     */
    @Nonnull
    public static DrawerWoodType fromMetadata(int metadata) {
        DrawerWoodType[] values = values();
        return metadata < 0 || metadata >= values.length ? OAK : values[metadata];
    }

    /**
     * @return the stable low-case identifier used by assets and lang keys
     */
    @Nonnull
    public String getId() {
        return id;
    }

    /**
     * @return the log block this wood is derived from
     */
    @Nonnull
    public Block getLog() {
        return log;
    }

    /**
     * @return the plank metadata for this wood
     */
    public int getMetadata() {
        return plankMetadata;
    }

    /**
     * @return a plank stack of this wood
     */
    @Nonnull
    public ItemStack getPlankStack() {
        return new ItemStack(Blocks.planks, 1, plankMetadata);
    }

    /**
     * @return a log stack of this wood
     */
    @Nonnull
    public ItemStack getLogStack() {
        return new ItemStack(log, 1, logMetadata);
    }
}
