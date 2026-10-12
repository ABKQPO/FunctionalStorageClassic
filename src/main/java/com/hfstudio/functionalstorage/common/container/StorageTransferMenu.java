package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

/** Maps the leading storage slots in a menu to physical storage indices on the server. */
public interface StorageTransferMenu {

    IBigItemHandler getTransferStorage();

    default IBigFluidHandler getTransferFluidStorage() {
        return null;
    }

    default IBigAspectHandler getTransferAspectStorage() {
        return null;
    }

    default boolean hasTransferStorage() {
        return getTransferStorage() != null || getTransferFluidStorage() != null || getTransferAspectStorage() != null;
    }

    int getTransferSlotCount();

    /** Returns -1 for a slot outside the current storage view. */
    int getTransferSlot(int menuSlot);

    default String getTransferResourceKind(int menuSlot) {
        if (getTransferItemSnapshot(menuSlot).hasTemplate() || getTransferStorage() != null)
            return AdaptiveMatrixTile.TERMINAL_ITEMS;
        if (getTransferFluidSnapshot(menuSlot).hasTemplate() || getTransferFluidStorage() != null)
            return AdaptiveMatrixTile.TERMINAL_FLUIDS;
        if (getTransferAspectSnapshot(menuSlot).hasTemplate() || getTransferAspectStorage() != null)
            return AdaptiveMatrixTile.TERMINAL_ASPECTS;
        return "";
    }

    default boolean isTransferViewVirtual() {
        return false;
    }

    default boolean isTransferViewMixed() {
        return false;
    }

    default BigItemStack getTransferItemSnapshot(int menuSlot) {
        IBigItemHandler handler = getTransferStorage();
        int slot = getTransferSlot(menuSlot);
        return handler == null || slot < 0 ? BigItemStack.empty() : handler.getSnapshot(slot);
    }

    default BigFluidStack getTransferFluidSnapshot(int menuSlot) {
        IBigFluidHandler handler = getTransferFluidStorage();
        int slot = getTransferSlot(menuSlot);
        return handler == null || slot < 0 ? BigFluidStack.empty() : handler.getSnapshot(slot);
    }

    default BigAspectStack getTransferAspectSnapshot(int menuSlot) {
        IBigAspectHandler handler = getTransferAspectStorage();
        int slot = getTransferSlot(menuSlot);
        return handler == null || slot < 0 ? BigAspectStack.empty() : handler.getSnapshot(slot);
    }

    default long getTransferCapacity(int menuSlot) {
        IBigItemHandler itemHandler = getTransferStorage();
        if (itemHandler != null) {
            int slot = getTransferSlot(menuSlot);
            return slot < 0 ? 0L : itemHandler.getCapacity(slot);
        }
        IBigFluidHandler fluidHandler = getTransferFluidStorage();
        if (fluidHandler != null) {
            int slot = getTransferSlot(menuSlot);
            return slot < 0 ? 0L : fluidHandler.getCapacity(slot);
        }
        IBigAspectHandler aspectHandler = getTransferAspectStorage();
        int slot = getTransferSlot(menuSlot);
        return aspectHandler == null || slot < 0 ? 0L : aspectHandler.getCapacity(slot);
    }
}
