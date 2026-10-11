package com.hfstudio.functionalstorage.common.tile.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.interaction.ToolFeedback;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayEntry;
import com.hfstudio.functionalstorage.common.presentation.AdaptiveDisplayProvider;
import com.hfstudio.functionalstorage.common.presentation.AdaptivePresentationPlan;
import com.hfstudio.functionalstorage.common.presentation.AdaptivePresentationRegistry;
import com.hfstudio.functionalstorage.common.tile.AdaptiveDepositTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveDrawerTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.IEssentiaTransport;

@Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHCore", striprefs = true)
public class AdaptiveMatrixTile extends DrawerControllerTile implements IEnergyReceiver {

    private static final String KEY_ENERGY = "AdaptiveEnergy";
    private static final String KEY_POWERED = "AdaptivePowered";
    private static final String KEY_PRESENTATION_SORT = "AdaptivePresentationSort";
    private static final String KEY_PRESENTATION_LAYOUT = "AdaptivePresentationLayout";
    private static final String KEY_PRESENTATION_ORDER = "AdaptivePresentationOrder";
    private static final String KEY_TERMINAL_MODE = "AdaptiveTerminalMode";
    private static final String KEY_LINKED_CONTROLLERS = "AdaptiveLinkedControllers";

    public static final String TERMINAL_ITEMS = "items";
    public static final String TERMINAL_FLUIDS = "fluids";
    public static final String TERMINAL_ASPECTS = "aspects";

    private int energy;
    private boolean powered;
    private String presentationSort = AdaptivePresentationRegistry.SORT_COUNT;
    private String presentationLayout = AdaptivePresentationRegistry.LAYOUT_BALANCED;
    private String presentationOrder = AdaptivePresentationRegistry.ORDER_UP;
    private String terminalMode = TERMINAL_ITEMS;
    private AdaptivePresentationPlan presentationPlan;
    private AdaptivePresentationPlan synchronizedPresentation = AdaptivePresentationPlan.empty();
    private NBTTagCompound presentationData;
    private StorageSubscription presentationSubscription = StorageSubscription.CLOSED;
    private boolean automaticLinkChecked;
    private final Set<Long> linkedControllers = new LinkedHashSet<>();
    private long controllerTopologySignature = Long.MIN_VALUE;
    private long topologyCheckedAt = Long.MIN_VALUE;
    private int loadedDrawerCount;
    private int cachedLinkingRange = -1;

    public static boolean usesRf() {
        return FunctionalStorageConfig.ADAPTIVE.enableRfPower && Mods.CoFHCore.isModLoaded();
    }

    @Override
    protected boolean networkAvailable() {
        return isPowered();
    }

    public boolean isPowered() {
        return !usesRf() || powered;
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getMaxEnergyStored() {
        return usesRf() ? Math.max(1, FunctionalStorageConfig.ADAPTIVE.energyCapacity) : 0;
    }

    public int getEnergyCostPerTick() {
        if (!usesRf()) return 0;
        long cost = (long) Math.max(0, FunctionalStorageConfig.ADAPTIVE.baseEnergyCost)
            + (long) Math.max(0, FunctionalStorageConfig.ADAPTIVE.itemDrawerEnergyCost) * loadedDrawerCount;
        return (int) Math.min(Integer.MAX_VALUE, cost);
    }

    @Override
    public int getLinkingRange() {
        if (cachedLinkingRange >= 0) return cachedLinkingRange;
        cachedLinkingRange = Math.max(1, FunctionalStorageConfig.ADAPTIVE.linkingRange);
        return cachedLinkingRange;
    }

    @Override
    protected boolean ownsPhysicalControllerLink() {
        return false;
    }

    @Override
    public boolean linkDrawer(int x, int y, int z, boolean remove) {
        if (linkFunctionalController(x, y, z, remove)) {
            return true;
        }
        return super.linkDrawer(x, y, z, remove);
    }

    private boolean linkFunctionalController(int x, int y, int z, boolean remove) {
        if (worldObj == null || !worldObj.blockExists(x, y, z)) {
            return false;
        }
        TileEntity tile = worldObj.getTileEntity(x, y, z);
        if (!(tile instanceof DrawerControllerTile controller) || controller instanceof AdaptiveMatrixTile) {
            return false;
        }
        long position = pack(x, y, z);
        if (remove) {
            if (!linkedControllers.remove(position)) {
                return false;
            }
            if (controller.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                controller.clearAdaptiveMatrixPosition();
            }
        } else {
            int range = getLinkingRange();
            int[] owner = controller.getAdaptiveMatrixPosition();
            if (Math.abs(x - xCoord) > range || Math.abs(y - yCoord) > range
                || Math.abs(z - zCoord) > range
                || owner != null && !controller.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)
                || (!linkedControllers.contains(position)
                    && linkedControllers.size() >= FunctionalStorageConfig.GENERAL.controllerLinkLimit)
                || !linkedControllers.add(position)) {
                return false;
            }
            controller.setAdaptiveMatrixPosition(xCoord, yCoord, zCoord);
        }
        controllerTopologySignature = Long.MIN_VALUE;
        markDirty();
        invalidateNetwork();
        return true;
    }

