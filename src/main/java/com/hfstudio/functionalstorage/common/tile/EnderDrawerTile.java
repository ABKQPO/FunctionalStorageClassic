package com.hfstudio.functionalstorage.common.tile;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.inventory.EnderItemHandler;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerInventoryAccess;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.world.EnderSavedData;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

/** Bound frequencies use per-save shared storage; unbound drawers retain local contents. */
public class EnderDrawerTile extends ControllableDrawerTile implements DrawerInventoryAccess {

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

    public UUID getOrCreateFrequency() {
        if (frequency == null) {
            NBTTagCompound contents = handler.serializeNBT();
            boolean previousLock = isLocked();
            boolean previousVoid = voidsOverflow();
            setFrequency(UUID.randomUUID());
            handler.deserializeNBT(contents);
            ((EnderItemHandler) handler).setLocked(previousLock);
            if (previousVoid) ((EnderItemHandler) handler).enableVoiding();
        }
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
        if (handler instanceof EnderItemHandler shared) shared.writePolicy(tag);
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
        if (handler instanceof EnderItemHandler shared && (frequency == null || worldObj == null || worldObj.isRemote))
            shared.readPolicy(tag);
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
        ((EnderItemHandler) handler).initializePolicy(super.isLocked());
        bindStorageHandler(handler);
    }

    @Override
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public boolean isLocked() {
        return handler instanceof EnderItemHandler shared ? shared.isLocked() : super.isLocked();
    }

    @Override
    public void setLocked(boolean locked) {
        if (handler instanceof EnderItemHandler shared) shared.setLocked(locked);
        super.setLocked(locked);
    }

    @Override
    public boolean voidsOverflow() {
        return handler instanceof EnderItemHandler shared ? shared.voidsOverflow() : super.voidsOverflow();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote
            || !(handler instanceof EnderItemHandler shared)
            || shared.voidsOverflow()) return;
        for (int slot = 0; slot < getUtilityUpgradeSlots(); slot++) {
            ItemStack stack = getUtilityUpgrade(slot);
            if (stack != null && stack.getItem() == RegistrationHandler.voidUpgrade) {
                shared.enableVoiding();
                setUpgradeSlot(false, slot, null);
                break;
            }
        }
    }

    private BigItemHandler createHandler() {
        return new EnderItemHandler(1);
    }
}
