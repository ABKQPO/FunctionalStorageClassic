package com.hfstudio.functionalstorage.common.integration.ae2;

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
 * AE2 grid host. Handlers are created on demand and never retained, so a stale
 * bus never keeps a broken drawer alive.
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
        ControllableDrawerTile drawer = (ControllableDrawerTile) te;
        return new DrawerMEInventoryHandler(drawer.getItemHandler(), drawer.getStorageUpgradeSlots());
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
        if (type == AEItemStackType.ITEM_STACK_TYPE) {
            return new DrawerMEInventoryHandler(drawer.getItemHandler(), drawer.getStorageUpgradeSlots());
        }
        return new DrawerMEEssentiaInventoryHandler(drawer.getAspectHandler(), drawer.getStorageUpgradeSlots());
    }
}
