package com.hfstudio.functionalstorage.api.storage;

/**
 * Selects whether a storage operation changes state or only reports what it
 * could do. Implementations must treat {@link #SIMULATE} as completely
 * side-effect free, including filters, NBT, and change notifications.
 */
public enum StorageAction {

    EXECUTE(false, false),
    SIMULATE(true, false),
    AUTOMATION_EXECUTE(false, true),
    AUTOMATION_SIMULATE(true, true);

    private final boolean simulation;
    private final boolean automation;

    StorageAction(boolean simulation, boolean automation) {
        this.simulation = simulation;
        this.automation = automation;
    }

    public static StorageAction fromSimulation(boolean simulate) {
        return simulate ? SIMULATE : EXECUTE;
    }

    public static StorageAction forAutomation(boolean simulate) {
        return simulate ? AUTOMATION_SIMULATE : AUTOMATION_EXECUTE;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public boolean isExecute() {
        return !simulation;
    }

    public boolean isAutomation() {
        return automation;
    }
}
