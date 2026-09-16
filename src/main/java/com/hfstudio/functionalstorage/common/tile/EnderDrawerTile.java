package com.hfstudio.functionalstorage.common.tile;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.world.EnderSavedData;

/** Bound frequencies use per-save shared storage; unbound drawers retain local contents. */
public class EnderDrawerTile extends ControllableDrawerTile {

    private static final String KEY_FREQUENCY = "Frequency";

    private UUID frequency;
    private BigItemHandler handler;

    public EnderDrawerTile() {
        this.handler = createHandler();
        bindStorageHandler(handler);
    }

    @Nullable
    public UUID getFrequency() {
        return frequency;
    }

    /**
     * Rebinds this drawer to a frequency and re-attaches the shared handler.
     *
     * @param frequency new frequency, or {@code null} to unbind
     */
    public void setFrequency(@Nullable UUID frequency) {
        if (Objects.equals(frequency, this.frequency)) {
            return;
        }
        this.frequency = frequency;
        rebindToSharedHandler();
        markDirty();
        requestUpdatePacket();
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag("Items", handler.serializeNBT());
        if (frequency != null) {
            tag.setString(KEY_FREQUENCY, frequency.toString());
        }
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        if (tag.hasKey(KEY_FREQUENCY)) {
            try {
                frequency = UUID.fromString(tag.getString(KEY_FREQUENCY));
            } catch (IllegalArgumentException ignored) {
                frequency = null;
            }
        } else {
            frequency = null;
        }
        rebindToSharedHandler();
        if (frequency == null || worldObj == null || worldObj.isRemote) {
            handler.deserializeNBT(tag.hasKey("Items", 10) ? tag.getCompoundTag("Items") : null);
        }
    }

    @Override
    public void validate() {
        super.validate();
        if (frequency != null && worldObj != null && !worldObj.isRemote) {
            rebindToSharedHandler();
        }
    }

    @Override
    protected void reconcileStorageConfiguration() {
        handler.applyLockConfiguration(isLocked());
    }

    @Override
    protected int calculateRedstoneSignal() {
        long capacity = 0L;
        long stored = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            capacity += handler.getCapacity(index);
            stored += handler.getSnapshot(index)
                .getAmount();
        }
        return capacity <= 0L ? 0 : redstoneForRatio(stored / (double) capacity);
    }

    @Override
    public void onChunkUnload() {
        closeStorageSubscription();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        closeStorageSubscription();
        super.invalidate();
    }

    private void rebindToSharedHandler() {
        if (worldObj == null || worldObj.isRemote || frequency == null) {
            handler = createHandler();
            bindStorageHandler(handler);
            return;
        }
        handler = EnderSavedData.dataFor(worldObj)
            .handlerFor(frequency, 1);
        bindStorageHandler(handler);
    }

    private BigItemHandler createHandler() {
        return new BigItemHandler(1) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ITEM_CAPACITY, 1D);
            }

            @Override
            protected boolean allowsEquivalentItems() {
                return EnderDrawerTile.this.hasEquivalentItems();
            }

            @Override
            public boolean isLocked() {
                return EnderDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return EnderDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return EnderDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return EnderDrawerTile.this.hasMaxStorage();
            }
        };
    }
}
