package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import java.util.function.Supplier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Adapts indexed essentia storage to Thaumcraft's suction and tube transfer protocol. */
public class DrawerEssentiaTransport implements IEssentiaTransport {

    private final TileEntity owner;
    private final Supplier<IBigAspectHandler> storage;

    public DrawerEssentiaTransport(TileEntity owner, Supplier<IBigAspectHandler> storage) {
        this.owner = owner;
        this.storage = storage;
    }

    public void tick() {
        World world = owner.getWorldObj();
        if (world == null || world.isRemote || world.getTotalWorldTime() % 5L != 0L) {
            return;
        }
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            int suction = getSuctionAmount(side);
            if (suction == 0) {
                continue;
            }
            TileEntity neighbor = world
                .getTileEntity(owner.xCoord + side.offsetX, owner.yCoord + side.offsetY, owner.zCoord + side.offsetZ);
            ForgeDirection opposite = side.getOpposite();
            if (!(neighbor instanceof IEssentiaTransport source) || !source.canOutputTo(opposite)
                || source.getSuctionAmount(opposite) >= suction
                || source.getMinimumSuction() > suction) {
                continue;
            }
            Aspect aspect = source.getEssentiaType(opposite);
            if (aspect != null && storage.get()
                .insertRouted(new BigAspectStack(aspect, 1L), StorageAction.SIMULATE)
                .isComplete()) {
                int extracted = source.takeEssentia(aspect, 1, opposite);
                if (extracted > 0) {
                    addEssentia(aspect, extracted, side);
                }
            }
        }
    }

    @Override
    public boolean isConnectable(ForgeDirection side) {
        return side != null && side != ForgeDirection.UNKNOWN;
    }

    @Override
    public boolean canInputFrom(ForgeDirection side) {
        return isConnectable(side);
    }

    @Override
    public boolean canOutputTo(ForgeDirection side) {
        return isConnectable(side);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Storage determines its own suction, just like a Thaumcraft jar.
    }

    @Override
    public Aspect getSuctionType(ForgeDirection side) {
        IBigAspectHandler handler = storage.get();
        for (int slot = 0; slot < handler.getStorageCount(); slot++) {
            BigAspectStack snapshot = handler.getSnapshot(slot);
            if (!snapshot.hasTemplate() && !handler.isLocked()) {
                return null;
            }
        }
        for (int slot = 0; slot < handler.getStorageCount(); slot++) {
            BigAspectStack snapshot = handler.getSnapshot(slot);
            if (snapshot.getAspect() != null && handler.insert(slot, snapshot.withAmount(1L), StorageAction.SIMULATE)
                .isComplete()) {
                return snapshot.getAspect();
            }
        }
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection side) {
        if (!canInputFrom(side)) {
            return 0;
        }
        IBigAspectHandler handler = storage.get();
        for (int slot = 0; slot < handler.getStorageCount(); slot++) {
            BigAspectStack snapshot = handler.getSnapshot(slot);
            if (!snapshot.hasTemplate() && !handler.isLocked() && handler.getCapacity(slot) > 0L) {
                return 32;
            }
            if (snapshot.hasTemplate() && handler.insert(slot, snapshot.withAmount(1L), StorageAction.SIMULATE)
                .isComplete()) {
                return handler.isLocked() ? 64 : 32;
            }
        }
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return canOutputTo(side) && aspect != null && amount > 0 ? (int) storage.get()
            .extractRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE)
            .getProcessedAmount() : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return canInputFrom(side) && aspect != null && amount > 0 ? (int) storage.get()
            .insertRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE)
            .getProcessedAmount() : 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection side) {
        IBigAspectHandler handler = storage.get();
        for (int slot = 0; slot < handler.getStorageCount(); slot++) {
            BigAspectStack snapshot = handler.getSnapshot(slot);
            if (!snapshot.isEmpty()) {
                return snapshot.getAspect();
            }
        }
        return null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection side) {
        Aspect aspect = getEssentiaType(side);
        return aspect == null ? 0
            : (int) storage.get()
                .extractRouted(new BigAspectStack(aspect, Integer.MAX_VALUE), StorageAction.SIMULATE)
                .getProcessedAmount();
    }

    @Override
    public int getMinimumSuction() {
        return 32;
    }

    @Override
    public boolean renderExtendedTube() {
        return false;
    }
}
