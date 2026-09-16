package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Immutable exact item identity containing item, metadata, and stack NBT.
 * Stack count is deliberately excluded.
 */
public class ItemStorageKey implements StorageKey {

    private final Item item;
    private final int metadata;
    @Nullable
    private final NBTTagCompound tag;
    private final int hashCode;

    /**
     * Creates a key from a non-empty stack and defensively copies its NBT.
     *
     * @param stack item template; count is ignored
     * @throws NullPointerException     if {@code stack} is null
     * @throws IllegalArgumentException if {@code stack} is empty
     */
    public ItemStorageKey(@Nonnull ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.getItem() == null) {
            throw new IllegalArgumentException("stack must contain an item");
        }
        this.item = stack.getItem();
        this.metadata = stack.getItemDamage();
        NBTTagCompound sourceTag = stack.getTagCompound();
        this.tag = sourceTag == null ? null : (NBTTagCompound) sourceTag.copy();
        this.hashCode = computeHashCode();
    }

    /**
     * @return the represented item
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * @return the represented item metadata
     */
    public int getMetadata() {
        return metadata;
    }

    /**
     * @return a defensive copy of the represented stack NBT, or {@code null}
     */
    @Nullable
    public NBTTagCompound getTag() {
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }

    /**
     * @return a fresh count-one stack carrying this key's metadata and NBT
     */
    @Nonnull
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(item, 1, metadata);
        if (tag != null) {
            stack.setTagCompound((NBTTagCompound) tag.copy());
        }
        return stack;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ItemStorageKey)) {
            return false;
        }
        ItemStorageKey other = (ItemStorageKey) object;
        return item == other.item && metadata == other.metadata && Objects.equals(tag, other.tag);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ItemStorageKey{" + Item.itemRegistry.getNameForObject(item) + '@' + metadata + '}';
    }

    private int computeHashCode() {
        int result = System.identityHashCode(item);
        result = 31 * result + metadata;
        result = 31 * result + (tag == null ? 0 : tag.hashCode());
        return result;
    }
}
