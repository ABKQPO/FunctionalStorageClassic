package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Active automation respects native ports; passive tube suction remains owned by the transport adapter. */
public class EssentiaTransfer {

    private EssentiaTransfer() {}

    public static boolean pull(@Nonnull IBigAspectHandler drawer, @Nullable TileEntity source,
        @Nonnull ForgeDirection side, int limit) {
        if (source == null || limit <= 0) {
            return false;
        }
        if (source instanceof IEssentiaTransport transport) {
            if (side != ForgeDirection.UNKNOWN) {
                return pullThroughPort(drawer, transport, side, limit);
            }
            for (ForgeDirection port : ForgeDirection.VALID_DIRECTIONS) {
                if (pullThroughPort(drawer, transport, port, limit)) {
                    return true;
                }
            }
            return false;
        }
        if (source instanceof IAspectContainer container) {
            AspectList contents = container.getAspects();
            if (contents == null) {
                return false;
            }
            for (Aspect aspect : contents.getAspects()) {
                int accepted = acceptedAmount(drawer, aspect, Math.min(limit, contents.getAmount(aspect)));
                if (accepted > 0 && container.takeFromContainer(aspect, accepted)) {
                    drawer.insertRouted(new BigAspectStack(aspect, accepted), StorageAction.EXECUTE);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean push(@Nonnull IBigAspectHandler drawer, @Nullable TileEntity target,
        @Nonnull ForgeDirection side, int limit) {
        if (target == null || limit <= 0) {
            return false;
        }
        for (int slot = 0; slot < drawer.getStorageCount(); slot++) {
            BigAspectStack stored = drawer.getSnapshot(slot);
            if (stored.isEmpty()) {
                continue;
            }
            int available = (int) drawer.extract(slot, Math.min(limit, stored.getAmount()), StorageAction.SIMULATE)
                .getProcessedAmount();
            if (available == 0) {
                continue;
            }
            int accepted = offer(target, side, stored.getAspect(), available);
            if (accepted > 0) {
                drawer.extract(slot, accepted, StorageAction.EXECUTE);
                return true;
            }
        }
        return false;
    }

    private static boolean pullThroughPort(IBigAspectHandler drawer, IEssentiaTransport source, ForgeDirection side,
        int limit) {
        if (!source.isConnectable(side) || !source.canOutputTo(side)) {
            return false;
        }
        Aspect aspect = source.getEssentiaType(side);
        int accepted = acceptedAmount(drawer, aspect, Math.min(limit, source.getEssentiaAmount(side)));
        if (accepted <= 0) {
            return false;
        }
        int taken = source.takeEssentia(aspect, accepted, side);
        if (taken <= 0) {
            return false;
        }
        drawer.insertRouted(new BigAspectStack(aspect, taken), StorageAction.EXECUTE);
        return true;
    }

    private static int acceptedAmount(IBigAspectHandler drawer, Aspect aspect, int amount) {
        return aspect == null || amount <= 0 ? 0
            : (int) drawer.insertRouted(new BigAspectStack(aspect, amount), StorageAction.SIMULATE)
                .getProcessedAmount();
    }

    private static int offer(TileEntity target, ForgeDirection side, Aspect aspect, int amount) {
        if (target instanceof IEssentiaTransport transport) {
            if (side != ForgeDirection.UNKNOWN) {
                return offerThroughPort(transport, side, aspect, amount);
            }
            for (ForgeDirection port : ForgeDirection.VALID_DIRECTIONS) {
                int accepted = offerThroughPort(transport, port, aspect, amount);
                if (accepted > 0) {
                    return accepted;
                }
            }
        } else if (target instanceof IAspectContainer container && container.doesContainerAccept(aspect)) {
            return amount - container.addToContainer(aspect, amount);
        }
        return 0;
    }

    private static int offerThroughPort(IEssentiaTransport target, ForgeDirection side, Aspect aspect, int amount) {
        return target.isConnectable(side) && target.canInputFrom(side) ? target.addEssentia(aspect, amount, side) : 0;
    }
}
