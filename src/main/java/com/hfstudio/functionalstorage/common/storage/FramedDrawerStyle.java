package com.hfstudio.functionalstorage.common.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

/**
 * Immutable material selection for the three visible parts of a framed drawer:
 * the exterior casing, the drawer fronts, and the divider between slots.
 */
public class FramedDrawerStyle {

    public static final String NBT_KEY = "FramedStyle";
    public static final FramedDrawerStyle EMPTY = new FramedDrawerStyle(null, null, null);

    private static final String KEY_EXTERIOR = "Exterior";
    private static final String KEY_FRONT = "Front";
    private static final String KEY_DIVIDER = "Divider";

    @Nullable
    private final ItemStack exterior;
    @Nullable
    private final ItemStack front;
    @Nullable
    private final ItemStack divider;
    private final String cacheKey;

    public FramedDrawerStyle(@Nullable ItemStack exterior, @Nullable ItemStack front, @Nullable ItemStack divider) {
        this.exterior = normalize(exterior);
        this.front = normalize(front);
        this.divider = normalize(divider);
        this.cacheKey = buildCacheKey();
    }

    @Nonnull
    public static FramedDrawerStyle fromNBT(@Nullable NBTTagCompound tag) {
        if (tag == null || tag.hasNoTags()) {
            return EMPTY;
        }
        FramedDrawerStyle style = new FramedDrawerStyle(
            readStack(tag, KEY_EXTERIOR),
            readStack(tag, KEY_FRONT),
            readStack(tag, KEY_DIVIDER));
        return style.isConfigured() ? style : EMPTY;
    }

    @Nonnull
    public static FramedDrawerStyle fromDrawerStack(@Nullable ItemStack drawer) {
        if (drawer == null || drawer.getItem() == null || !drawer.hasTagCompound()) {
            return EMPTY;
        }
        NBTTagCompound root = drawer.getTagCompound();
        if (!root.hasKey("TileData", 10)) {
            return EMPTY;
        }
        NBTTagCompound tileData = root.getCompoundTag("TileData");
        return tileData.hasKey(NBT_KEY, 10) ? fromNBT(tileData.getCompoundTag(NBT_KEY)) : EMPTY;
    }

    public boolean isConfigured() {
        return exterior != null && front != null;
    }

    /** Resolves material items consistently for crafting, placement, and rendering. */
    @Nullable
    public static Block materialBlock(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == null || block == Blocks.air || block instanceof DrawerBlock ? null : block;
    }

    @Nullable
    public ItemStack getExterior() {
        return exterior == null ? null : exterior.copy();
    }

    @Nullable
    public ItemStack getFront() {
        return front == null ? null : front.copy();
    }

    /**
     * An omitted divider follows the exterior material.
     *
     * @return the divider material, or {@code null}
     */
    @Nullable
    public ItemStack getDivider() {
        ItemStack source = divider != null ? divider : exterior;
        return source == null ? null : source.copy();
    }

    @Nonnull
    public String getCacheKey() {
        return cacheKey;
    }

    @Nonnull
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        writeStack(tag, KEY_EXTERIOR, exterior);
        writeStack(tag, KEY_FRONT, front);
        writeStack(tag, KEY_DIVIDER, divider);
        return tag;
    }

    public void applyDrawerStyle(@Nonnull ItemStack drawer) {
        if (drawer.getItem() == null || !isConfigured()) {
            return;
        }
        if (!drawer.hasTagCompound()) {
            drawer.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound root = drawer.getTagCompound();
        NBTTagCompound tileData = root.hasKey("TileData", 10) ? root.getCompoundTag("TileData") : new NBTTagCompound();
        tileData.setTag(NBT_KEY, writeToNBT());
        root.setTag("TileData", tileData);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FramedDrawerStyle)) {
            return false;
        }
        return cacheKey.equals(((FramedDrawerStyle) object).cacheKey);
    }

    @Override
    public int hashCode() {
        return cacheKey.hashCode();
    }

    @Override
    public String toString() {
        return cacheKey;
    }

    @Nullable
    private static ItemStack normalize(@Nullable ItemStack stack) {
        if (materialBlock(stack) == null) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    private static void writeStack(@Nonnull NBTTagCompound parent, @Nonnull String key, @Nullable ItemStack stack) {
        if (stack != null) {
            parent.setTag(key, stack.writeToNBT(new NBTTagCompound()));
        }
    }

    @Nullable
    private static ItemStack readStack(@Nonnull NBTTagCompound parent, @Nonnull String key) {
        if (!parent.hasKey(key, 10)) {
            return null;
        }
        ItemStack stack = ItemStack.loadItemStackFromNBT(parent.getCompoundTag(key));
        return stack == null || stack.getItem() == null ? null : stack;
    }

    @Nonnull
    private String buildCacheKey() {
        return describe(exterior) + '|' + describe(front) + '|' + describe(divider);
    }

    @Nonnull
    private static String describe(@Nullable ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        NBTTagCompound tag = stack.getTagCompound();
        return Item.itemRegistry.getNameForObject(stack.getItem()) + '@'
            + stack.getItemDamage()
            + (tag == null ? "" : tag.toString());
    }
}
