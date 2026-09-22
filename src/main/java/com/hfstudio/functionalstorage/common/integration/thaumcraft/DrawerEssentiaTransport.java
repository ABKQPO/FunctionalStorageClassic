package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import java.util.function.Supplier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

@Optional.Interface(iface = "thaumcraft.api.aspects.IEssentiaTransport", modid = "Thaumcraft", striprefs = true)
public class DrawerEssentiaTransport implements IEssentiaTransport {

    private final TileEntity owner;
    private final Supplier<IBigAspectHandler> storage;

    public DrawerEssentiaTransport(TileEntity owner, Supplier<IBigAspectHandler> storage) {
        this.owner = owner;
        this.storage = storage;
    }

    @Optional.Method(modid = "Thaumcraft")
    public void tick() {
        World world = owner.getWorldObj();
        if (world == null || world.isRemote || world.getTotalWorldTime() % 5L != 0L) {
            return;
        }
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity neighbor = world
                .getTileEntity(owner.xCoord + side.offsetX, owner.yCoord + side.offsetY, owner.zCoord + side.offsetZ);
            ForgeDirection opposite = side.getOpposite();
            if (!(neighbor instanceof IEssentiaTransport source) || !source.canOutputTo(opposite)) {
                continue;
            }
            int suction = getSuctionAmount(side);
            if (suction == 0 || source.getSuctionAmount(opposite) >= suction || source.getMinimumSuction() > suction) {
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
    @Optional.Method(modid = "Thaumcraft")
    public void setSuction(Aspect aspect, int amount) {
        // Storage determines its own suction, just like a Thaumcraft jar.
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public Aspect getSuctionType(ForgeDirection side) {
        return storage.get()
            .summary()
            .getSuctionType();
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public int getSuctionAmount(ForgeDirection side) {
        if (!canInputFrom(side)) {
            return 0;
        }
        return storage.get()
            .summary()
            .suctionAmount();
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return canOutputTo(side) && aspect != null && amount > 0 ? (int) storage.get()
            .extractRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE)
            .getProcessedAmount() : 0;
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return canInputFrom(side) && aspect != null && amount > 0 ? (int) storage.get()
            .insertRouted(new BigAspectStack(aspect, amount), StorageAction.EXECUTE)
            .getProcessedAmount() : 0;
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public Aspect getEssentiaType(ForgeDirection side) {
        for (Aspect aspect : storage.get()
            .summary()
            .getTotals()
            .keySet()) {
            return aspect;
        }
        return null;
    }

    @Override
    @Optional.Method(modid = "Thaumcraft")
    public int getEssentiaAmount(ForgeDirection side) {
        Aspect aspect = getEssentiaType(side);
        if (aspect == null) {
            return 0;
        }
        long total = storage.get()
            .summary()
            .getTotal(aspect);
        return (int) Math.min(Integer.MAX_VALUE, total);
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
