package com.hfstudio.functionalstorage.api.upgrade;

/**
 * Numeric storage properties that installed upgrades may modify.
 *
 * <p>
 * The enum values are stable API identifiers. Consumers supply the unmodified base value
 * when evaluating an attribute through {@link UpgradeState#calculate(UpgradeAttribute, double)}.
 * </p>
 */
public enum UpgradeAttribute {
    /**
     * Capacity of an item-storage slot.
     */
    ITEM_CAPACITY,
    /**
     * Capacity of a fluid-storage tank.
     */
    FLUID_CAPACITY,
    /**
     * Capacity of an essentia-storage slot.
     */
    ASPECT_CAPACITY,
    /**
     * Additional controller discovery range.
     */
    CONTROLLER_RANGE
}
