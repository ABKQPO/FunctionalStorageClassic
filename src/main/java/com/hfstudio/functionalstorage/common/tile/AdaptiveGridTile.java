package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.ControllerExtensionTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;

import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.IEssentiaTransport;

public class AdaptiveGridTile extends ControllerExtensionTile {

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
    public void onBlockBroken() {
        if (worldObj != null && !worldObj.isRemote) {
            int[] position = getAdaptiveMatrixPosition();
            if (position != null && worldObj.blockExists(position[0], position[1], position[2])
                && worldObj.getTileEntity(position[0], position[1], position[2]) instanceof AdaptiveMatrixTile matrix) {
                matrix.linkDrawer(xCoord, yCoord, zCoord, true);
            }
        }
        super.onBlockBroken();
    }

    @Override
    @Nullable
    public IBigItemHandler getItemHandler() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getItemHandler();
    }

    @Override
    @Nullable
    public IBigFluidHandler getFluidHandler() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getFluidHandler();
    }

    @Override
    @Nullable
    public IBigAspectHandler getAspectHandler() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getAspectHandler();
    }

    @Override
    @Nullable
    public IInventory getInventoryView() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getInventoryView();
    }

    @Override
    @Nullable
    public IFluidHandler getForgeFluidHandler() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getForgeFluidHandler();
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    @Nullable
    public IEssentiaTransport getEssentiaTransport() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? null : matrix.getEssentiaTransport();
    }

    @Override
    protected boolean opensGuiOnEmptyHand() {
        return true;
    }

    @Override
    public boolean openGui(@Nonnull EntityPlayer player) {
        if (worldObj == null || worldObj.isRemote) return false;
        player.openGui(FunctionalStorage.instance, GuiHandler.GUI_DRAWER, worldObj, xCoord, yCoord, zCoord);
        return true;
    }

    @Nonnull
    public String getTerminalMode() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix == null ? AdaptiveMatrixTile.TERMINAL_ITEMS : matrix.getTerminalMode();
    }

    public void cycleTerminalMode() {
        AdaptiveMatrixTile matrix = matrix();
        if (matrix != null) matrix.cycleTerminalMode();
    }

    public void cyclePresentationSort() {
        AdaptiveMatrixTile matrix = matrix();
        if (matrix != null) matrix.cyclePresentationSort();
    }

    public void cyclePresentationLayout() {
        AdaptiveMatrixTile matrix = matrix();
        if (matrix != null) matrix.cyclePresentationLayout();
    }

    public void cyclePresentationOrder() {
        AdaptiveMatrixTile matrix = matrix();
        if (matrix != null) matrix.cyclePresentationOrder();
    }

    public boolean isTerminalItems() {
        return AdaptiveMatrixTile.TERMINAL_ITEMS.equals(getTerminalMode());
    }

    public boolean isTerminalFluids() {
        return AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(getTerminalMode());
    }

    public boolean isTerminalAspects() {
        return AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(getTerminalMode());
    }

    public void tryAutoLink() {
        AdaptiveLinking.tryLink(this);
    }

    @Override
    protected int getClientModelKey() {
        AdaptiveMatrixTile matrix = matrix();
        return matrix != null && matrix.isPowered() ? 1 : 0;
    }

    @Override
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public int getUtilityUpgradeSlots() {
        return 0;
    }

    private AdaptiveMatrixTile matrix() {
        return AdaptiveLinking.resolveMatrix(this);
    }

    @Nullable
    public AdaptiveMatrixTile getMatrix() {
        return matrix();
    }
}
