package com.hfstudio.functionalstorage.common.presentation;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;

public interface AdaptiveDisplayProvider {

    default int getRequestedDisplayCells() {
        return -1;
    }

    default boolean isDisplayCellReserved(int cell) {
        return false;
    }

    @Nullable
    default ItemStack getDisplayReservation(int cell) {
        return null;
    }

    /**
     * @return the current face entries, or {@code null} when the physical storage should render itself
     */
    @Nullable
    List<AdaptiveDisplayEntry> getAdaptiveDisplayEntries();

    /**
     * @return the current presentation geometry, or {@code null} when the physical
     *         drawer geometry should be used
     */
    @Nullable
    default DrawerFaceLayout getAdaptiveDisplayLayout() {
        return null;
    }
}