    @Override
    protected List<ControllableDrawerTile> connectedDrawers() {
        List<ControllableDrawerTile> result = new ArrayList<>(super.connectedDrawers());
        if (worldObj == null) {
            return result;
        }
        Set<Long> positions = new HashSet<>();
        for (ControllableDrawerTile drawer : result) {
            positions.add(pack(drawer.xCoord, drawer.yCoord, drawer.zCoord));
        }
        for (long position : linkedControllers) {
            int x = ConnectedDrawerScope.unpackX(position);
            int y = ConnectedDrawerScope.unpackY(position);
            int z = ConnectedDrawerScope.unpackZ(position);
            if (!worldObj.blockExists(x, y, z)) {
                continue;
            }
            TileEntity tile = worldObj.getTileEntity(x, y, z);
            if (!(tile instanceof DrawerControllerTile controller) || controller instanceof AdaptiveMatrixTile) {
                continue;
            }
            if (!controller.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                continue;
            }
            for (ControllableDrawerTile drawer : controller.connectedDrawers()) {
                long drawerPosition = pack(drawer.xCoord, drawer.yCoord, drawer.zCoord);
                if (positions.add(drawerPosition)) {
                    result.add(drawer);
                }
            }
        }
        return result;
    }

    @Nonnull
    public String getPresentationSort() {
        return presentationSort;
    }

    @Nonnull
    public String getPresentationLayout() {
        return presentationLayout;
    }

    @Nonnull
    public String getPresentationOrder() {
        return presentationOrder;
    }

    public void cyclePresentationSort() {
        setPresentationSort(AdaptivePresentationRegistry.nextSortId(presentationSort));
    }

    public void cyclePresentationLayout() {
        setPresentationLayout(AdaptivePresentationRegistry.nextLayoutId(presentationLayout));
    }

    public void cyclePresentationOrder() {
        setPresentationOrder(AdaptivePresentationRegistry.nextOrderId(presentationOrder));
    }

    @Nonnull
    public String getTerminalMode() {
        return terminalMode;
    }

    public void cycleTerminalMode() {
        String next = switch (terminalMode) {
            case TERMINAL_ITEMS -> TERMINAL_FLUIDS;
            case TERMINAL_FLUIDS -> supportsEssentia() ? TERMINAL_ASPECTS : TERMINAL_ITEMS;
            default -> TERMINAL_ITEMS;
        };
        setTerminalMode(next);
    }

    public void setTerminalMode(@Nonnull String mode) {
        String resolved = resolveTerminalMode(mode);
        if (terminalMode.equals(resolved)) return;
        terminalMode = resolved;
        invalidateNetwork();
        markDirty();
        requestUpdatePacket();
    }

    public boolean isTerminalItems() {
        return TERMINAL_ITEMS.equals(terminalMode);
    }

    public boolean isTerminalFluids() {
        return TERMINAL_FLUIDS.equals(terminalMode);
    }

    public boolean isTerminalAspects() {
        return TERMINAL_ASPECTS.equals(terminalMode);
    }

    public void setPresentationSort(@Nonnull String id) {
        String resolved = AdaptivePresentationRegistry.resolveSortId(id);
        if (presentationSort.equals(resolved)) return;
        presentationSort = resolved;
        invalidatePresentation();
    }

    public void setPresentationLayout(@Nonnull String id) {
        String resolved = AdaptivePresentationRegistry.resolveLayoutId(id);
        if (presentationLayout.equals(resolved)) return;
        presentationLayout = resolved;
        invalidatePresentation();
    }

    public void setPresentationOrder(@Nonnull String id) {
        String resolved = AdaptivePresentationRegistry.resolveOrderId(id);
        if (presentationOrder.equals(resolved)) return;
        presentationOrder = resolved;
        invalidatePresentation();
    }

