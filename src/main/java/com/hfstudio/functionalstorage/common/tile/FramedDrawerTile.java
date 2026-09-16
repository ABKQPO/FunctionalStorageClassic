package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;

public class FramedDrawerTile extends WoodDrawerTile {

    private FramedDrawerStyle style = FramedDrawerStyle.EMPTY;

    public FramedDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public FramedDrawerTile(@Nonnull DrawerLayout layout) {
        super(layout);
    }

    @Nonnull
    public FramedDrawerStyle getStyle() {
        return style;
    }

    public void setStyle(@Nonnull FramedDrawerStyle style) {
        if (this.style.equals(style)) {
            return;
        }
        this.style = style;
        markDirty();
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /** Sneaking selects front and divider materials; otherwise selects the exterior. */
    public boolean applyMaterial(@Nullable ItemStack material, boolean front) {
        if (material == null || material.getItem() == null) {
            return false;
        }
        ItemStack exterior = front && style.isConfigured() ? style.getExterior() : material;
        ItemStack frontStack = front || !style.isConfigured() ? material : style.getFront();
        ItemStack divider = front || !style.isConfigured() ? material : style.getDivider();
        FramedDrawerStyle updated = new FramedDrawerStyle(exterior, frontStack, divider);
        if (!updated.isConfigured() || updated.equals(style)) {
            return false;
        }
        setStyle(updated);
        return true;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        super.writeStorageData(tag);
        if (style.isConfigured()) {
            tag.setTag(FramedDrawerStyle.NBT_KEY, style.writeToNBT());
        }
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        super.readStorageData(tag);
        style = tag.hasKey(FramedDrawerStyle.NBT_KEY, 10)
            ? FramedDrawerStyle.fromNBT(tag.getCompoundTag(FramedDrawerStyle.NBT_KEY))
            : FramedDrawerStyle.EMPTY;
    }

    @Override
    public ItemStack createDropStack(@Nonnull ItemStack base) {
        ItemStack result = super.createDropStack(base);
        if (style.isConfigured()) {
            style.applyDrawerStyle(result);
        }
        return result;
    }
}
