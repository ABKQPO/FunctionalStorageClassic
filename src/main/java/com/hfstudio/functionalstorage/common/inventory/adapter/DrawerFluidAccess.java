package com.hfstudio.functionalstorage.common.inventory.adapter;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public interface DrawerFluidAccess extends IFluidHandler {

    IFluidHandler getForgeFluidHandler();

    @Override
    default int fill(ForgeDirection from, FluidStack fluid, boolean execute) {
        return getForgeFluidHandler().fill(from, fluid, execute);
    }

    @Override
    default FluidStack drain(ForgeDirection from, FluidStack fluid, boolean execute) {
        return getForgeFluidHandler().drain(from, fluid, execute);
    }

    @Override
    default FluidStack drain(ForgeDirection from, int amount, boolean execute) {
        return getForgeFluidHandler().drain(from, amount, execute);
    }

    @Override
    default boolean canFill(ForgeDirection from, Fluid fluid) {
        return getForgeFluidHandler().canFill(from, fluid);
    }

    @Override
    default boolean canDrain(ForgeDirection from, Fluid fluid) {
        return getForgeFluidHandler().canDrain(from, fluid);
    }

    @Override
    default FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return getForgeFluidHandler().getTankInfo(from);
    }
}