    @Nonnull
    public AdaptivePresentationPlan getPresentationPlan() {
        if (worldObj != null && worldObj.isRemote) return synchronizedPresentation;
        if (!isPowered()) return AdaptivePresentationPlan.empty();
        ensurePresentationSubscription();
        if (presentationPlan == null) {
            presentationPlan = AdaptivePresentationPlan
                .build(connectedDrawers(), presentationSort, presentationLayout, presentationOrder);
        }
        return presentationPlan;
    }

    @Nullable
    public List<AdaptiveDisplayEntry> getDisplayEntries(long drawerPosition) {
        return isPowered() ? getPresentationPlan().entries(drawerPosition) : null;
    }

    @Nonnull
    public DrawerFaceLayout getDisplayLayout(long drawerPosition) {
        return getPresentationPlan().layout(drawerPosition);
    }

    public boolean canReserveDisplayItem(ControllableDrawerTile owner, int cell, ItemStack template) {
        for (ControllableDrawerTile drawer : connectedDrawers()) {
            if (!(drawer instanceof AdaptiveDisplayProvider provider)) continue;
            for (int index = 0; index < 4; index++) {
                if (drawer == owner && index == cell || !provider.isDisplayCellReserved(index)) continue;
                ItemStack reserved = provider.getDisplayReservation(index);
                if (reserved != null && new ItemStorageKey(reserved).equals(new ItemStorageKey(template))) return false;
            }
        }
        return true;
    }

    public int getUnassignedDisplayResourceCount() {
        return getPresentationPlan().getUnassignedResourceCount();
    }

    public boolean extractDisplayItem(@Nonnull EntityPlayer player, @Nonnull ItemStack template, long amount) {
        if (worldObj == null || worldObj.isRemote || !isPowered() || amount <= 0L) {
            return false;
        }
        IBigItemHandler handler = getItemHandler();
        if (handler == null) {
            return false;
        }
        long requested = Math.min(Math.max(1L, amount), template.getMaxStackSize());
        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .extractRouted(new BigItemStack(template, requested), StorageAction.EXECUTE);
        ItemStack extracted = result.getProcessed()
            .toItemStack();
        if (extracted == null) {
            return true;
        }
        if (!player.inventory.addItemStackToInventory(extracted)) {
            player.dropPlayerItemWithRandomChoice(extracted, false);
        }
        player.inventory.markDirty();
        markDirty();
        requestUpdatePacket();
        return true;
    }

    @Override
    public void invalidateNetwork() {
        presentationPlan = null;
        presentationData = null;
        cachedLinkingRange = -1;
        topologyCheckedAt = Long.MIN_VALUE;
        super.invalidateNetwork();
    }

    @Override
    public void onChunkUnload() {
        closePresentationSubscription();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        closePresentationSubscription();
        super.invalidate();
    }

    @Override
    public void onBlockBroken() {
        if (worldObj != null) {
            for (long position : linkedControllers) {
                int x = ConnectedDrawerScope.unpackX(position);
                int y = ConnectedDrawerScope.unpackY(position);
                int z = ConnectedDrawerScope.unpackZ(position);
                if (worldObj.blockExists(x, y, z)
                    && worldObj.getTileEntity(x, y, z) instanceof DrawerControllerTile controller
                    && controller.isLinkedToAdaptiveMatrix(xCoord, yCoord, zCoord)) {
                    controller.clearAdaptiveMatrixPosition();
                }
            }
        }
        linkedControllers.clear();
        super.onBlockBroken();
    }

    @Override
    @Nullable
    public IBigItemHandler getItemHandler() {
        refreshControllerTopology();
        return isPowered() ? super.getItemHandler() : null;
    }

    @Override
    @Nullable
    public IBigFluidHandler getFluidHandler() {
        refreshControllerTopology();
        return isPowered() ? super.getFluidHandler() : null;
    }

    @Override
    @Nullable
    public IBigAspectHandler getAspectHandler() {
        refreshControllerTopology();
        return isPowered() ? super.getAspectHandler() : null;
    }

    @Override
    @Nullable
    public IInventory getInventoryView() {
        return isPowered() ? super.getInventoryView() : null;
    }

    @Override
    @Nullable
    public IFluidHandler getForgeFluidHandler() {
        return isPowered() ? super.getForgeFluidHandler() : null;
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    @Nullable
    public IEssentiaTransport getEssentiaTransport() {
        return isPowered() ? super.getEssentiaTransport() : null;
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote) {
            linkNearbyDrawers();
            refreshControllerTopology();
            updatePowerState();
        }
        super.updateEntity();
    }

