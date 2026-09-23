package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageViewCache;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerEssentiaTransport;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerTransportAccess;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.interaction.FluidContainerInteraction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidAccess;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerInventoryAccess;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.item.LayeredToolItem;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.IEssentiaTransport;

@Optional.Interface(
    iface = "com.hfstudio.functionalstorage.common.integration.thaumcraft.DrawerTransportAccess",
    modid = "Thaumcraft")
public abstract class StorageNetworkTile extends ControllableDrawerTile
    implements DrawerInventoryAccess, DrawerFluidAccess, DrawerTransportAccess {

    protected static final int ROUTED = -1;

    private final AggregatedStorage.Items items = new AggregatedStorage.Items();
    private final AggregatedStorage.Fluids fluids = new AggregatedStorage.Fluids();
    private final AggregatedStorage.Aspects aspects = Mods.Thaumcraft.isModLoaded() ? new AggregatedStorage.Aspects()
        : null;
    private final DrawerFluidHandler fluidPort = new DrawerFluidHandler(fluids);
    private DrawerEssentiaTransport aspectPort;
    private final List<IBigItemHandler> itemHandlers = new ArrayList<>();
    private final List<IBigFluidHandler> fluidHandlers = new ArrayList<>();
    private final List<IBigAspectHandler> aspectHandlers = new ArrayList<>();
    private IInventory inventory;
    private long checkedAt = Long.MIN_VALUE;

    protected abstract List<ControllableDrawerTile> connectedDrawers();

    @Override
    public void updateEntity() {
        // Cached external handlers must observe invalidations without another tile lookup.
        if (worldObj != null && !worldObj.isRemote && checkedAt == Long.MIN_VALUE) {
            refreshNetwork();
        }
        super.updateEntity();
    }

    public void invalidateNetwork() {
        if (inventory instanceof DrawerItemInventory view) {
            view.flushChanges();
            // The aggregate is rebuilt lazily, so the limit derived from it must be
            // dropped here too: this is the funnel every drawer change reaches.
            view.invalidateLimit();
        }
        dropAggregateCache();
        checkedAt = Long.MIN_VALUE;
    }

    /**
     * Drops the aggregated read memos.
     *
     * <p>
     * A linked drawer reaches this through a storage change, but an upgrade that
     * only alters capacity need not change any stored amount, so the memos are also
     * dropped here where every structural drawer change funnels through.
     * </p>
     */
    private void dropAggregateCache() {
        StorageViewCache cache = items.getStorageViewCache();
        if (cache != null) {
            cache.invalidate();
        }
        fluids.invalidateFirstPopulated();
        if (aspects != null) {
            aspects.invalidateSummary();
        }
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
        if (aspects != null) aspects.rebuild(aspectHandlers);
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
    @Optional.Method(modid = "Thaumcraft")
    public IEssentiaTransport getEssentiaTransport() {
        refreshNetwork();
        if (aspectPort == null) aspectPort = new DrawerEssentiaTransport(this, this::getAspectHandler);
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
        if (aspects != null) aspects.rebuild(List.of());
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
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held != null && !player.isSneaking()
            && !(held.getItem() instanceof LayeredToolItem)
            && !(held.getItem() instanceof IStorageUpgrade)) {
            IBigFluidHandler fluids = getFluidHandler();
            if (fluids != null && fluids.getStorageCount() > 0
                && FluidContainerInteraction.activate(player, fluids, ROUTED)) {
                return true;
            }
            IBigAspectHandler aspects = getAspectHandler();
            if (aspects != null && aspects.getStorageCount() > 0
                && EssentiaContainerRegistry.activate(player, aspects, ROUTED)) {
                return true;
            }
        }
        return super.onSlotActivated(player, side, x, y, z, slot);
    }

    @Override
    protected int depositTarget(int slot) {
        return ROUTED;
    }

    @Override
    protected boolean acceptsDeposit(@Nonnull IBigItemHandler handler, int slot) {
        return handler.getStorageCount() > 0;
    }

    @Override
    protected boolean prioritizesFluidContainerDeposit() {
        return FunctionalStorageConfig.CLIENT.prioritizeFluidContainerDeposit;
    }

    @Override
    public void onSlotClicked(@Nonnull EntityPlayer player, int slot) {}
}
