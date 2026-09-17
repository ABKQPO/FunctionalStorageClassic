package com.hfstudio.functionalstorage.common.inventory;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;

public class EnderItemHandler extends BigItemHandler {

    private boolean initialized;
    private boolean locked;
    private boolean voiding;

    public EnderItemHandler(int slots) {
        super(slots);
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public boolean voidsOverflow() {
        return voiding;
    }

    public void initializePolicy(boolean value) {
        if (!initialized) {
            initialized = true;
            setLocked(value);
        }
    }

    public void setLocked(boolean value) {
        if (locked == value) return;
        locked = value;
        applyLockConfiguration(value);
    }

    public void enableVoiding() {
        if (voiding) return;
        voiding = true;
        applyLockConfiguration(locked);
    }

    public void writePolicy(NBTTagCompound tag) {
        tag.setBoolean("PolicyInitialized", initialized);
        tag.setBoolean("SharedLocked", locked);
        tag.setBoolean("SharedVoid", voiding);
    }

    public void readPolicy(NBTTagCompound tag) {
        initialized = tag.getBoolean("PolicyInitialized");
        locked = tag.hasKey("SharedLocked") ? tag.getBoolean("SharedLocked") : tag.getBoolean("Locked");
        voiding = tag.getBoolean("SharedVoid");
    }
}
