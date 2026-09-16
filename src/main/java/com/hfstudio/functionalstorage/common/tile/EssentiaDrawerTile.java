package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

/**
 * Essentia drawer tile. Stores Thaumcraft essentia with the same long-capacity
 * semantics as item and fluid drawers, and adapts the generic handler to
 * Thaumcraft's integer based {@link IAspectContainer} contract.
 */
public class EssentiaDrawerTile extends ControllableDrawerTile implements IAspectContainer {

    private static final String KEY_ASPECTS = "Aspects";

    private final DrawerLayout layout;
    private final BigAspectHandler handler;

    public EssentiaDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public EssentiaDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = new BigAspectHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ASPECT_CAPACITY, 1D);
            }

            @Override
            public boolean isLocked() {
                return EssentiaDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return EssentiaDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return EssentiaDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return EssentiaDrawerTile.this.hasMaxStorage();
            }
        };
        bindStorageHandler(handler);
    }

    /**
     * @return the slot layout of this drawer
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    @Override
    public IBigAspectHandler getAspectHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_ASPECTS, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_ASPECTS, 10) ? tag.getCompoundTag(KEY_ASPECTS) : null);
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
    public AspectList getAspects() {
        AspectList list = new AspectList();
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            Aspect aspect = snapshot.getAspect();
            if (aspect != null && !snapshot.isEmpty()) {
                list.add(aspect, snapshot.toAmount());
            }
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            handler.extract(index, Long.MAX_VALUE, StorageAction.EXECUTE);
        }
        if (aspects == null) {
            return;
        }
        for (Aspect aspect : aspects.getAspects()) {
            handler.insertRouted(new BigAspectStack(aspect, aspects.getAmount(aspect)), StorageAction.EXECUTE);
        }
        markDirty();
        requestUpdatePacket();
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        if (aspect == null) {
            return false;
        }
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (!handler.supportsAspect(index, aspect)) {
                continue;
            }
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.isEmpty() || snapshot.isSameType(aspect)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int addToContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return amount;
        }
        TransferResult<BigAspectStack, AspectStorageKey> result = handler
            .insertRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE);
        return (int) Math.min(Integer.MAX_VALUE, result.getRemainingAmount());
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return false;
        }
        TransferResult<BigAspectStack, AspectStorageKey> result = handler
            .extractRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE);
        return result.isComplete();
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int amount) {
        return aspect != null && amount > 0 && containerContains(aspect) >= amount;
    }

    @Override
    public int containerContains(Aspect aspect) {
        if (aspect == null) {
            return 0;
        }
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.isSameType(aspect)) {
                total += snapshot.getAmount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean takeFromContainer(AspectList aspects) {
        if (aspects == null) {
            return false;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (!doesContainerContainAmount(aspect, aspects.getAmount(aspect))) {
                return false;
            }
        }
        for (Aspect aspect : aspects.getAspects()) {
            takeFromContainer(aspect, aspects.getAmount(aspect));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean doesContainerContain(AspectList aspects) {
        if (aspects == null) {
            return false;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (!doesContainerContainAmount(aspect, aspects.getAmount(aspect))) {
                return false;
            }
        }
        return true;
    }
}
