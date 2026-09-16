package com.hfstudio.functionalstorage.common.inventory.adapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/**
 * Exposes a long-capacity fluid drawer through Forge's 1.7.10
 * {@link IFluidHandler}, so fluid pipes and machines can fill and drain it.
 * Forge's per-side argument is accepted and ignored because every tank is
 * reachable from every side.
 */
public class DrawerFluidHandler implements IFluidHandler {

    private final IBigFluidHandler handler;

    public DrawerFluidHandler(@Nonnull IBigFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }
        TransferResult<BigFluidStack, FluidStorageKey> result = handler
            .fillRouted(new BigFluidStack(resource, resource.amount), StorageAction.fromSimulation(!doFill));
        long processed = Math.min(resource.amount, Math.max(0L, result.getProcessedAmount()));
        return processed >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) processed;
    }

    @Nullable
    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return null;
        }
        TransferResult<BigFluidStack, FluidStorageKey> result = handler
            .drainRouted(new BigFluidStack(resource, resource.amount), StorageAction.fromSimulation(!doDrain));
        long processed = Math.min(resource.amount, Math.max(0L, result.getProcessedAmount()));
        return processed == 0L ? null
            : result.getProcessed()
                .withAmount(processed)
                .toFluidStack();
    }

    @Nullable
    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        TransferResult<BigFluidStack, FluidStorageKey> result = handler
            .drainRouted(maxDrain, StorageAction.fromSimulation(!doDrain));
        long processed = Math.min(maxDrain, Math.max(0L, result.getProcessedAmount()));
        return processed == 0L ? null
            : result.getProcessed()
                .withAmount(processed)
                .toFluidStack();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        int count = Math.max(0, handler.getStorageCount());
        FluidTankInfo[] info = new FluidTankInfo[count];
        for (int index = 0; index < count; index++) {
            FluidStack contents = handler.getSnapshot(index)
                .toFluidStack();
            long capacity = Math.max(0L, handler.getCapacity(index));
            int saturated = capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
            info[index] = new FluidTankInfo(contents, saturated);
        }
        return info;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        for (int index = 0; index < Math.max(0, handler.getStorageCount()); index++) {
            if (!handler.supportsFill(index)) {
                continue;
            }
            BigFluidStack probe = new BigFluidStack(new FluidStack(fluid, 1), 1L);
            if (handler.supportsFluid(index, probe)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        for (int index = 0; index < Math.max(0, handler.getStorageCount()); index++) {
            if (!handler.supportsDrain(index)) {
                continue;
            }
            BigFluidStack snapshot = handler.getSnapshot(index);
            if (snapshot.isSameType(new FluidStack(fluid, 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the wrapped drawer tank handler
     */
    @Nonnull
    public IBigFluidHandler getHandler() {
        return handler;
    }
}
