package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;

/**
 * Framed drawer tile. Behaves exactly like a wooden drawer but carries a
 * material selection used to retexture its exterior, fronts, and divider.
 */
public class FramedDrawerTile extends WoodDrawerTile {

    private FramedDrawerStyle style = FramedDrawerStyle.EMPTY;

    public FramedDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public FramedDrawerTile(@Nonnull DrawerLayout layout) {
        super(layout);
    }

    /**
     * @return the current material selection
     */
    @Nonnull
    public FramedDrawerStyle getStyle() {
        return style;
    }

    /**
     * Applies a new material selection, for example from the configuration tool.
     *
     * @param style new selection
     */
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

    /**
     * Replaces this drawer's material selection from a material stack.
     * Sneaking sets the front and divider, otherwise the exterior is set.
     *
     * @param material block item to take the texture from
     * @param front    whether the front and divider are being set
     * @return whether the style changed
     */
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
