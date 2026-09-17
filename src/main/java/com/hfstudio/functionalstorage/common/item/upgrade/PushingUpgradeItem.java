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

/** Pushes configured resources to the adjacent or bound wireless endpoint. */
@Getter
public class PushingUpgradeItem extends AutomationUpgradeItem {

    private static final String KEY_TARGET = "WirelessTarget";

    private final boolean wireless;

    public PushingUpgradeItem(boolean wireless) {
        super(wireless ? "wireless_pushing_upgrade" : "pushing_upgrade", FunctionalStorageConfig.UPGRADES.upgradeTick);
        this.wireless = wireless;
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity target = resolveTarget(tile, stack);
        if (target == null || target == tile) {
            return;
        }
        ForgeDirection access = wireless ? (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey("WirelessSide") ? ForgeDirection.getOrientation(
                stack.getTagCompound()
                    .getInteger("WirelessSide"))
                : ForgeDirection.UNKNOWN)
            : UpgradeTargeting.targetDirection(tile, stack)
                .getOpposite();
        if (tile.getItemHandler() != null) {
            TransferUtil.pushItems(
                UpgradeSettings.itemStorage(tile.getItemHandler(), stack),
                target,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePushItems);
        }
        if (tile.getFluidHandler() != null) {
            TransferUtil.pushFluid(
                UpgradeSettings.fluidStorage(tile.getFluidHandler(), stack),
                target,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePushFluid);
        }
        if (tile.getAspectHandler() != null) {
            EssentiaTransfer.push(
                UpgradeSettings.aspectStorage(tile.getAspectHandler(), stack),
                target,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePushAspect);
        }
    }

    @Nullable
    private TileEntity resolveTarget(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (!wireless) {
            ForgeDirection side = UpgradeTargeting.targetDirection(tile, stack);
            return tile.getWorldObj()
                .getTileEntity(tile.xCoord + side.offsetX, tile.yCoord + side.offsetY, tile.zCoord + side.offsetZ);
        }
        int[] target = getWirelessTarget(stack);
        return target == null || stack.getTagCompound()
            .hasKey("WirelessDimension")
            && stack.getTagCompound()
                .getInteger("WirelessDimension") != tile.getWorldObj().provider.dimensionId
            || !tile.getWorldObj()
                .blockExists(target[0], target[1], target[2]) ? null
                    : tile.getWorldObj()
                        .getTileEntity(target[0], target[1], target[2]);
    }

    public void setWirelessTarget(@Nonnull ItemStack stack, int x, int y, int z) {
        tagOf(stack).setIntArray(KEY_TARGET, new int[] { x, y, z });
    }

    @Nullable
    public int[] getWirelessTarget(@Nonnull ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_TARGET)) {
            return null;
        }
        int[] target = tag.getIntArray(KEY_TARGET);
        return target.length == 3 ? target : null;
    }
}
