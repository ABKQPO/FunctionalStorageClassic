package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaTransfer;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.TransferUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import lombok.Getter;

/** Pulls configured resources from the adjacent or bound wireless endpoint. */
@Getter
public class PullingUpgradeItem extends AutomationUpgradeItem {

    private final boolean wireless;

    public PullingUpgradeItem(boolean wireless) {
        super(wireless ? "wireless_pulling_upgrade" : "pulling_upgrade", FunctionalStorageConfig.UPGRADES.upgradeTick);
        this.wireless = wireless;
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity source = resolveSource(tile, stack);
        if (source == null || source == tile) {
            return;
        }
        ForgeDirection access = accessSide(tile, stack, source);
        if (tile.getItemHandler() != null) {
            TransferUtil.pullItems(
                UpgradeSettings.itemStorage(tile.getItemHandler(), stack),
                source,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePullItems);
        }
        if (tile.getFluidHandler() != null) {
            TransferUtil.pullFluid(
                UpgradeSettings.fluidStorage(tile.getFluidHandler(), stack),
                source,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePullFluid);
        }
        if (tile.getAspectHandler() != null) {
            EssentiaTransfer.pull(
                UpgradeSettings.aspectStorage(tile.getAspectHandler(), stack),
                source,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePullAspect);
        }
    }

    @Nullable
    private TileEntity resolveSource(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (!wireless) {
            ForgeDirection side = UpgradeTargeting.targetDirection(tile, stack);
            return tile.getWorldObj()
                .getTileEntity(tile.xCoord + side.offsetX, tile.yCoord + side.offsetY, tile.zCoord + side.offsetZ);
        }
        int[] target = getWirelessTarget(stack);
        if (target == null || stack.getTagCompound()
            .hasKey("WirelessDimension")
            && stack.getTagCompound()
                .getInteger("WirelessDimension") != tile.getWorldObj().provider.dimensionId
            || !tile.getWorldObj()
                .blockExists(target[0], target[1], target[2])) {
            return null;
        }
        return tile.getWorldObj()
            .getTileEntity(target[0], target[1], target[2]);
    }

    @Nonnull
    private ForgeDirection accessSide(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack,
        @Nonnull TileEntity source) {
        if (!wireless) {
            return UpgradeTargeting.targetDirection(tile, stack)
                .getOpposite();
        }
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("WirelessSide") ? ForgeDirection.getOrientation(
                stack.getTagCompound()
                    .getInteger("WirelessSide"))
                : ForgeDirection.UNKNOWN;
    }

    public void setWirelessTarget(@Nonnull ItemStack stack, int x, int y, int z) {
        tagOf(stack).setIntArray("WirelessTarget", new int[] { x, y, z });
    }

    @Nullable
    public int[] getWirelessTarget(@Nonnull ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("WirelessTarget")) {
            return null;
        }
        int[] target = tag.getIntArray("WirelessTarget");
        return target.length == 3 ? target : null;
    }
}
