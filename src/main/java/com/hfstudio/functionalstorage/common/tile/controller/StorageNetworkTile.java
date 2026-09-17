package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerEssentiaTransport;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerTransportAccess;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidAccess;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerInventoryAccess;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.item.LayeredToolItem;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import thaumcraft.api.aspects.IEssentiaTransport;

public abstract class StorageNetworkTile extends ControllableDrawerTile
    implements DrawerInventoryAccess, DrawerFluidAccess, DrawerTransportAccess {

    private final AggregatedStorage.Items items = new AggregatedStorage.Items();
    private final AggregatedStorage.Fluids fluids = new AggregatedStorage.Fluids();
    private final AggregatedStorage.Aspects aspects = new AggregatedStorage.Aspects();
    private final DrawerFluidHandler fluidPort = new DrawerFluidHandler(fluids);
    private final DrawerEssentiaTransport aspectPort = new DrawerEssentiaTransport(this, this::getAspectHandler);
    private final List<IBigItemHandler> itemHandlers = new ArrayList<>();
    private final List<IBigFluidHandler> fluidHandlers = new ArrayList<>();
    private final List<IBigAspectHandler> aspectHandlers = new ArrayList<>();
    private IInventory inventory;
    private long checkedAt = Long.MIN_VALUE;

    protected abstract List<ControllableDrawerTile> connectedDrawers();

    public void invalidateNetwork() {
        if (inventory instanceof DrawerItemInventory view) view.flushChanges();
        checkedAt = Long.MIN_VALUE;
    }

    private void refreshNetwork() {
        if (worldObj == null || worldObj.getTotalWorldTime() == checkedAt) return;
        checkedAt = worldObj.getTotalWorldTime();
        if (inventory instanceof DrawerItemInventory view) view.flushChanges();
        itemHandlers.clear();
        fluidHandlers.clear();
        aspectHandlers.clear();
        for (ControllableDrawerTile drawer : connectedDrawers()) {
            if (drawer instanceof StorageNetworkTile) continue;
            if (drawer.getItemHandler() != null) itemHandlers.add(drawer.getItemHandler());
            if (drawer.getFluidHandler() != null) fluidHandlers.add(drawer.getFluidHandler());
            if (drawer.getAspectHandler() != null) aspectHandlers.add(drawer.getAspectHandler());
        }
        if (items.rebuild(itemHandlers)) inventory = null;
        fluids.rebuild(fluidHandlers);
        aspects.rebuild(aspectHandlers);
    }

    @Override
    public IBigItemHandler getItemHandler() {
        refreshNetwork();
        return items;
    }

    @Override
    public IBigFluidHandler getFluidHandler() {
        refreshNetwork();
        return fluids;
    }

    @Override
    public IBigAspectHandler getAspectHandler() {
        refreshNetwork();
        return aspects;
    }

    @Override
    public IInventory getInventoryView() {
        refreshNetwork();
        if (inventory == null)
            inventory = new DrawerItemInventory(items, "container.functionalstorage.drawer", this::markDirty);
        return inventory;
    }

    @Override
    public IFluidHandler getForgeFluidHandler() {
        refreshNetwork();
        return fluidPort;
    }

    @Override
    public IEssentiaTransport getEssentiaTransport() {
        refreshNetwork();
        return aspectPort;
    }

    @Override
    public void applyConfiguration(ConfigurationAction action) {
        super.applyConfiguration(action);
        for (ControllableDrawerTile drawer : connectedDrawers()) {
            if (action == ConfigurationAction.LOCKING) drawer.setLocked(isLocked());
            else {
                drawer.getDrawerOptions()
                    .setActive(action, getDrawerOptions().isActive(action));
                drawer.getDrawerOptions()
                    .setAdvancedValue(action, getDrawerOptions().getAdvancedValue(action));
                drawer.markOptionsDirty();
            }
        }
    }

    @Override
    public void markDirty() {
        if (inventory instanceof DrawerItemInventory view) view.flushChanges();
        super.markDirty();
    }

    private void clearNetwork() {
        if (inventory instanceof DrawerItemInventory view) view.flushChanges();
        items.rebuild(List.of());
        fluids.rebuild(List.of());
        aspects.rebuild(List.of());
        itemHandlers.clear();
        fluidHandlers.clear();
        aspectHandlers.clear();
        inventory = null;
        checkedAt = Long.MIN_VALUE;
    }

    @Override
    public void onChunkUnload() {
        clearNetwork();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        clearNetwork();
        super.invalidate();
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, int side, float x, float y, float z, int slot) {
        ItemStack held = player.getHeldItem();
        if (held != null && !player.isSneaking()
            && !(held.getItem() instanceof LayeredToolItem)
            && !(held.getItem() instanceof IStorageUpgrade)) {
            ItemStack remaining = getItemHandler().insertItem(0, held, false);
            if (!player.capabilities.isCreativeMode)
                player.inventory.setInventorySlotContents(player.inventory.currentItem, remaining);
            player.inventory.markDirty();
            return true;
        }
        return super.onSlotActivated(player, side, x, y, z, slot);
    }
}