    @Override
    public boolean onSlotActivated(EntityPlayer player, int side, float x, float y, float z, int slot) {
        if (worldObj != null && !worldObj.isRemote && player.getHeldItem() == null) {
            ToolFeedback.send(
                player,
                new ChatComponentTranslation(
                    usesRf() ? "functionalstorage.adaptive.matrix.status.rf"
                        : "functionalstorage.adaptive.matrix.status",
                    StatCollector.translateToLocal(
                        isPowered() ? "functionalstorage.gui.adaptive.online"
                            : "functionalstorage.gui.adaptive.offline"),
                    NumberFormatUtil.formatNumberCompact(getDrawerCount()),
                    NumberFormatUtil.formatNumberCompact(energy),
                    NumberFormatUtil.formatNumberCompact(getMaxEnergyStored()),
                    NumberFormatUtil.formatNumberCompact(getEnergyCostPerTick())));
            return true;
        }
        return super.onSlotActivated(player, side, x, y, z, slot);
    }

    @Override
    public boolean openGui(@Nonnull EntityPlayer player) {
        return false;
    }

    @Override
    protected int getClientModelKey() {
        return isPowered() ? 1 : 0;
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        if (!usesRf() || maxReceive <= 0 || !canConnectEnergy(from)) {
            return 0;
        }
        int accepted = Math.min(
            maxReceive,
            Math.min(
                Math.max(0, FunctionalStorageConfig.ADAPTIVE.maxEnergyReceive),
                Math.max(0, getMaxEnergyStored() - energy)));
        if (!simulate && accepted > 0) {
            energy += accepted;
            markEnergyDirty();
        }
        return accepted;
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public int getEnergyStored(ForgeDirection from) {
        return canConnectEnergy(from) ? energy : 0;
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return canConnectEnergy(from) ? getMaxEnergyStored() : 0;
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return usesRf() && from != null && from != ForgeDirection.UNKNOWN;
    }

    private void updatePowerState() {
        if (!usesRf()) {
            // Without CoFHCore this block is the no-energy variant. Its
            // availability must not depend on a server tick or a sync packet.
            powered = true;
            return;
        }
        int cost = getEnergyCostPerTick();
        boolean nextPowered = cost == 0 || energy >= cost;
        if (nextPowered && cost > 0) {
            energy -= cost;
            markEnergyDirty();
        }
        if (powered != nextPowered) {
            powered = nextPowered;
            invalidateNetwork();
            markDirty();
            requestUpdatePacket();
        }
    }

    private void linkNearbyDrawers() {
        if (automaticLinkChecked || worldObj == null) {
            return;
        }
        automaticLinkChecked = true;
        List<?> loaded = new ArrayList<>(worldObj.loadedTileEntityList);
        for (Object entry : loaded) {
            if (entry instanceof ControllableDrawerTile drawer && drawer != this
                && !drawer.isInvalid()
                && isAdaptiveMemberType(drawer)) {
                AdaptiveLinking.tryLink(drawer);
            }
        }
    }

    private boolean isAdaptiveMemberType(ControllableDrawerTile drawer) {
        return drawer instanceof AdaptiveDrawerTile || drawer instanceof AdaptiveDepositTile
            || drawer instanceof AdaptiveGridTile
            || drawer instanceof FluidDrawerTile
            || drawer instanceof ArmoryCabinetTile
            || (Mods.Thaumcraft.isModLoaded() && drawer instanceof EssentiaDrawerTile);
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        super.writeStorageData(tag);
        NBTTagList controllers = new NBTTagList();
        for (long position : linkedControllers) {
            NBTTagCompound controller = new NBTTagCompound();
            controller.setLong("Position", position);
            controllers.appendTag(controller);
        }
        tag.setTag(KEY_LINKED_CONTROLLERS, controllers);
        tag.setInteger(KEY_ENERGY, Math.max(0, Math.min(getMaxEnergyStored(), energy)));
        tag.setBoolean(KEY_POWERED, isPowered());
        tag.setString(KEY_PRESENTATION_SORT, presentationSort);
        tag.setString(KEY_PRESENTATION_LAYOUT, presentationLayout);
        tag.setString(KEY_PRESENTATION_ORDER, presentationOrder);
        tag.setString(KEY_TERMINAL_MODE, terminalMode);
        if (worldObj != null && !worldObj.isRemote) {
            if (presentationData == null) presentationData = getPresentationPlan().serializeNBT();
            tag.setTag("AdaptiveDisplay", presentationData);
        }
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        super.readStorageData(tag);
        linkedControllers.clear();
        NBTTagList controllers = tag.getTagList(KEY_LINKED_CONTROLLERS, 10);
        for (int index = 0; index < controllers.tagCount(); index++) {
            linkedControllers.add(
                controllers.getCompoundTagAt(index)
                    .getLong("Position"));
        }
        controllerTopologySignature = Long.MIN_VALUE;
        energy = Math.max(0, Math.min(getMaxEnergyStored(), tag.getInteger(KEY_ENERGY)));
        powered = !usesRf() || tag.getBoolean(KEY_POWERED);
        presentationSort = AdaptivePresentationRegistry.resolveSortId(tag.getString(KEY_PRESENTATION_SORT));
        presentationLayout = AdaptivePresentationRegistry.resolveLayoutId(tag.getString(KEY_PRESENTATION_LAYOUT));
        presentationOrder = AdaptivePresentationRegistry.resolveOrderId(tag.getString(KEY_PRESENTATION_ORDER));
        terminalMode = resolveTerminalMode(tag.getString(KEY_TERMINAL_MODE));
        presentationPlan = null;
        if (worldObj != null && worldObj.isRemote) {
            synchronizedPresentation = AdaptivePresentationPlan.fromNBT(tag.getCompoundTag("AdaptiveDisplay"));
        }
        invalidateNetwork();
    }

    private String resolveTerminalMode(@Nonnull String mode) {
        if (TERMINAL_FLUIDS.equals(mode)) return TERMINAL_FLUIDS;
        if (TERMINAL_ASPECTS.equals(mode) && supportsEssentia()) return TERMINAL_ASPECTS;
        return TERMINAL_ITEMS;
    }

    private boolean supportsEssentia() {
        return FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Mods.Thaumcraft.isModLoaded();
    }

    private void ensurePresentationSubscription() {
        if (!presentationSubscription.isClosed()) return;
        IBigItemHandler handler = super.getItemHandler();
        if (handler == null) return;
        presentationSubscription = handler.subscribe(change -> {
            presentationPlan = null;
            presentationData = null;
            requestUpdatePacket();
        });
    }

    private void closePresentationSubscription() {
        presentationSubscription.close();
        presentationSubscription = StorageSubscription.CLOSED;
    }

    private void refreshControllerTopology() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        long now = worldObj.getTotalWorldTime();
        if (now == topologyCheckedAt) return;
        topologyCheckedAt = now;
        long signature = getDrawers().hashCode();
        for (long position : getDrawers()) signature = 31L * signature + loadedTileIdentity(position);
        for (long position : linkedControllers) {
            signature = 31L * signature + position;
            signature = 31L * signature + loadedTileIdentity(position);
            int x = ConnectedDrawerScope.unpackX(position);
            int y = ConnectedDrawerScope.unpackY(position);
            int z = ConnectedDrawerScope.unpackZ(position);
            if (!worldObj.blockExists(x, y, z)) {
                continue;
            }
            TileEntity tile = worldObj.getTileEntity(x, y, z);
            if (tile instanceof DrawerControllerTile controller) {
                signature = 31L * signature + controller.getDrawers()
                    .hashCode();
                for (long member : controller.getDrawers()) signature = 31L * signature + loadedTileIdentity(member);
            }
        }
        if (signature == controllerTopologySignature) {
            return;
        }
        boolean changed = controllerTopologySignature != Long.MIN_VALUE;
        controllerTopologySignature = signature;
        cachedLinkingRange = -1;
        loadedDrawerCount = 0;
        for (ControllableDrawerTile drawer : connectedDrawers()) {
            if (drawer instanceof WoodDrawerTile) loadedDrawerCount++;
        }
        if (changed) {
            invalidateNetwork();
        }
        topologyCheckedAt = now;
    }

    private int loadedTileIdentity(long position) {
        int x = ConnectedDrawerScope.unpackX(position);
        int y = ConnectedDrawerScope.unpackY(position);
        int z = ConnectedDrawerScope.unpackZ(position);
        if (!worldObj.blockExists(x, y, z)) return 0;
        TileEntity tile = worldObj.getTileEntity(x, y, z);
        return tile == null || tile.isInvalid() ? 0 : System.identityHashCode(tile);
    }

    private void invalidatePresentation() {
        presentationPlan = null;
        presentationData = null;
        markDirty();
        requestUpdatePacket();
    }

    private void markEnergyDirty() {
        if (worldObj != null) worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
    }
}
