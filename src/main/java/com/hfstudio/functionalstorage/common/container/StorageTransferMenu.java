package com.hfstudio.functionalstorage.common.container;

import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;

/** Maps the leading storage slots in a menu to physical storage indices on the server. */
public interface StorageTransferMenu {

    IBigItemHandler getTransferStorage();

    int getTransferSlotCount();

    /** Returns -1 for a slot outside the current storage view. */
    int getTransferSlot(int menuSlot);
}
