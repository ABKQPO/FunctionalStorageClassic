package com.hfstudio.functionalstorage.common.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.ItemUtil;

/**
 * Item resource kind. Capacity follows the configured base capacity scaled by
 * the item's own maximum stack size so tools and other low-stack items do not
 * gain an unfair advantage.
 */
public class ItemStorageResource implements StorageResource<BigItemStack, ItemStorageKey> {

    public static final ItemStorageResource INSTANCE = new ItemStorageResource();

    @Override
    public String getId() {
        return "item";
    }

    @Nonnull
    @Override
    public BigItemStack empty() {
        return BigItemStack.empty();
    }

    @Override
    public boolean hasTemplate(@Nonnull BigItemStack snapshot) {
        return snapshot.hasTemplate();
    }

    @Nonnull
    @Override
    public BigItemStack templateOf(@Nonnull BigItemStack snapshot) {
        return snapshot.withAmount(1L);
    }

    @Override
    public boolean matches(@Nonnull BigItemStack left, @Nonnull BigItemStack right) {
        return left.hasTemplate() && left.isSameType(right);
    }

    @Override
    public boolean accepts(@Nonnull BigItemStack template, @Nonnull BigItemStack candidate) {
        return matches(template, candidate);
    }

    @Override
    public long capacityFor(@Nonnull BigItemStack template) {
        ItemStack stack = template.getTemplate();
        return stack == null ? defaultCapacity() : capacityForStack(stack);
    }

    @Override
    public long defaultCapacity() {
        return Math.max(0L, FunctionalStorageConfig.STORAGE.baseItemCapacity);
    }

    @Override
    public int upgradeDivisor() {
        return 1;
    }

    public long capacityForStack(@Nonnull ItemStack template) {
        int maxStackSize = Math.max(0, template.getMaxStackSize());
        double capacity = (double) defaultCapacity() * maxStackSize;
        return capacity >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) Math.floor(capacity));
    }

    @Nullable
    @Override
    public NBTTagCompound writeTemplate(@Nonnull BigItemStack snapshot) {
        ItemStack stack = snapshot.getTemplate();
        return stack == null ? null : stack.writeToNBT(new NBTTagCompound());
    }

    @Nonnull
    @Override
    public BigItemStack readSnapshot(@Nullable NBTTagCompound tag, long amount) {
        if (tag == null) {
            return BigItemStack.empty();
        }
        ItemStack stack = ItemUtil.readStack(tag);
        return stack == null ? BigItemStack.empty() : new BigItemStack(stack, amount);
    }
}
