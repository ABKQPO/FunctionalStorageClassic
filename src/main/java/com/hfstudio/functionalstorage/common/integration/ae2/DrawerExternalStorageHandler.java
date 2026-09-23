package com.hfstudio.functionalstorage.common.integration.ae2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IExternalStorageHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStackType;
import appeng.util.item.AEItemStackType;
import cpw.mods.fml.common.Optional;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/**
 * Lets an AE2 storage bus read and write a drawer without the drawer being an
 * AE2 grid host.
 *
 * <p>
 * The monitors themselves are held by {@link DrawerBridgeCache}, which hands one
 * monitor per drawer to every bus that reads it. A fresh monitor per request
 * would instead leave an abandoned subscription behind on every bus rebuild,
 * each still delivering events to a discarded object.
 * </p>
 */
@Optional.Interface(
    iface = "appeng.api.storage.IExternalStorageHandler",
    modid = "appliedenergistics2",
    striprefs = true)
public class DrawerExternalStorageHandler implements IExternalStorageHandler {

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean canHandle(TileEntity te, ForgeDirection d, StorageChannel channel, BaseActionSource mySrc) {
        return channel == StorageChannel.ITEMS && te instanceof ControllableDrawerTile
            && ((ControllableDrawerTile) te).getItemHandler() != null;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public IMEInventory getInventory(TileEntity te, ForgeDirection d, StorageChannel channel, BaseActionSource src) {
        if (!canHandle(te, d, channel, src)) {
            return null;
        }
        return DrawerBridgeCache.itemMonitor((ControllableDrawerTile) te);
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public boolean canHandle(TileEntity te, ForgeDirection d, IAEStackType<?> type, BaseActionSource mySrc) {
        if (!(te instanceof ControllableDrawerTile drawer)) {
            return false;
        }
        if (type == AEItemStackType.ITEM_STACK_TYPE) {
            return drawer.getItemHandler() != null;
        }
        return type == AEEssentiaStackType.ESSENTIA_STACK_TYPE && drawer.getAspectHandler() != null;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IMEInventory getInventory(TileEntity te, ForgeDirection d, IAEStackType<?> type, BaseActionSource src) {
        if (!canHandle(te, d, type, src)) {
            return null;
        }
        ControllableDrawerTile drawer = (ControllableDrawerTile) te;
        return type == AEItemStackType.ITEM_STACK_TYPE ? DrawerBridgeCache.itemMonitor(drawer)
            : DrawerBridgeCache.aspectMonitor(drawer);
    }

    public static void invalidate(@Nonnull ControllableDrawerTile drawer) {
        DrawerBridgeCache.invalidate(drawer);
    }

    @Nullable
    public static DrawerMEInventoryHandler itemMonitorOf(@Nonnull ControllableDrawerTile drawer) {
        return DrawerBridgeCache.itemMonitorOf(drawer);
    }
}
