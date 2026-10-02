package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;

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
}
