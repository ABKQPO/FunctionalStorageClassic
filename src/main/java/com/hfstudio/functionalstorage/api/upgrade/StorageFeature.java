package com.hfstudio.functionalstorage.api.upgrade;

/**
 * Non-numeric storage behavior enabled by an installed upgrade.
 */
public enum StorageFeature {
    /**
     * Removes the ordinary capacity ceiling.
     */
    MAX_CAPACITY,
    /**
     * Exposes an inexhaustible template and does not consume stored resources.
     */
    CREATIVE,
    /**
     * Accepts compatible input beyond the physical capacity.
     */
    VOID_OVERFLOW,
    /**
     * Allows item templates considered equivalent by the implementation.
     */
    EQUIVALENT_ITEMS
}
