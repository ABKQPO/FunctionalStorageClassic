package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class DrawerControllerTile extends StorageNetworkTile {

    private final List<ControllableDrawerTile> loadedDrawers = new ArrayList<>();
    private static final Comparator<ControllableDrawerTile> PRIORITY = Comparator
        .comparingInt(ControllableDrawerTile::getPriority)
        .reversed();

    protected final Set<Long> drawers = new LinkedHashSet<>();

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
        writeModelState(tag);
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
            if (ownsPhysicalControllerLink()) {
                int[] controller = drawer.getControllerPosition();
                if (controller != null && controller[0] == xCoord
                    && controller[1] == yCoord
                    && controller[2] == zCoord) {
                    drawer.clearControllerPosition();
                }
            } else if (drawer.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                drawer.clearAdaptiveMatrixPosition();
            }
            markDirty();
            invalidateNetwork();
            return true;
        }
        if (!isWithinLinkingRange(x, y, z)
            || (!drawers.contains(position) && drawers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit)) {
            return false;
        }
        if (!ownsPhysicalControllerLink()) {
            int[] owner = drawer.getAdaptiveMatrixPosition();
            if (owner != null && !drawer.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                return false;
            }
            drawer.setAdaptiveMatrixPosition(xCoord, yCoord, zCoord);
        } else {
            drawer.detachFromController(worldObj);
            drawer.setControllerPosition(xCoord, yCoord, zCoord);
        }
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

    @Nonnull
    public ConnectedDrawerScope connectedScope(int maxRadius) {
        if (worldObj == null) {
            return ConnectedDrawerScope.empty();
        }
        Set<Long> members = new HashSet<>(drawers.size());
        long radius = maxRadius < 0 ? Long.MAX_VALUE : maxRadius;
        for (ControllableDrawerTile drawer : connectedDrawers()) {
            if (!(drawer instanceof StorageNetworkTile) && Math.abs((long) drawer.xCoord - xCoord) <= radius
                && Math.abs((long) drawer.yCoord - yCoord) <= radius
                && Math.abs((long) drawer.zCoord - zCoord) <= radius) {
                members.add(pack(drawer.xCoord, drawer.yCoord, drawer.zCoord));
            }
        }
        return ConnectedDrawerScope.of(members);
    }

    @Nullable
    private ControllableDrawerTile linkedDrawerAt(long position, long radius) {
        int x = ConnectedDrawerScope.unpackX(position);
        int y = ConnectedDrawerScope.unpackY(position);
        int z = ConnectedDrawerScope.unpackZ(position);
        if (Math.abs(x - xCoord) > radius || Math.abs(y - yCoord) > radius || Math.abs(z - zCoord) > radius) {
            return null;
        }
        if (!worldObj.blockExists(x, y, z)) {
            return null;
        }
        if (!(worldObj.getTileEntity(x, y, z) instanceof ControllableDrawerTile drawer)) {
            return null;
        }
        // An extension owns no storage of its own, so a second controller's
        // extension must never be counted as a drawer of this network.
        if (drawer instanceof StorageNetworkTile && !(drawer instanceof ControllerExtensionTile)) {
            return null;
        }
        return isLinkedMember(drawer) ? drawer : null;
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
                if (isLinkedMember(drawer)) loaded.add(drawer);
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
                if (ownsPhysicalControllerLink()) {
                    int[] controller = drawer.getControllerPosition();
                    if (controller != null && controller[0] == xCoord
                        && controller[1] == yCoord
                        && controller[2] == zCoord) drawer.clearControllerPosition();
                } else if (drawer.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                    drawer.clearAdaptiveMatrixPosition();
                }
            }
        }
        drawers.clear();
        super.onBlockBroken();
    }

    @Override
    protected void writeStorageData(@NotNull NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (long position : drawers) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("Position", position);
            list.appendTag(entry);
        }
        tag.setTag("Drawers", list);
    }

    @Override
    protected void readStorageData(@NotNull NBTTagCompound tag) {
        drawers.clear();
        NBTTagList list = tag.getTagList("Drawers", 10);
        for (int index = 0; index < list.tagCount(); index++) drawers.add(
            list.getCompoundTagAt(index)
                .getLong("Position"));
        invalidateNetwork();
    }

    protected boolean ownsPhysicalControllerLink() {
        return true;
    }

    protected boolean isWithinLinkingRange(int x, int y, int z) {
        int range = getLinkingRange();
        return Math.abs((long) x - xCoord) <= range && Math.abs((long) y - yCoord) <= range
            && Math.abs((long) z - zCoord) <= range;
    }

    protected boolean isLinkedMember(ControllableDrawerTile drawer) {
        return ownsPhysicalControllerLink() ? drawer.isLinkedTo(xCoord, yCoord, zCoord)
            : drawer.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord);
    }

    protected static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }
}
