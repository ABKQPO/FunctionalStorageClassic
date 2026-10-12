package com.hfstudio.functionalstorage.common.container;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageResourceView;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.serverutilities.ServerUtilitiesIntegration;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.interaction.FluidContainerInteraction;
import com.hfstudio.functionalstorage.common.inventory.adapter.UpgradeSlotInventory;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.network.StorageSnapshotMessage;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.GuiHandler;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.common.Optional;
import lombok.Getter;
import thaumcraft.api.aspects.Aspect;

public class ContainerDrawer extends Container
    implements MenuSettingsReceiver, StorageTransferMenu, GhostFilterMenu, StorageSearchReceiver {

    private static final int PLAYER_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;
    private static final int TERMINAL_COLUMNS = DrawerGuiLayout.TERMINAL_COLUMNS;
    private static final int TERMINAL_ROWS = DrawerGuiLayout.TERMINAL_ROWS;
    private static final int MAX_VISIBLE_STORAGE_SLOTS = TERMINAL_COLUMNS * TERMINAL_ROWS;

    private final ControllableDrawerTile tile;
    @Getter
    private final DrawerGuiLayout layout;
    private final IInventory openedInventory;
    private final int storageSlotCount;
    private final InventoryBasic display;
    private static final AtomicLong NEXT_SESSION = new AtomicLong();
    private final List<StorageSubscription> subscriptions = new ArrayList<>();
    private IStorageHandler<?, ?> subscribedItems;
    private IStorageHandler<?, ?> subscribedFluids;
    private IStorageHandler<?, ?> subscribedAspects;
    private final Deque<NBTTagCompound> pendingSnapshots = new ArrayDeque<>();
    private List<NBTTagCompound> sentResources = List.of();
    @Getter
    private long session = NEXT_SESSION.incrementAndGet();
    @Getter
    private long viewRevision;
    private int[] visibleResources = new int[MAX_VISIBLE_STORAGE_SLOTS];
    private String viewMode = AdaptiveMatrixTile.TERMINAL_ITEMS;
    private boolean online;
    @Getter
    private int rfStored;
    @Getter
    private int rfCapacity;
    @Getter
    private int rfCost;
    @Getter
    private int unassignedDisplayResources;
    @Getter
    private String sortId = "count";
    @Getter
    private String layoutId = "balanced";
    @Getter
    private String orderId = "up";
    private boolean snapshotDirty = true;
    private boolean resourcesDirty = true;
    private boolean receivingCatalog;
    private boolean receivingReset;
    private boolean snapshotReady;
    private boolean catalogSent;
    private final List<Integer> filteredResources = new ArrayList<>();
    private List<StorageResourceView<BigItemStack, ItemStorageKey>> itemViews = List.of();
    private List<StorageResourceView<BigFluidStack, FluidStorageKey>> fluidViews = List.of();
    private List<StorageResourceView<BigAspectStack, AspectStorageKey>> aspectViews = List.of();
    private long transferViewsAt = Long.MIN_VALUE;
    private String query = "";
    private int scrollRow;
    private int maxScrollRow;
    private BitSet searchMatches;
    @Getter
    private int searchRevision;
    private boolean searchDirty = true;
    private boolean displayDirty = true;
    private boolean stateDirty = true;

    public ContainerDrawer(@Nonnull ControllableDrawerTile tile, @Nonnull EntityPlayer player) {
        this(tile, player, false);
    }

    protected ContainerDrawer(@Nonnull ControllableDrawerTile tile, @Nonnull EntityPlayer player, boolean crafting) {
        this.tile = tile;

        boolean terminal = isAdaptiveTerminal(tile);
        this.openedInventory = tile instanceof StorageNetworkTile && !terminal ? null : tile.getInventoryView();
        this.storageSlotCount = terminal ? MAX_VISIBLE_STORAGE_SLOTS
            : tile instanceof StorageNetworkTile || tile.getActiveStorage() == null ? 0
                : Math.min(
                    tile.getActiveStorage()
                        .getStorageCount(),
                    MAX_VISIBLE_STORAGE_SLOTS);
        this.display = new InventoryBasic("Drawer", false, storageSlotCount);
        Arrays.fill(visibleResources, -1);
        bindSubscriptions();
        int storageSlots = visibleStorageSlots();
        this.layout = new DrawerGuiLayout(
            storageSlots,
            ((DrawerBlock) tile.getBlockType()).getFaceLayout(),
            terminal,
            crafting);
        IInventory storage = display;
        for (int index = 0; index < storageSlots; index++) {
            addSlotToContainer(new StorageSlot(storage, index, layout.storageX(index), layout.storageY(index)));
        }

        refreshDisplay();
        int upgradeY = layout.upgradeY();
        IInventory storageUpgrades = UpgradeSlotInventory.of(tile, true);
        for (int slot = 0; slot < tile.getStorageUpgradeSlots(); slot++) {
            addSlotToContainer(new UpgradeSlot(storageUpgrades, slot, true, 10 + slot * 18, upgradeY));
        }
        IInventory utilityUpgrades = UpgradeSlotInventory.of(tile, false);
        for (int slot = 0; slot < tile.getUtilityUpgradeSlots(); slot++) {
            addSlotToContainer(new UpgradeSlot(utilityUpgrades, slot, false, 114 + slot * 18, upgradeY));
        }

        int playerTop = layout.inventoryY();
        for (int row = 0; row < PLAYER_ROWS; row++) {
            for (int column = 0; column < PLAYER_COLUMNS; column++) {
                addSlotToContainer(
                    new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, playerTop + row * 18));
            }
        }
        for (int column = 0; column < PLAYER_COLUMNS; column++) {
            addSlotToContainer(new Slot(player.inventory, column, 8 + column * 18, playerTop + 58));
        }
    }

    @Nonnull
    public ControllableDrawerTile getTile() {
        return tile;
    }

    public int getVisibleStorageSlots() {
        return visibleStorageSlots();
    }

    protected int playerInventoryEnd() {
        return storageSlotCount + tile.getStorageUpgradeSlots() + tile.getUtilityUpgradeSlots() + 36;
    }

    public String getViewMode() {
        return isTransferViewVirtual() ? viewMode : "";
    }

    public boolean isOnline() {
        return online && (!tile.getWorldObj().isRemote || snapshotReady && !receivingCatalog);
    }

    public boolean isSnapshotReady() {
        return snapshotReady && !receivingCatalog;
    }

    public boolean acceptsView(long expectedSession, long expectedRevision) {
        if (!isTransferViewVirtual()) return true;
        updateTerminalState();
        bindSubscriptions();
        refreshTransferViews();
        if (resourcesDirty) synchronizeResources();
        return session == expectedSession && viewRevision == expectedRevision
            && online
            && catalogSent
            && (!resourcesDirty || catalogIdentitiesMatch());
    }

    public boolean acceptsSearchView(long expectedSession, long expectedRevision) {
        return !isTransferViewVirtual() || acceptsView(expectedSession, expectedRevision);
    }

    private boolean catalogIdentitiesMatch() {
        if (sentResources.size() != itemViews.size() + fluidViews.size() + aspectViews.size()) return false;
        int offset = 0;
        for (StorageResourceView<BigItemStack, ItemStorageKey> view : itemViews) {
            if (!view.getSnapshot()
                .isSameType(
                    ItemUtil.readStack(
                        sentResources.get(offset++)
                            .getCompoundTag("Template"))))
                return false;
        }
        for (StorageResourceView<BigFluidStack, FluidStorageKey> view : fluidViews) {
            FluidStack template = FluidStack.loadFluidStackFromNBT(
                sentResources.get(offset++)
                    .getCompoundTag("Template"));
            if (template == null || !view.getSnapshot()
                .getTemplate()
                .isFluidEqual(template)) return false;
        }
        for (StorageResourceView<BigAspectStack, AspectStorageKey> view : aspectViews) {
            if (!view.getSnapshot()
                .getKey()
                .getTag()
                .equals(
                    sentResources.get(offset++)
                        .getCompoundTag("Template")
                        .getString("Aspect")))
                return false;
        }
        return true;
    }

    @Override
    public IBigItemHandler getTransferStorage() {
        return isItemTerminal() || isAllTerminal() ? tile.getItemHandler() : null;
    }

    @Override
    public boolean hasTransferStorage() {
        return isTransferViewVirtual() ? isOnline() : StorageTransferMenu.super.hasTransferStorage();
    }

    public int getItemResourceCount() {
        refreshTransferViews();
        return itemViews.size();
    }

    public int getFluidResourceCount() {
        refreshTransferViews();
        return fluidViews.size();
    }

    public int getAspectResourceCount() {
        refreshTransferViews();
        return aspectViews.size();
    }

    @Override
    public IBigFluidHandler getTransferFluidStorage() {
        return isFluidTerminal() || isAllTerminal() ? tile.getFluidHandler() : null;
    }

    @Override
    public IBigAspectHandler getTransferAspectStorage() {
        return isAspectTerminal() || isAllTerminal() ? tile.getAspectHandler() : null;
    }

    @Override
    public int getTransferSlotCount() {
        return storageSlotCount;
    }

    @Override
    public int getTransferSlot(int menuSlot) {
        return menuSlot >= 0 && menuSlot < storageSlotCount ? menuSlot : -1;
    }

    @Override
    public String getTransferResourceKind(int menuSlot) {
        if (!isTransferViewVirtual()) return StorageTransferMenu.super.getTransferResourceKind(menuSlot);
        return getViewResourceKind(transferResourceIndex(menuSlot));
    }

    public int getScrollRow() {
        refreshSearch();
        return scrollRow;
    }

    public int getMaxScrollRow() {
        refreshSearch();
        return maxScrollRow;
    }

    public int getTransferResourceCount() {
        refreshTransferViews();
        return transferResourceCount();
    }

    public BigItemStack getResourceItemSnapshot(int index) {
        refreshTransferViews();
        return index >= 0 && index < itemViews.size() ? itemViews.get(index)
            .getSnapshot() : BigItemStack.empty();
    }

    public BigFluidStack getResourceFluidSnapshot(int index) {
        refreshTransferViews();
        return index >= 0 && index < fluidViews.size() ? fluidViews.get(index)
            .getSnapshot() : BigFluidStack.empty();
    }

    public BigAspectStack getResourceAspectSnapshot(int index) {
        refreshTransferViews();
        return index >= 0 && index < aspectViews.size() ? aspectViews.get(index)
            .getSnapshot() : BigAspectStack.empty();
    }

    public String getViewResourceKind(int index) {
        refreshTransferViews();
        return resourceKind(index);
    }

    public BigItemStack getViewItemSnapshot(int index) {
        refreshTransferViews();
        int localIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_ITEMS);
        return localIndex >= 0 ? itemViews.get(localIndex)
            .getSnapshot() : BigItemStack.empty();
    }

    public BigFluidStack getViewFluidSnapshot(int index) {
        refreshTransferViews();
        int localIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_FLUIDS);
        return localIndex >= 0 ? fluidViews.get(localIndex)
            .getSnapshot() : BigFluidStack.empty();
    }

    public BigAspectStack getViewAspectSnapshot(int index) {
        refreshTransferViews();
        int localIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_ASPECTS);
        return localIndex >= 0 ? aspectViews.get(localIndex)
            .getSnapshot() : BigAspectStack.empty();
    }

    public void setScrollRow(int row) {
        refreshSearch();
        scrollRow = Math.max(0, Math.min(maxScrollRow, row));
        displayDirty = true;
        viewRevision++;
        snapshotDirty = true;
    }

    @Override
    public boolean isTransferViewVirtual() {
        return isAdaptiveTerminal(tile);
    }

    @Override
    public boolean isTransferViewMixed() {
        return isAllTerminal();
    }

    @Override
    public BigItemStack getTransferItemSnapshot(int menuSlot) {
        refreshTransferViews();
        if (isTransferViewVirtual()) {
            int index = transferResourceIndex(menuSlot);
            return getViewItemSnapshot(index);
        }
        return StorageTransferMenu.super.getTransferItemSnapshot(menuSlot);
    }

    @Override
    public BigFluidStack getTransferFluidSnapshot(int menuSlot) {
        refreshTransferViews();
        if (isTransferViewVirtual()) {
            int index = transferResourceIndex(menuSlot);
            return getViewFluidSnapshot(index);
        }
        return StorageTransferMenu.super.getTransferFluidSnapshot(menuSlot);
    }

    @Override
    public BigAspectStack getTransferAspectSnapshot(int menuSlot) {
        refreshTransferViews();
        if (isTransferViewVirtual()) {
            int index = transferResourceIndex(menuSlot);
            return getViewAspectSnapshot(index);
        }
        return StorageTransferMenu.super.getTransferAspectSnapshot(menuSlot);
    }

    @Override
    public long getTransferCapacity(int menuSlot) {
        refreshTransferViews();
        if (isTransferViewVirtual()) {
            int index = transferResourceIndex(menuSlot);
            int itemIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_ITEMS);
            if (itemIndex >= 0) {
                return itemViews.get(itemIndex)
                    .getCapacity();
            }
            int fluidIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_FLUIDS);
            if (fluidIndex >= 0) {
                return fluidViews.get(fluidIndex)
                    .getCapacity();
            }
            int aspectIndex = resourceIndex(index, AdaptiveMatrixTile.TERMINAL_ASPECTS);
            if (aspectIndex >= 0) {
                return aspectViews.get(aspectIndex)
                    .getCapacity();
            }
            return 0L;
        }
        return StorageTransferMenu.super.getTransferCapacity(menuSlot);
    }

    @Override
    public void applySettings(EntityPlayer player, int value, String text) {
        if ("adaptive_mode_next".equals(text) && canInteractWith(player)) {
            if (tile instanceof AdaptiveMatrixTile matrix) matrix.cycleTerminalMode();
            else if (tile instanceof AdaptiveGridTile grid) grid.cycleTerminalMode();
            else return;
            transferViewsAt = Long.MIN_VALUE;
            stateDirty = true;
            return;
        }
        if ("adaptive_sort_next".equals(text) && canInteractWith(player)) {
            if (tile instanceof AdaptiveMatrixTile matrix) matrix.cyclePresentationSort();
            else if (tile instanceof AdaptiveGridTile grid) grid.cyclePresentationSort();
            stateDirty = true;
            return;
        }
        if ("adaptive_layout_next".equals(text) && canInteractWith(player)) {
            if (tile instanceof AdaptiveMatrixTile matrix) matrix.cyclePresentationLayout();
            else if (tile instanceof AdaptiveGridTile grid) grid.cyclePresentationLayout();
            stateDirty = true;
            return;
        }
        if ("adaptive_order_next".equals(text) && canInteractWith(player)) {
            if (tile instanceof AdaptiveMatrixTile matrix) matrix.cyclePresentationOrder();
            else if (tile instanceof AdaptiveGridTile grid) grid.cyclePresentationOrder();
            stateDirty = true;
            return;
        }
        if (!(tile instanceof StorageNetworkTile) && canInteractWith(player)) tile.setPriority(value);
    }

    @Override
    public void applySearch(EntityPlayer player, int requestedRow, String text, int[] matches) {
        if (!isTransferViewVirtual() || !canInteractWith(player)) return;
        query = text == null ? ""
            : text.trim()
                .toLowerCase(Locale.ROOT);
        searchMatches = query.isEmpty() ? null : new BitSet(transferResourceCount());
        if (searchMatches != null) {
            for (int index : matches) {
                if (index >= 0 && index < transferResourceCount()) searchMatches.set(index);
            }
        }
        searchDirty = true;
        refreshSearch();
        scrollRow = Math.max(0, Math.min(requestedRow, maxScrollRow));
        viewRevision++;
        snapshotDirty = true;
        displayDirty = true;
        stateDirty = true;
        detectAndSendChanges();
    }

    @Override
    public void applyFilter(EntityPlayer player, int slot, ItemStack template) {
        if (!isTransferViewVirtual() && canInteractWith(player)
            && slot >= 0
            && slot < storageSlotCount
            && tile.setItemFilter(slot, template)) detectAndSendChanges();
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        int slot = id - GuiHandler.GUI_UPGRADE_BASE;
        if (!canInteractWith(player) || slot < 0 || slot >= tile.getUtilityUpgradeSlots()) {
            return false;
        }
        ItemStack stack = tile.getUtilityUpgrade(slot);
        if (stack != null && stack.getItem() instanceof RedstoneUpgradeItem upgrade) {
            upgrade.cycleSlot(
                stack,
                tile.getActiveStorage() == null ? 1
                    : tile.getActiveStorage()
                        .getStorageCount());
            tile.markOptionsDirty();
            return true;
        }
        if (stack == null || !(stack.getItem() instanceof AutomationUpgradeItem upgrade)
            || !upgrade.canOpenConfigurationGui()) {
            return false;
        }
        player.openGui(FunctionalStorage.instance, id, tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        return true;
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return (tile instanceof StorageNetworkTile || tile.getInventoryView() == openedInventory)
            && tile.getWorldObj() != null
            && !ServerUtilitiesIntegration.blocksInteraction(player, tile.xCoord, tile.yCoord, tile.zCoord)
            && tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
            && player.getDistanceSq(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D) <= 64D;
    }

    @Override
    public void detectAndSendChanges() {
        if (isTransferViewVirtual() && !tile.getWorldObj().isRemote) {
            updateTerminalState();
            bindSubscriptions();
            refreshTransferViews();
            synchronizeResources();
            sendSnapshots();
        }
        if (stateDirty && !isTransferViewVirtual()
            && storageSlotCount > 0
            && !tile.getWorldObj().isRemote
            && !crafters.isEmpty()) {
            stateDirty = false;
            Packet update = tile.getDescriptionPacket();
            for (Object crafter : crafters) {
                if (crafter instanceof EntityPlayerMP player && player.playerNetServerHandler != null) {
                    player.playerNetServerHandler.sendPacket(update);
                }
            }
        }
        refreshDisplay();
        super.detectAndSendChanges();
        refreshSearch();
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) scrollRow = Math.max(0, value);
        else if (id == 1) maxScrollRow = Math.max(0, value);
    }

    private void refreshDisplay() {
        if (tile.getWorldObj().isRemote) return;
        if (!isItemTerminal() && !isAllTerminal()) {
            for (int index = 0; index < storageSlotCount; index++) {
                if (display.getStackInSlot(index) != null) display.setInventorySlotContents(index, null);
            }
            displayDirty = false;
            return;
        }
        if (tile.getItemHandler() == null) return;
        refreshTransferViews();
        if (!displayDirty && !isAdaptiveTerminal(tile) && !subscriptions.isEmpty()) {
            return;
        }
        displayDirty = false;
        for (int index = 0; index < storageSlotCount; index++) {
            BigItemStack stored = getTransferItemSnapshot(index);
            ItemStack icon = stored.isEmpty() ? null : stored.getTemplate();
            if (!ItemStack.areItemStacksEqual(display.getStackInSlot(index), icon)) {
                display.setInventorySlotContents(index, icon);
            }
        }
    }

    private void refreshTransferViews() {
        if (!isTransferViewVirtual() || tile.getWorldObj() == null) return;
        if (tile.getWorldObj().isRemote || transferViewsAt != Long.MIN_VALUE) return;
        transferViewsAt = 0L;
        itemViews = tile.getItemHandler() != null ? StorageResourceView.aggregate(tile.getItemHandler()) : List.of();
        fluidViews = tile.getFluidHandler() != null ? StorageResourceView.aggregate(tile.getFluidHandler()) : List.of();
        aspectViews = tile.getAspectHandler() != null ? StorageResourceView.aggregate(tile.getAspectHandler())
            : List.of();
        searchDirty = true;
        refreshSearch();
    }

    private void closeSubscriptions() {
        for (StorageSubscription subscription : subscriptions) subscription.close();
        subscriptions.clear();
    }

    private void bindSubscriptions() {
        if (tile.getWorldObj() == null || tile.getWorldObj().isRemote) return;
        IStorageHandler<?, ?> items = tile.getItemHandler();
        IStorageHandler<?, ?> fluids = tile.getFluidHandler();
        IStorageHandler<?, ?> aspects = tile.getAspectHandler();
        if (items == subscribedItems && fluids == subscribedFluids && aspects == subscribedAspects) return;
        closeSubscriptions();
        subscribedItems = items;
        subscribedFluids = fluids;
        subscribedAspects = aspects;
        subscribe(items);
        subscribe(fluids);
        subscribe(aspects);
        dirtyResources();
    }

    private void subscribe(IStorageHandler<?, ?> storage) {
        if (storage != null) subscriptions.add(storage.subscribe(change -> dirtyResources()));
    }

    private void dirtyResources() {
        displayDirty = true;
        stateDirty = true;
        transferViewsAt = Long.MIN_VALUE;
        searchDirty = true;
        snapshotDirty = true;
        resourcesDirty = true;
    }

    private AdaptiveMatrixTile terminalMatrix() {
        return tile instanceof AdaptiveMatrixTile matrix ? matrix
            : tile instanceof AdaptiveGridTile grid ? grid.getMatrix() : null;
    }

    private void updateTerminalState() {
        AdaptiveMatrixTile matrix = terminalMatrix();
        String mode = matrix == null ? AdaptiveMatrixTile.TERMINAL_ITEMS : matrix.getTerminalMode();
        boolean powered = matrix != null && matrix.isPowered();
        if (!viewMode.equals(mode) || online != powered) {
            viewMode = mode;
            online = powered;
            scrollRow = 0;
            searchMatches = null;
            viewRevision++;
            dirtyResources();
        }
        int stored = matrix == null ? 0 : matrix.getEnergyStored();
        int capacity = matrix == null ? 0 : matrix.getMaxEnergyStored();
        int cost = matrix == null ? 0 : matrix.getEnergyCostPerTick();
        int unassigned = matrix == null || !powered ? 0 : matrix.getUnassignedDisplayResourceCount();
        String sort = matrix == null ? "count" : matrix.getPresentationSort();
        String layout = matrix == null ? "balanced" : matrix.getPresentationLayout();
        String order = matrix == null ? "up" : matrix.getPresentationOrder();
        if (rfStored != stored || rfCapacity != capacity
            || rfCost != cost
            || unassignedDisplayResources != unassigned
            || !sortId.equals(sort)
            || !layoutId.equals(layout)
            || !orderId.equals(order)) snapshotDirty = true;
        rfStored = stored;
        rfCapacity = capacity;
        rfCost = cost;
        unassignedDisplayResources = unassigned;
        sortId = sort;
        layoutId = layout;
        orderId = order;
    }

    private NBTTagCompound snapshotState() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Session", session);
        tag.setLong("Revision", viewRevision);
        tag.setString("Mode", viewMode);
        tag.setBoolean("Online", online);
        tag.setInteger("Energy", rfStored);
        tag.setInteger("EnergyCapacity", rfCapacity);
        tag.setInteger("EnergyCost", rfCost);
        tag.setInteger("Unassigned", unassignedDisplayResources);
        tag.setString("Sort", sortId);
        tag.setString("Layout", layoutId);
        tag.setString("Order", orderId);
        tag.setInteger("Row", scrollRow);
        tag.setInteger("MaxRow", maxScrollRow);
        int[] visible = new int[MAX_VISIBLE_STORAGE_SLOTS];
        for (int index = 0; index < visible.length; index++) visible[index] = transferResourceIndex(index);
        tag.setIntArray("Visible", visible);
        return tag;
    }

    private NBTTagCompound resourceData(int index, String kind) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Index", index);
        tag.setString("Kind", kind);
        NBTTagCompound template = new NBTTagCompound();
        if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind)) {
            StorageResourceView<BigItemStack, ItemStorageKey> view = itemViews.get(index);
            view.getSnapshot()
                .getTemplate()
                .writeToNBT(template);
            tag.setLong(
                "Amount",
                view.getSnapshot()
                    .getAmount());
            tag.setLong("Capacity", view.getCapacity());
        } else if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) {
            StorageResourceView<BigFluidStack, FluidStorageKey> view = fluidViews.get(index);
            view.getSnapshot()
                .getTemplate()
                .writeToNBT(template);
            tag.setLong(
                "Amount",
                view.getSnapshot()
                    .getAmount());
            tag.setLong("Capacity", view.getCapacity());
        } else {
            writeAspectResource(index, tag, template);
        }
        tag.setTag("Template", template);
        return tag;
    }

    @Optional.Method(modid = "Thaumcraft")
    private void writeAspectResource(int index, NBTTagCompound tag, NBTTagCompound template) {
        StorageResourceView<BigAspectStack, AspectStorageKey> view = aspectViews.get(index);
        template.setString(
            "Aspect",
            view.getSnapshot()
                .getAspect()
                .getTag());
        tag.setLong(
            "Amount",
            view.getSnapshot()
                .getAmount());
        tag.setLong("Capacity", view.getCapacity());
    }

    private void synchronizeResources() {
        if (!snapshotDirty || !pendingSnapshots.isEmpty()) return;
        snapshotDirty = false;
        List<NBTTagCompound> current = resourcesDirty ? new ArrayList<>() : sentResources;
        if (resourcesDirty) {
            for (int index = 0; index < itemViews.size(); index++)
                current.add(resourceData(index, AdaptiveMatrixTile.TERMINAL_ITEMS));
            for (int index = 0; index < fluidViews.size(); index++)
                current.add(resourceData(index, AdaptiveMatrixTile.TERMINAL_FLUIDS));
            for (int index = 0; index < aspectViews.size(); index++)
                current.add(resourceData(index, AdaptiveMatrixTile.TERMINAL_ASPECTS));
        }
        resourcesDirty = false;
        boolean reset = !catalogSent || sentResources.size() != current.size();
        for (int index = 0; !reset && index < current.size(); index++) {
            reset = !current.get(index)
                .getString("Kind")
                .equals(
                    sentResources.get(index)
                        .getString("Kind"))
                || !current.get(index)
                    .getCompoundTag("Template")
                    .equals(
                        sentResources.get(index)
                            .getCompoundTag("Template"));
        }
        if (reset) {
            viewRevision++;
            searchRevision++;
            searchMatches = null;
            searchDirty = true;
            refreshSearch();
        }
        List<NBTTagCompound> changed = new ArrayList<>();
        for (int index = 0; index < current.size(); index++) {
            if (reset || !current.get(index)
                .equals(sentResources.get(index))) changed.add(current.get(index));
        }
        NBTTagCompound state = snapshotState();
        int chunkSize = Math.max(1, FunctionalStorageConfig.ADAPTIVE.terminalEntriesPerPacket);
        if (reset) pendingSnapshots.clear();
        for (int offset = 0; offset < changed.size() || offset == 0; offset += chunkSize) {
            NBTTagCompound chunk = (NBTTagCompound) state.copy();
            chunk.setBoolean("Reset", reset && offset == 0);
            chunk.setInteger("Count", current.size());
            chunk.setBoolean("Complete", offset + chunkSize >= changed.size());
            NBTTagList entries = new NBTTagList();
            for (int index = offset; index < Math.min(changed.size(), offset + chunkSize); index++) {
                entries.appendTag(changed.get(index));
            }
            chunk.setTag("Entries", entries);
            pendingSnapshots.addLast(chunk);
        }
        sentResources = current;
        catalogSent = true;
    }

    private void sendSnapshots() {
        int limit = Math.max(1, FunctionalStorageConfig.ADAPTIVE.terminalPacketsPerTick);
        for (int index = 0; index < limit && !pendingSnapshots.isEmpty(); index++) {
            NBTTagCompound chunk = pendingSnapshots.removeFirst();
            for (Object crafter : crafters) {
                if (crafter instanceof EntityPlayerMP player) {
                    FunctionalStorage.network.sendTo(new StorageSnapshotMessage(windowId, chunk), player);
                }
            }
        }
    }

    public void receiveSnapshot(NBTTagCompound tag) {
        if (!isTransferViewVirtual() || !tile.getWorldObj().isRemote) return;
        receivingCatalog = true;
        if (tag.getBoolean("Reset")) {
            receivingReset = true;
            itemViews = new ArrayList<>();
            fluidViews = new ArrayList<>();
            aspectViews = new ArrayList<>();
            receivingCatalog = true;
        }
        NBTTagList entries = tag.getTagList("Entries", 10);
        for (int entry = 0; entry < entries.tagCount(); entry++) {
            NBTTagCompound data = entries.getCompoundTagAt(entry);
            int index = data.getInteger("Index");
            if (index < 0 || index >= tag.getInteger("Count")) continue;
            long amount = Math.max(0L, data.getLong("Amount"));
            long capacity = Math.max(0L, data.getLong("Capacity"));
            NBTTagCompound template = data.getCompoundTag("Template");
            String kind = data.getString("Kind");
            if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind)) {
                while (itemViews.size() <= index)
                    itemViews.add(new StorageResourceView<>(BigItemStack.empty(), 0L, false));
                itemViews.set(
                    index,
                    new StorageResourceView<>(new BigItemStack(ItemUtil.readStack(template), amount), capacity, false));
            } else if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) {
                while (fluidViews.size() <= index)
                    fluidViews.add(new StorageResourceView<>(BigFluidStack.empty(), 0L, false));
                fluidViews.set(
                    index,
                    new StorageResourceView<>(
                        new BigFluidStack(FluidStack.loadFluidStackFromNBT(template), amount),
                        capacity,
                        false));
            } else {
                readAspectResource(index, template, amount, capacity);
            }
        }
        if (tag.getBoolean("Complete")) {
            boolean searchChanged = !snapshotReady || receivingReset || !viewMode.equals(tag.getString("Mode"));
            session = tag.getLong("Session");
            viewRevision = tag.getLong("Revision");
            viewMode = tag.getString("Mode");
            online = tag.getBoolean("Online");
            rfStored = tag.getInteger("Energy");
            rfCapacity = tag.getInteger("EnergyCapacity");
            rfCost = tag.getInteger("EnergyCost");
            unassignedDisplayResources = tag.getInteger("Unassigned");
            sortId = tag.getString("Sort");
            layoutId = tag.getString("Layout");
            orderId = tag.getString("Order");
            scrollRow = tag.getInteger("Row");
            maxScrollRow = tag.getInteger("MaxRow");
            int[] visible = tag.getIntArray("Visible");
            if (visible.length == MAX_VISIBLE_STORAGE_SLOTS) visibleResources = visible;
            if (searchChanged) searchRevision++;
            receivingCatalog = false;
            receivingReset = false;
            snapshotReady = true;
            searchDirty = false;
        }
    }

    @Optional.Method(modid = "Thaumcraft")
    private void readAspectResource(int index, NBTTagCompound template, long amount, long capacity) {
        while (aspectViews.size() <= index)
            aspectViews.add(new StorageResourceView<>(BigAspectStack.empty(), 0L, false));
        aspectViews.set(
            index,
            new StorageResourceView<>(
                new BigAspectStack(Aspect.getAspect(template.getString("Aspect")), amount),
                capacity,
                false));
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        closeSubscriptions();
        super.onContainerClosed(player);
    }

    private int transferResourceCount() {
        if (isItemTerminal()) return itemViews.size();
        if (isFluidTerminal()) return fluidViews.size();
        if (isAspectTerminal()) return aspectViews.size();
        if (isAllTerminal()) return itemViews.size() + fluidViews.size() + aspectViews.size();
        return 0;
    }

    private String resourceKind(int index) {
        if (index < 0 || index >= transferResourceCount()) return "";
        if (isAllTerminal()) {
            if (index < itemViews.size()) return AdaptiveMatrixTile.TERMINAL_ITEMS;
            index -= itemViews.size();
            if (index < fluidViews.size()) return AdaptiveMatrixTile.TERMINAL_FLUIDS;
            return AdaptiveMatrixTile.TERMINAL_ASPECTS;
        }
        if (isItemTerminal()) return AdaptiveMatrixTile.TERMINAL_ITEMS;
        if (isFluidTerminal()) return AdaptiveMatrixTile.TERMINAL_FLUIDS;
        return AdaptiveMatrixTile.TERMINAL_ASPECTS;
    }

    private int resourceIndex(int index, String kind) {
        if (index < 0 || index >= transferResourceCount()) return -1;
        if (isAllTerminal()) {
            if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind)) return index < itemViews.size() ? index : -1;
            index -= itemViews.size();
            if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind))
                return index >= 0 && index < fluidViews.size() ? index : -1;
            index -= fluidViews.size();
            return AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind) && index >= 0 && index < aspectViews.size() ? index
                : -1;
        }
        if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind) && isItemTerminal()) return index;
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind) && isFluidTerminal()) return index;
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind) && isAspectTerminal()) return index;
        return -1;
    }

    private int transferResourceIndex(int menuSlot) {
        if (menuSlot < 0 || menuSlot >= MAX_VISIBLE_STORAGE_SLOTS) return -1;
        if (tile.getWorldObj().isRemote) return isSnapshotReady() ? visibleResources[menuSlot] : -1;
        int visible = scrollRow * TERMINAL_COLUMNS + menuSlot;
        if (query.isEmpty()) return visible < transferResourceCount() ? visible : -1;
        return visible < filteredResources.size() ? filteredResources.get(visible) : -1;
    }

    private void refreshSearch() {
        if (!isTransferViewVirtual() || !searchDirty) return;
        filteredResources.clear();
        int count = transferResourceCount();
        if (!query.isEmpty()) {
            for (int index = 0; index < count; index++) {
                if (searchMatches != null ? searchMatches.get(index) : matchesResource(index, query)) {
                    filteredResources.add(index);
                }
            }
        }
        int visibleCount = query.isEmpty() ? count : filteredResources.size();
        maxScrollRow = Math.max(0, (visibleCount + TERMINAL_COLUMNS - 1) / TERMINAL_COLUMNS - TERMINAL_ROWS);
        scrollRow = Math.min(scrollRow, maxScrollRow);
        searchDirty = false;
    }

    private boolean matchesResource(int index, String text) {
        String value = resourceSearchText(index);
        return value != null && value.toLowerCase(Locale.ROOT)
            .contains(text);
    }

    private String resourceSearchText(int index) {
        BigItemStack itemSnapshot = getViewItemSnapshot(index);
        if (itemSnapshot.hasTemplate()) {
            ItemStack item = itemSnapshot.getTemplate();
            if (item == null) return "";
            return item.getDisplayName() + " " + Item.itemRegistry.getNameForObject(item.getItem());
        }
        BigFluidStack fluidSnapshot = getViewFluidSnapshot(index);
        if (fluidSnapshot.hasTemplate()) {
            FluidStack fluid = fluidSnapshot.getTemplate();
            return fluid == null ? ""
                : fluid.getLocalizedName() + " "
                    + fluid.getFluid()
                        .getName();
        }
        BigAspectStack aspect = getViewAspectSnapshot(index);
        if (aspect.hasTemplate()) {
            return aspect.getAspect() == null ? ""
                : aspect.getAspect()
                    .getName();
        }
        return "";
    }

    @Override
    public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
        if (!canInteractWith(player)) return null;
        boolean storage = index >= 0 && index < storageSlotCount;
        if (mode == 5) return super.slotClick(index, button, mode, player);
        if (mode == 0 && (button == 0 || button == 1)
            && index >= 0
            && index < inventorySlots.size()
            && inventorySlots.get(index) instanceof UpgradeSlot upgradeSlot
            && player.inventory.getItemStack() != null) {
            replaceUpgrade(player, upgradeSlot);
            synchronize(player);
            return null;
        }
        if (!storage && mode != 1 && mode != 6) return super.slotClick(index, button, mode, player);
        if (tile.getWorldObj().isRemote) return null;
        func_94533_d();
        if (mode == 1 && (button == 0 || button == 1)
            && storage
            && player.inventory.getItemStack() != null
            && (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(getTransferResourceKind(index))
                || AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(getTransferResourceKind(index))
                || isAllTerminal() && getTransferResourceKind(index).isEmpty())) {
            clickStorage(player, index, button);
        } else if (mode == 1 && (button == 0 || button == 1)) {
            transferStackInSlot(player, index);
        } else if (mode == 2 && storage && (button >= 0 && button < 9)) {
            clickHotbarResource(player, index, button);
        } else if (mode == 6 && (button == 0 || button == 1)) {
            collectToCursor(player);
            super.slotClick(index, button, mode, player);
        } else if (storage && mode == 0 && (button == 0 || button == 1)) {
            clickStorage(player, index, button);
        } else if (storage && AdaptiveMatrixTile.TERMINAL_ITEMS.equals(getTransferResourceKind(index))
            && tile.getItemHandler() != null) {
                clickItemShortcut(player, index, button, mode);
            }
        synchronize(player);
        return null;
    }

    private void clickHotbarResource(EntityPlayer player, int index, int hotbarSlot) {
        String kind = getTransferResourceKind(index);
        if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind) || kind.isEmpty() && isAllTerminal()) {
            clickItemShortcut(player, index, hotbarSlot, 2);
            return;
        }
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind) && tile.getFluidHandler() != null) {
            BigFluidStack selected = getTransferFluidSnapshot(index);
            FluidStack template = selected.getTemplate();
            ItemStack held = player.inventory.getStackInSlot(hotbarSlot);
            if (template == null || held == null && selected.getAmount() <= 0L) return;
            if (FluidContainerInteraction.isFilledFluidContainer(held)) {
                FluidContainerInteraction
                    .depositInventorySlot(player, tile.getFluidHandler(), hotbarSlot, template, Integer.MAX_VALUE);
            } else {
                FluidContainerInteraction
                    .fillInventorySlot(player, tile.getFluidHandler(), hotbarSlot, selected, Integer.MAX_VALUE);
            }
            return;
        }
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind) && tile.getAspectHandler() != null) {
            BigAspectStack selected = getTransferAspectSnapshot(index);
            ItemStack held = player.inventory.getStackInSlot(hotbarSlot);
            if (!selected.hasTemplate() || held == null && selected.getAmount() <= 0L) return;
            if (EssentiaContainerRegistry.getContainedAspect(held) != null) {
                EssentiaContainerRegistry.depositInventorySlot(
                    player,
                    tile.getAspectHandler(),
                    hotbarSlot,
                    selected.getAspect(),
                    Integer.MAX_VALUE);
            } else {
                EssentiaContainerRegistry
                    .fillInventorySlot(player, tile.getAspectHandler(), hotbarSlot, selected, Integer.MAX_VALUE);
            }
        }
    }

    private void replaceUpgrade(EntityPlayer player, UpgradeSlot slot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor == null || !slot.isItemValid(cursor)) {
            return;
        }
        ItemStack previous = slot.getStack() == null ? null
            : slot.getStack()
                .copy();
        ItemStack installed = cursor.copy();
        installed.stackSize = 1;
        cursor.stackSize--;
        tile.setUpgradeSlot(slot.storage, slot.getSlotIndex(), installed);
        if (previous != null) {
            player.inventory.setItemStack(previous);
            if (cursor.stackSize > 0 && !player.inventory.addItemStackToInventory(cursor)) {
                player.dropPlayerItemWithRandomChoice(cursor, false);
            }
        } else {
            player.inventory.setItemStack(cursor.stackSize > 0 ? cursor : null);
        }
    }

    private void clickStorage(EntityPlayer player, int index, int button) {
        ItemStack cursor = player.inventory.getItemStack();
        String kind = getTransferResourceKind(index);
        if (kind.isEmpty() && isAllTerminal()) {
            if (FluidContainerInteraction.isFilledFluidContainer(cursor)) kind = AdaptiveMatrixTile.TERMINAL_FLUIDS;
            else if (EssentiaContainerRegistry.getContainedAspect(cursor) != null)
                kind = AdaptiveMatrixTile.TERMINAL_ASPECTS;
            else kind = AdaptiveMatrixTile.TERMINAL_ITEMS;
        }
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) {
            if (isTransferViewVirtual()) {
                BigFluidStack selected = getTransferFluidSnapshot(index);
                FluidContainerInteraction
                    .activateCursor(player, tile.getFluidHandler(), selected.hasTemplate() ? selected : null, null);
            } else {
                FluidContainerInteraction.activateCursor(
                    player,
                    tile.getFluidHandler(),
                    index,
                    tile instanceof FluidDrawerTile fluidDrawer ? fluidDrawer::prepareLockedSlot
                        : tile instanceof StorageNetworkTile network ? network::prepareLockedFluidSlot : null);
            }
            return;
        }
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind)) {
            if (isTransferViewVirtual()) {
                BigAspectStack selected = getTransferAspectSnapshot(index);
                EssentiaContainerRegistry
                    .activateCursor(player, tile.getAspectHandler(), selected.hasTemplate() ? selected : null, null);
            } else {
                EssentiaContainerRegistry.activateCursor(
                    player,
                    tile.getAspectHandler(),
                    index,
                    tile instanceof EssentiaDrawerTile essentiaDrawer ? essentiaDrawer::prepareLockedSlot
                        : tile instanceof StorageNetworkTile network ? network::prepareLockedAspectSlot : null);
            }
            return;
        }
        IBigItemHandler handler = tile.getItemHandler();
        if (handler != null) {
            if (cursor == null) {
                ItemStack available = extract(index, 64, StorageAction.SIMULATE);
                if (available != null) player.inventory.setItemStack(
                    extract(
                        index,
                        button == 1 ? (available.stackSize + 1) / 2 : available.stackSize,
                        StorageAction.EXECUTE));
            } else {
                if (isTransferViewVirtual() || prepareLockedSlot(index, cursor)) {
                    insert(index, cursor, button == 1 ? 1 : cursor.stackSize);
                }
                if (cursor.stackSize == 0) player.inventory.setItemStack(null);
            }
        } else if (tile.getFluidHandler() != null) {
            FluidContainerInteraction.activateCursor(
                player,
                tile.getFluidHandler(),
                index,
                tile instanceof FluidDrawerTile fluidDrawer ? fluidDrawer::prepareLockedSlot
                    : tile instanceof StorageNetworkTile network ? network::prepareLockedFluidSlot : null);
        } else if (tile.getAspectHandler() != null) {
            EssentiaContainerRegistry.activateCursor(
                player,
                tile.getAspectHandler(),
                index,
                tile instanceof EssentiaDrawerTile essentiaDrawer ? essentiaDrawer::prepareLockedSlot
                    : tile instanceof StorageNetworkTile network ? network::prepareLockedAspectSlot : null);
        }
    }

    private void clickItemShortcut(EntityPlayer player, int index, int button, int mode) {
        if (mode == 2 && button >= 0 && button < 9) {
            ItemStack hotbar = player.inventory.getStackInSlot(button);
            if (hotbar == null)
                player.inventory.setInventorySlotContents(button, extract(index, 64, StorageAction.EXECUTE));
            else {
                if (prepareLockedSlot(index, hotbar)) {
                    insert(index, hotbar, hotbar.stackSize);
                }
                if (hotbar.stackSize == 0) player.inventory.setInventorySlotContents(button, null);
            }
        } else if (mode == 3 && player.capabilities.isCreativeMode && player.inventory.getItemStack() == null) {
            ItemStack copy = extract(index, 64, StorageAction.SIMULATE);
            if (copy != null) {
                copy.stackSize = copy.getMaxStackSize();
                player.inventory.setItemStack(copy);
            }
        } else if (mode == 4 && player.inventory.getItemStack() == null && (button == 0 || button == 1)) {
            ItemStack dropped = extract(index, button == 0 ? 1 : 64, StorageAction.EXECUTE);
            if (dropped != null) player.dropPlayerItemWithRandomChoice(dropped, true);
        }
    }

    private void collectToCursor(EntityPlayer player) {
        if (!isItemTerminal() && !isAllTerminal()) return;
        ItemStack cursor = player.inventory.getItemStack();
        IBigItemHandler handler = tile.getItemHandler();
        if (cursor == null || handler == null) return;
        int space = cursor.getMaxStackSize() - cursor.stackSize;
        if (space > 0)
            cursor.stackSize += (int) handler.extractRouted(new BigItemStack(cursor, space), StorageAction.EXECUTE)
                .getProcessedAmount();
    }

    private void insert(int index, ItemStack stack, int amount) {
        IBigItemHandler handler = tile.getItemHandler();
        if (handler == null || stack == null || amount <= 0) return;
        BigItemStack request = new BigItemStack(stack, amount);
        long accepted;
        if (isTransferViewVirtual()) {
            BigItemStack target = getTransferItemSnapshot(index);
            if (target.hasTemplate() && !target.isSameType(stack)) return;
            accepted = handler.insertRouted(request, StorageAction.EXECUTE)
                .getProcessedAmount();
        } else {
            accepted = handler.insert(index, request, StorageAction.EXECUTE)
                .getProcessedAmount();
        }
        stack.stackSize -= (int) Math.min(stack.stackSize, Math.max(0L, accepted));
    }

    private boolean prepareLockedSlot(int index, ItemStack stack) {
        IBigItemHandler handler = tile.getItemHandler();
        if (handler == null || stack == null || stack.stackSize <= 0) {
            return false;
        }
        if (!tile.isLocked() || handler.getSnapshot(index)
            .hasTemplate()) {
            return true;
        }
        return tile.setItemFilter(index, stack);
    }

    private long insertForGui(ItemStack stack) {
        IBigItemHandler handler = tile.getItemHandler();
        if (handler == null || stack == null || stack.stackSize <= 0) {
            return 0L;
        }
        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        long accepted = handler.insertRouted(request, StorageAction.EXECUTE)
            .getProcessedAmount();
        if (isTransferViewVirtual()) return accepted;
        if (accepted > 0L || !tile.isLocked()) {
            return accepted;
        }
        for (int index = 0; index < storageSlotCount; index++) {
            if (!handler.getSnapshot(index)
                .isEmpty() || !prepareLockedSlot(index, stack)) {
                continue;
            }
            accepted = handler.insert(index, request, StorageAction.EXECUTE)
                .getProcessedAmount();
            if (accepted > 0L) {
                return accepted;
            }
        }
        return 0L;
    }

    private ItemStack extract(int index, int amount, StorageAction action) {
        IBigItemHandler handler = tile.getItemHandler();
        if (handler == null || amount <= 0) return null;
        BigItemStack selected = getTransferItemSnapshot(index);
        ItemStack template = selected.getTemplate();
        return template == null ? null
            : isTransferViewVirtual()
                ? handler
                    .extractRouted(new BigItemStack(template, Math.min(amount, template.getMaxStackSize())), action)
                    .getProcessed()
                    .toItemStack()
                : handler.extract(index, Math.min(amount, template.getMaxStackSize()), action)
                    .getProcessed()
                    .toItemStack();
    }

    private void synchronize(EntityPlayer player) {
        player.inventory.markDirty();
        detectAndSendChanges();
        // Vanilla suppresses cursor updates after accepting a click transaction.
        if (player instanceof EntityPlayerMP serverPlayer && serverPlayer.playerNetServerHandler != null) {
            serverPlayer.updateHeldItem();
        }
    }

    @Override
    public boolean canDragIntoSlot(Slot slot) {
        return !(slot instanceof StorageSlot);
    }

    @Override
    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return stack == null || !(slot instanceof StorageSlot);
    }

    @Nullable
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        if (tile.getWorldObj().isRemote || !canInteractWith(player) || index < 0 || index >= inventorySlots.size())
            return null;
        int playerStart = storageSlotCount + tile.getStorageUpgradeSlots() + tile.getUtilityUpgradeSlots();
        if (index < storageSlotCount) {
            String kind = getTransferResourceKind(index);
            if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind) && tile.getItemHandler() != null) {
                ItemStack available = extract(index, 64, StorageAction.SIMULATE);
                if (available == null) return null;
                ItemStack original = available.copy();
                if (!mergeItemStack(available, playerStart, playerInventoryEnd(), true)) return null;
                extract(index, original.stackSize - available.stackSize, StorageAction.EXECUTE);
                return original;
            }
            if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind) && tile.getFluidHandler() != null) {
                if (isTransferViewVirtual()) {
                    FluidContainerInteraction.fillInventory(
                        player,
                        tile.getFluidHandler(),
                        getTransferFluidSnapshot(index),
                        Integer.MAX_VALUE);
                } else {
                    FluidContainerInteraction.fillInventory(player, tile.getFluidHandler(), index);
                }
                return null;
            }
            if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind) && tile.getAspectHandler() != null) {
                if (isTransferViewVirtual()) {
                    EssentiaContainerRegistry.fillInventory(
                        player,
                        tile.getAspectHandler(),
                        getTransferAspectSnapshot(index),
                        Integer.MAX_VALUE);
                } else {
                    EssentiaContainerRegistry.fillInventory(player, tile.getAspectHandler(), index);
                }
                return null;
            }
            return null;
        }
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < playerStart) {
            mergeItemStack(stack, playerStart, playerInventoryEnd(), true);
        } else {
            if (stack.getItem() instanceof IStorageUpgrade) {
                moveUpgradesToSlots(stack, storageSlotCount, playerStart);
            }
            if (isAllTerminal()) {
                return transferStackToAllResources(player, slot, original);
            }
            if (stack.stackSize > 0 && isItemTerminal() && tile.getItemHandler() != null) {
                stack.stackSize -= (int) Math.min(stack.stackSize, insertForGui(stack));
            } else if (stack.stackSize > 0 && isFluidTerminal() && tile.getFluidHandler() != null) {
                if (FluidContainerInteraction
                    .depositInventorySlot(player, tile.getFluidHandler(), slot.getSlotIndex(), false) == 0) return null;
                return original;
            } else if (stack.stackSize > 0 && isAspectTerminal() && tile.getAspectHandler() != null) {
                if (EssentiaContainerRegistry.depositInventorySlot(player, tile.getAspectHandler(), slot.getSlotIndex())
                    == 0) return null;
                return original;
            }
        }
        if (stack.stackSize == original.stackSize) return null;
        if (stack.stackSize <= 0) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    private void moveUpgradesToSlots(ItemStack stack, int start, int end) {
        for (int targetIndex = start; targetIndex < end && stack.stackSize > 0; targetIndex++) {
            Slot target = inventorySlots.get(targetIndex);
            if (target.getHasStack() || !target.isItemValid(stack)) {
                continue;
            }
            ItemStack single = stack.splitStack(1);
            target.putStack(single);
            target.onSlotChanged();
        }
    }

    private ItemStack transferStackToAllResources(EntityPlayer player, Slot slot, ItemStack original) {
        int inventorySlot = slot.getSlotIndex();
        ItemStack before = player.inventory.getStackInSlot(inventorySlot);
        before = before == null ? null : before.copy();
        if (tile.getFluidHandler() != null) {
            FluidContainerInteraction.depositInventorySlot(player, tile.getFluidHandler(), inventorySlot, false);
        }
        if (tile.getAspectHandler() != null) {
            EssentiaContainerRegistry.depositInventorySlot(player, tile.getAspectHandler(), inventorySlot);
        }
        ItemStack remaining = player.inventory.getStackInSlot(inventorySlot);
        boolean containerChanged = !ItemStack.areItemStacksEqual(before, remaining);
        int transferred = containerChanged ? 1 : 0;
        if (!containerChanged && remaining != null && tile.getItemHandler() != null) {
            long accepted = tile.getItemHandler()
                .insertRouted(new BigItemStack(remaining, remaining.stackSize), StorageAction.EXECUTE)
                .getProcessedAmount();
            int inserted = (int) Math.min(remaining.stackSize, Math.max(0L, accepted));
            remaining.stackSize -= inserted;
            transferred += inserted;
            if (remaining.stackSize <= 0) player.inventory.setInventorySlotContents(inventorySlot, null);
        }
        if (transferred == 0) return null;
        if (player.inventory.getStackInSlot(inventorySlot) == null) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    private int visibleStorageSlots() {
        return storageSlotCount;
    }

    private boolean isItemTerminal() {
        if (isTransferViewVirtual() && tile.getWorldObj().isRemote)
            return AdaptiveMatrixTile.TERMINAL_ITEMS.equals(viewMode);
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalItems();
        return tile.getItemHandler() != null;
    }

    private boolean isFluidTerminal() {
        if (isTransferViewVirtual() && tile.getWorldObj().isRemote)
            return AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(viewMode);
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalFluids();
        return tile.getItemHandler() == null && tile.getFluidHandler() != null;
    }

    private boolean isAspectTerminal() {
        if (isTransferViewVirtual() && tile.getWorldObj().isRemote)
            return AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(viewMode);
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalAspects();
        return tile.getItemHandler() == null && tile.getFluidHandler() == null && tile.getAspectHandler() != null;
    }

    private boolean isAllTerminal() {
        if (isTransferViewVirtual() && tile.getWorldObj().isRemote) {
            return AdaptiveMatrixTile.TERMINAL_ALL.equals(viewMode);
        }
        return tile instanceof AdaptiveGridTile grid && AdaptiveMatrixTile.TERMINAL_ALL.equals(grid.getTerminalMode());
    }

    private static boolean isAdaptiveTerminal(ControllableDrawerTile tile) {
        return tile instanceof AdaptiveGridTile;
    }

    @Nullable
    private IStorageHandler<?, ?> displayStorage() {
        if (isFluidTerminal()) return tile.getFluidHandler();
        if (isAspectTerminal()) return tile.getAspectHandler();
        if (isItemTerminal()) return tile.getItemHandler();
        return tile.getActiveStorage();
    }

    /** A synchronized icon; storage mutations belong to the container click handler. */
    public static class StorageSlot extends Slot {

        public StorageSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
        }
    }

    /**
     * Slot that admits only upgrades of the matching group.
     */
    public static class UpgradeSlot extends Slot {

        private final boolean storage;

        public UpgradeSlot(IInventory inventory, int index, boolean storage, int x, int y) {
            super(inventory, index, x, y);
            this.storage = storage;
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (stack.getItem() == null || !(stack.getItem() instanceof IStorageUpgrade)) {
                return false;
            }
            boolean storageUpgrade = ((IStorageUpgrade) stack.getItem()).isStorageUpgrade();
            return storage == storageUpgrade && inventory.isItemValidForSlot(getSlotIndex(), stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return ((UpgradeSlotInventory) inventory).canRemove(getSlotIndex());
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
