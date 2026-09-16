package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Framed drawer tile. Behaves exactly like a wooden drawer but carries a
 * material selection used to retexture its exterior, fronts, and divider.
 */
public class FramedDrawerTile extends ControllableDrawerTile {

    private static final String KEY_ITEMS = "Items";

    private final DrawerLayout layout;
    private final BigItemHandler handler;

    private FramedDrawerStyle style = FramedDrawerStyle.EMPTY;

    public FramedDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public FramedDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = new BigItemHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ITEM_CAPACITY, 1D);
            }

            @Override
            public boolean isLocked() {
                return FramedDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return FramedDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return FramedDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return FramedDrawerTile.this.hasMaxStorage();
            }

            @Override
            protected boolean allowsEquivalentItems() {
                return FramedDrawerTile.this.hasEquivalentItems();
            }
        };
        bindStorageHandler(handler);
    }

    /**
     * @return the slot layout of this drawer
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
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
        ItemStack exterior = front ? style.getExterior() : material;
        ItemStack frontStack = front ? material : style.getFront();
        ItemStack divider = front ? material : style.getDivider();
        FramedDrawerStyle updated = new FramedDrawerStyle(exterior, frontStack, divider);
        if (!updated.isConfigured() || updated.equals(style)) {
            return false;
        }
        setStyle(updated);
        return true;
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_ITEMS, handler.serializeNBT());
        if (style.isConfigured()) {
            tag.setTag(FramedDrawerStyle.NBT_KEY, style.writeToNBT());
        }
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_ITEMS, 10) ? tag.getCompoundTag(KEY_ITEMS) : null);
        style = tag.hasKey(FramedDrawerStyle.NBT_KEY, 10)
            ? FramedDrawerStyle.fromNBT(tag.getCompoundTag(FramedDrawerStyle.NBT_KEY))
            : FramedDrawerStyle.EMPTY;
    }

    @Override
    protected void reconcileStorageConfiguration() {
        handler.applyLockConfiguration(isLocked());
    }

    @Override
    protected int calculateRedstoneSignal() {
        long total = 0L;
        long capacity = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
            capacity += handler.getCapacity(index);
        }
        return capacity <= 0L ? 0 : redstoneForRatio(total / (double) capacity);
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
