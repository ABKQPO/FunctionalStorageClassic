package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.TransferUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import lombok.Getter;

/**
 * Pulling upgrade. Moves items and fluids from a neighbour into the drawer.
 *
 * <p>
 * The wired variant works on the block touching a chosen side of the drawer.
 * The wireless variant works on a coordinate recorded on the upgrade stack, which
 * lets a drawer pull from a container it is not touching.
 * </p>
 */
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
        if (source == null) {
            return;
        }
        ForgeDirection access = accessSide(tile, stack, source);
        if (tile.getItemHandler() != null) {
            TransferUtil
                .pullItems(tile.getItemHandler(), source, access, FunctionalStorageConfig.UPGRADES.upgradePullItems);
        }
        if (tile.getFluidHandler() != null) {
            TransferUtil
                .pullFluid(tile.getFluidHandler(), source, access, FunctionalStorageConfig.UPGRADES.upgradePullFluid);
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
        if (target == null) {
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
        return ForgeDirection.UNKNOWN;
    }

    /**
     * Records the coordinate this wireless upgrade should pull from.
     *
     * @param stack upgrade stack
     * @param x     target x
     * @param y     target y
     * @param z     target z
     */
    public void setWirelessTarget(@Nonnull ItemStack stack, int x, int y, int z) {
        tagOf(stack).setIntArray("WirelessTarget", new int[] { x, y, z });
    }

    /**
     * @param stack upgrade stack
     * @return the recorded coordinate, or {@code null} when unset
     */
    @Nullable
    public int[] getWirelessTarget(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        if (!tag.hasKey("WirelessTarget")) {
            return null;
        }
        int[] target = tag.getIntArray("WirelessTarget");
        return target.length == 3 ? target : null;
    }
}
