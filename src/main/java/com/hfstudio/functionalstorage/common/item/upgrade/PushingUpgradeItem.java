package com.hfstudio.functionalstorage.common.item.upgrade;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.TransferUtil;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

/**
 * Pushing upgrade. Moves items and fluids from the drawer into a neighbour.
 *
 * <p>The wired variant works on the block touching a chosen side of the drawer.
 * The wireless variant works on a coordinate recorded on the upgrade stack.</p>
 */
public class PushingUpgradeItem extends AutomationUpgradeItem {

    private static final String KEY_TARGET = "WirelessTarget";

    private final boolean wireless;

    public PushingUpgradeItem(boolean wireless) {
        super(
            wireless ? "wireless_pushing_upgrade" : "pushing_upgrade",
            FunctionalStorageConfig.UPGRADES.upgradeTick);
        this.wireless = wireless;
    }

    /**
     * @return whether this upgrade pushes to a recorded coordinate
     */
    public boolean isWireless() {
        return wireless;
    }

    @Override
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {
        World world = tile.getWorldObj();
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity target = resolveTarget(tile, stack);
        if (target == null) {
            return;
        }
        ForgeDirection access = wireless ? ForgeDirection.UNKNOWN
            : UpgradeTargeting.targetDirection(tile, stack);
        if (tile.getItemHandler() != null) {
            TransferUtil.pushItems(
                tile.getItemHandler(),
                target,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePushItems);
        }
        if (tile.getFluidHandler() != null) {
            TransferUtil.pushFluid(
                tile.getFluidHandler(),
                target,
                access,
                FunctionalStorageConfig.UPGRADES.upgradePushFluid);
        }
    }

    @Nullable
    private TileEntity resolveTarget(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack) {
        if (!wireless) {
            ForgeDirection side = UpgradeTargeting.targetDirection(tile, stack);
            return tile.getWorldObj()
                .getTileEntity(
                    tile.xCoord + side.offsetX,
                    tile.yCoord + side.offsetY,
                    tile.zCoord + side.offsetZ);
        }
        int[] target = getWirelessTarget(stack);
        return target == null ? null
            : tile.getWorldObj()
                .getTileEntity(target[0], target[1], target[2]);
    }

    /**
     * Records the coordinate this wireless upgrade should push to.
     *
     * @param stack upgrade stack
     * @param x     target x
     * @param y     target y
     * @param z     target z
     */
    public void setWirelessTarget(@Nonnull ItemStack stack, int x, int y, int z) {
        tagOf(stack).setIntArray(KEY_TARGET, new int[] { x, y, z });
    }

    /**
     * @param stack upgrade stack
     * @return the recorded coordinate, or {@code null} when unset
     */
    @Nullable
    public int[] getWirelessTarget(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        if (!tag.hasKey(KEY_TARGET)) {
            return null;
        }
        int[] target = tag.getIntArray(KEY_TARGET);
        return target.length == 3 ? target : null;
    }
}
