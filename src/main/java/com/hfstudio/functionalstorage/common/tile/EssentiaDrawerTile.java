package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerEssentiaTransport;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Adapts long-capacity essentia storage to Thaumcraft containers and tubes. */
public class EssentiaDrawerTile extends ControllableDrawerTile implements IAspectContainer, IEssentiaTransport {

    private static final String KEY_ASPECTS = "Aspects";

    private DrawerLayout layout;
    private BigAspectHandler handler;
    private final DrawerEssentiaTransport transport = new DrawerEssentiaTransport(this, this::getAspectHandler);

    public EssentiaDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public EssentiaDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = createHandler();
        bindStorageHandler(handler);
    }

    private BigAspectHandler createHandler() {
        return new BigAspectHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ASPECT_CAPACITY, 1D) / layout.getSlotCount();
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
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Override
    public boolean onSlotActivated(@Nonnull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        return EssentiaContainerRegistry.activate(player, handler, slot)
            || super.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
    }

    @Override
    public void onSlotClicked(@Nonnull EntityPlayer player, int slot) {
        if (worldObj != null && !worldObj.isRemote) {
            EssentiaContainerRegistry.activate(player, handler, slot);
        }
    }

    @Nonnull
    @Override
    public IBigAspectHandler getAspectHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setString("DrawerLayout", layout.getId());
        tag.setTag(KEY_ASPECTS, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        DrawerLayout restored = DrawerLayout.fromStorage(tag, KEY_ASPECTS, layout);
        if (restored != layout) {
            layout = restored;
            handler = createHandler();
            bindStorageHandler(handler);
        }

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
        if (!handler.extractRouted(new BigAspectStack(aspect, amount), StorageAction.SIMULATE)
            .isComplete()) {
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

    @Override
    public void updateEntity() {
        super.updateEntity();
        transport.tick();
    }

    @Override
    public boolean isConnectable(ForgeDirection side) {
        return transport.isConnectable(side);
    }

    @Override
    public boolean canInputFrom(ForgeDirection side) {
        return transport.canInputFrom(side);
    }

    @Override
    public boolean canOutputTo(ForgeDirection side) {
        return transport.canOutputTo(side);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        transport.setSuction(aspect, amount);
    }

    @Override
    public Aspect getSuctionType(ForgeDirection side) {
        return transport.getSuctionType(side);
    }

    @Override
    public int getSuctionAmount(ForgeDirection side) {
        return transport.getSuctionAmount(side);
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return transport.takeEssentia(aspect, amount, side);
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return transport.addEssentia(aspect, amount, side);
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection side) {
        return transport.getEssentiaType(side);
    }

    @Override
    public int getEssentiaAmount(ForgeDirection side) {
        return transport.getEssentiaAmount(side);
    }

    @Override
    public int getMinimumSuction() {
        return transport.getMinimumSuction();
    }

    @Override
    public boolean renderExtendedTube() {
        return transport.renderExtendedTube();
    }

}
