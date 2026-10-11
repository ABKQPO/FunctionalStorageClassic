package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.inventory.adapter.AdaptiveDepositHandlers;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.ControllerExtensionTile;

public class AdaptiveDepositTile extends ControllerExtensionTile {

    private final AdaptiveDepositHandlers.Items depositHandler = new AdaptiveDepositHandlers.Items(
        () -> matrix() == null ? null : matrix().getItemHandler());
    private final AdaptiveDepositHandlers.Fluids depositFluids = new AdaptiveDepositHandlers.Fluids(
        () -> matrix() == null ? null : matrix().getFluidHandler());
    private final AdaptiveDepositHandlers.Aspects depositAspects = Mods.Thaumcraft.isModLoaded()
        ? new AdaptiveDepositHandlers.Aspects(() -> matrix() == null ? null : matrix().getAspectHandler())
        : null;
    private final DrawerItemInventory depositInventory = new DrawerItemInventory(
        depositHandler,
        "container.functionalstorage.drawer",
        this::markDirty);
    private final DrawerFluidHandler fluidPort = new DrawerFluidHandler(depositFluids);
    private boolean automaticLinkChecked;

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            tryAutoLink();
        }
        super.updateEntity();
    }

    @Override
    @Nonnull
    public IBigItemHandler getItemHandler() {
        return depositHandler;
    }

    @Override
    public IInventory getInventoryView() {
        return depositInventory;
    }

    @Override
    public IBigFluidHandler getFluidHandler() {
        return depositFluids;
    }

    @Override
    public IFluidHandler getForgeFluidHandler() {
        return fluidPort;
    }

    @Override
    public IBigAspectHandler getAspectHandler() {
        return depositAspects;
    }

    @Override
    public boolean openGui(EntityPlayer player) {
        return false;
    }

    public void tryAutoLink() {
        AdaptiveLinking.tryLink(this);
    }

    @Override
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public int getUtilityUpgradeSlots() {
        return 0;
    }

    @Override
    protected void writeStorageData(NBTTagCompound tag) {}

    @Override
    protected void readStorageData(NBTTagCompound tag) {}

    private AdaptiveMatrixTile matrix() {
        int[] position = getAdaptiveMatrixPosition();
        if (worldObj == null || position == null || !worldObj.blockExists(position[0], position[1], position[2])) {
            return null;
        }
        TileEntity tile = worldObj.getTileEntity(position[0], position[1], position[2]);
        return tile instanceof AdaptiveMatrixTile matrix ? matrix : null;
    }
}
