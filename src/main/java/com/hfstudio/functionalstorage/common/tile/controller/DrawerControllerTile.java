package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class DrawerControllerTile extends StorageNetworkTile {

    private final List<ControllableDrawerTile> loadedDrawers = new ArrayList<>();
    private static final Comparator<ControllableDrawerTile> PRIORITY = Comparator
        .comparingInt(ControllableDrawerTile::getPriority)
        .reversed();

    private final Set<Long> drawers = new LinkedHashSet<>();

    @Override
    public void markDirty() {
        super.markDirty();
        requestUpdatePacket();
    }

    @Override
    public void invalidateNetwork() {
        super.invalidateNetwork();
        if (worldObj == null) return;
        for (long position : drawers) {
            int x = (int) (position >> 38);
            int y = (int) (position >> 26 & 0xFFF);
            int z = (int) (position << 38 >> 38);
            if (worldObj.blockExists(x, y, z)
                && worldObj.getTileEntity(x, y, z) instanceof ControllerExtensionTile extension) {
                extension.invalidateNetwork();
            }
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }

    public boolean linkDrawer(int x, int y, int z, boolean remove) {
        if (worldObj == null || !worldObj.blockExists(x, y, z)
            || !(worldObj.getTileEntity(x, y, z) instanceof ControllableDrawerTile drawer)) {
            return false;
        }
        long position = pack(x, y, z);
        if (drawer instanceof StorageNetworkTile && !(drawer instanceof ControllerExtensionTile)) return false;
        if (remove) {
            if (!drawers.remove(position)) {
                return false;
            }
            int[] controller = drawer.getControllerPosition();
            if (controller != null && controller[0] == xCoord && controller[1] == yCoord && controller[2] == zCoord) {
                drawer.clearControllerPosition();
            }
            markDirty();
            invalidateNetwork();
            return true;
        }
        int range = getLinkingRange();
        if (Math.abs(x - xCoord) > range || Math.abs(y - yCoord) > range
            || Math.abs(z - zCoord) > range
            || (!drawers.contains(position) && drawers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit)) {
            return false;
        }
        drawer.detachFromController(worldObj);
        drawer.setControllerPosition(xCoord, yCoord, zCoord);
        addDrawer(x, y, z);
        return true;
    }

    public void addDrawer(int x, int y, int z) {
        if (drawers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit) {
            return;
        }
        if (drawers.add(pack(x, y, z))) {
            invalidateNetwork();
            markDirty();
        }
    }

    public void removeDrawer(int x, int y, int z) {
        if (drawers.remove(pack(x, y, z))) {
            invalidateNetwork();
            markDirty();
        }
    }

    public int getDrawerCount() {
        return drawers.size();
    }

    @Nonnull
    public Set<Long> getDrawers() {
        return Collections.unmodifiableSet(drawers);
    }

    public int getLinkingRange() {
        return Math.max(
            1,
            (int) calculateModifier(
                UpgradeAttribute.CONTROLLER_RANGE,
                FunctionalStorageConfig.GENERAL.drawerControllerLinkingRange));
    }

    @Override
    protected List<ControllableDrawerTile> connectedDrawers() {
        List<ControllableDrawerTile> loaded = loadedDrawers;
        loaded.clear();
        for (long position : drawers) {
            int x = (int) (position >> 38);
            int y = (int) (position >> 26 & 0xFFF);
            int z = (int) (position << 38 >> 38);
            if (worldObj.blockExists(x, y, z)
                && worldObj.getTileEntity(x, y, z) instanceof ControllableDrawerTile drawer
                && !(drawer instanceof StorageNetworkTile)) {
                if (drawer.isLinkedTo(xCoord, yCoord, zCoord)) loaded.add(drawer);
            }
        }
        loaded.sort(PRIORITY);
        return loaded;
    }

    @Override
    public void onBlockBroken() {
        invalidateNetwork();
        for (long position : drawers) {
            int x = (int) (position >> 38);
            int y = (int) (position >> 26 & 0xFFF);
            int z = (int) (position << 38 >> 38);
            if (worldObj.blockExists(x, y, z)
                && worldObj.getTileEntity(x, y, z) instanceof ControllableDrawerTile drawer) {
                int[] controller = drawer.getControllerPosition();
                if (controller != null && controller[0] == xCoord && controller[1] == yCoord && controller[2] == zCoord)
                    drawer.clearControllerPosition();
            }
        }
        drawers.clear();
        super.onBlockBroken();
    }

    @Override
    protected void writeStorageData(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (long position : drawers) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("Position", position);
            list.appendTag(entry);
        }
        tag.setTag("Drawers", list);
    }

    @Override
    protected void readStorageData(NBTTagCompound tag) {
        drawers.clear();
        NBTTagList list = tag.getTagList("Drawers", 10);
        for (int index = 0; index < list.tagCount(); index++) drawers.add(
            list.getCompoundTagAt(index)
                .getLong("Position"));
        invalidateNetwork();
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }
}
