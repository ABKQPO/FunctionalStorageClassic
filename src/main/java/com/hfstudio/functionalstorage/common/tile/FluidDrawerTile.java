package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Fluid drawer tile. Holds one long-capacity tank per layout slot and exposes
 * it through the generic fluid capability.
 *
 * <p>
 * The tile implements Forge's {@link IFluidHandler} directly so fluid pipes
 * and machines can find it without a capability lookup.
 * </p>
 */
public class FluidDrawerTile extends ControllableDrawerTile implements IFluidHandler {

    private static final String KEY_TANKS = "Tanks";

    private final DrawerLayout layout;
    private final BigFluidHandler handler;
    private final DrawerFluidHandler fluidHandler;

    public FluidDrawerTile() {
        this(DrawerLayout.X_1);
    }

    public FluidDrawerTile(@Nonnull DrawerLayout layout) {
        this.layout = layout;
        this.handler = new BigFluidHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.FLUID_CAPACITY, 1D);
            }

            @Override
            public boolean isLocked() {
                return FluidDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return FluidDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return FluidDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return FluidDrawerTile.this.hasMaxStorage();
            }
        };
        this.fluidHandler = new DrawerFluidHandler(handler);
        bindStorageHandler(handler);
    }

    /**
     * @return the Forge fluid handler view used by pipes and machines
     */
    @Nonnull
    public DrawerFluidHandler getForgeFluidHandler() {
        return fluidHandler;
    }

    /**
     * @return the slot layout of this drawer
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return fluidHandler.fill(from, resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return fluidHandler.drain(from, resource, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return fluidHandler.drain(from, maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluidHandler.canFill(from, fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return fluidHandler.canDrain(from, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return fluidHandler.getTankInfo(from);
    }

    @Nonnull
    @Override
    public IBigFluidHandler getFluidHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_TANKS, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_TANKS, 10) ? tag.getCompoundTag(KEY_TANKS) : null);
    }

    @Override
    protected int calculateRedstoneSignal() {
        return redstoneForRatio(fillRatio());
    }

    @Override
    protected void reconcileStorageConfiguration() {
        handler.applyLockConfiguration(isLocked());
    }

    private double fillRatio() {
        long total = 0L;
        long capacity = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
            capacity += handler.getCapacity(index);
        }
        return capacity <= 0L ? 0D : Math.min(1D, total / (double) capacity);
    }
}
