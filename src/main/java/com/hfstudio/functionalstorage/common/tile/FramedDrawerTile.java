package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;

public class FramedDrawerTile extends WoodDrawerTile {

    public FramedDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public FramedDrawerTile(@Nonnull DrawerLayout layout) {
        super(layout);
    }

    /** Sneaking selects front and divider materials; otherwise selects the exterior. */
    public boolean applyMaterial(@Nullable ItemStack material, boolean front) {
        if (material == null || material.getItem() == null) {
            return false;
        }
        FramedDrawerStyle style = getStyle();
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

}
