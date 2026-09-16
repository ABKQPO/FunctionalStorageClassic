package com.hfstudio.functionalstorage.api.storage;

/**
 * Selects whether a storage operation changes state or only reports what it
 * could do. Implementations must treat {@link #SIMULATE} as completely
 * side-effect free, including filters, NBT, and change notifications.
 */
public enum StorageAction {

    EXECUTE,
    SIMULATE;

    public static StorageAction fromSimulation(boolean simulate) {
        return simulate ? SIMULATE : EXECUTE;
    }

    public boolean isSimulation() {
        return this == SIMULATE;
    }
}
