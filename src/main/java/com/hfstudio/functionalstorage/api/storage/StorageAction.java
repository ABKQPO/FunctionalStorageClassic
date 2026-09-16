package com.hfstudio.functionalstorage.api.storage;

/**
 * Selects whether a storage operation changes state or only reports what it
 * could do. Implementations must treat {@link #SIMULATE} as completely
 * side-effect free, including filters, NBT, and change notifications.
 */
public enum StorageAction {
    /**
     * Apply the operation to storage.
     */
    EXECUTE,
    /**
     * Calculate the result without changing any observable state.
     */
    SIMULATE;

    /**
     * Converts Forge's simulation flag to an explicit action.
     *
     * @param simulate {@code true} when Forge requested a simulation
     * @return the corresponding action
     */
    public static StorageAction fromSimulation(boolean simulate) {
        return simulate ? SIMULATE : EXECUTE;
    }

    /**
     * @return {@code true} only for a side-effect-free simulation
     */
    public boolean isSimulation() {
        return this == SIMULATE;
    }
}
