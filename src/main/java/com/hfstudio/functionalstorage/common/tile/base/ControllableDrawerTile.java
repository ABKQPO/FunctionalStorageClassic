package com.hfstudio.functionalstorage.common.tile.base;

import java.util.Arrays;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.StorageViewCache;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.AbstractStorageHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeSettings;
import com.hfstudio.functionalstorage.common.options.DrawerOptions;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import lombok.Getter;

/** Owns upgrades, display options, locking, and the active storage subscription. */
public abstract class ControllableDrawerTile extends TileEntity {

    private static final String KEY_STORAGE_UPGRADES = "StorageUpgrades";
    private static final String KEY_UTILITY_UPGRADES = "UtilityUpgrades";
    private static final String KEY_OPTIONS = "DrawerOptions";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_CONTROLLER = "ControllerPos";
    private static final String KEY_TILE_DATA = "TileData";
    private static final String KEY_SLOT = "Slot";

    private static final int STORAGE_UPGRADE_SLOTS = 4;
    private static final int UTILITY_UPGRADE_SLOTS = 3;
    private static final long REPEAT_WINDOW_TICKS = 10L;

    private final ItemStack[] storageUpgrades = new ItemStack[STORAGE_UPGRADE_SLOTS];
    private final ItemStack[] utilityUpgrades = new ItemStack[UTILITY_UPGRADE_SLOTS];
    private final DrawerOptions drawerOptions = new DrawerOptions();

    @Getter
    private boolean locked;
    private boolean upgradeCacheDirty = true;
    private UpgradeState cachedUpgradeState = UpgradeState.empty();

    private int controllerX = Integer.MIN_VALUE;
    private int controllerY = Integer.MIN_VALUE;
    private int controllerZ = Integer.MIN_VALUE;

    private StorageSubscription storageSubscription = StorageSubscription.CLOSED;
    private IStorageHandler<?, ?> subscribedStorage;
    private boolean pendingUpdatePacket;
    private boolean layoutValidated;
    private IInventory inventoryView;
    private UUID lastInteractionPlayer;
    private long lastInteractionTick = Long.MIN_VALUE;
    private int lastInteractionSlot = -1;
    private int lastRedstoneSignal = -1;

    @Nullable
    public IBigItemHandler getItemHandler() {
        return null;
    }

    /** Sets a locked drawer's retained template without inserting any items. */
    public boolean setItemFilter(int slot, @Nonnull ItemStack template) {
        return worldObj != null && !worldObj.isRemote
            && isLocked()
            && getItemHandler() instanceof BigItemHandler handler
            && handler.setSlotFilter(slot, new BigItemStack(template, 0L));
    }

    @Nullable
    public IBigFluidHandler getFluidHandler() {
        return null;
    }

    @Nullable
    public IBigAspectHandler getAspectHandler() {
        return null;
    }

    @Nullable
    public IStorageHandler<?, ?> getActiveStorage() {
        IBigItemHandler itemHandler = getItemHandler();
        if (itemHandler != null) {
            return itemHandler;
        }
        IBigFluidHandler fluidHandler = getFluidHandler();
        if (fluidHandler != null) {
            return fluidHandler;
        }
        return getAspectHandler();
    }

    @Getter
    private FramedDrawerStyle style = FramedDrawerStyle.EMPTY;
    @Getter
    private int priority;

    public void setStyle(FramedDrawerStyle style) {
        if (this.style.equals(style)) return;
        this.style = style;
        markOptionsDirty();
    }

    public void setPriority(int priority) {
        int updated = Math.max(0, Math.min(999999999, priority));
        if (this.priority == updated) return;
        this.priority = updated;
        if (worldObj != null && controllerX != Integer.MIN_VALUE
            && worldObj.blockExists(controllerX, controllerY, controllerZ)
            && worldObj
                .getTileEntity(controllerX, controllerY, controllerZ) instanceof DrawerControllerTile controller) {
            controller.invalidateNetwork();
        }
        markOptionsDirty();
    }

    public boolean isLinkedTo(int x, int y, int z) {
        return controllerX == x && controllerY == y && controllerZ == z;
    }

    public int getStorageUpgradeSlots() {
        return STORAGE_UPGRADE_SLOTS;
    }

    public int getUtilityUpgradeSlots() {
        return UTILITY_UPGRADE_SLOTS;
    }

    public boolean hasUpgradeSlots() {
        return getStorageUpgradeSlots() > 0 || getUtilityUpgradeSlots() > 0;
    }

    /** Lazily creates the physical inventory view; non-item drawers return null. */
    @Nullable
    public IInventory getInventoryView() {
        IBigItemHandler itemHandler = getItemHandler();
        if (itemHandler == null) {
            return null;
        }
        if (inventoryView == null) {
            inventoryView = new DrawerItemInventory(
                itemHandler,
                getBlockType() == null ? "container.functionalstorage.drawer"
                    : getBlockType().getUnlocalizedName() + ".name",
                this::onStorageChanged);
        }
        return inventoryView;
    }

    @Nonnull
    public DrawerOptions getDrawerOptions() {
        return drawerOptions;
    }

    /**
     * Persists and synchronizes a change made to this drawer's display options.
     */
    public void markOptionsDirty() {
        markDirty();
        requestUpdatePacket();
    }

    @Nullable
    public ItemStack getStorageUpgrade(int slot) {
        return stackAt(storageUpgrades, slot);
    }

    @Nullable
    public ItemStack getUtilityUpgrade(int slot) {
        return stackAt(utilityUpgrades, slot);
    }

    public boolean canSetUpgradeSlot(boolean storage, int slot, @Nullable ItemStack stack) {
        int limit = storage ? getStorageUpgradeSlots() : getUtilityUpgradeSlots();
        if (slot < 0 || slot >= limit) return false;
        if (stack != null
            && (!(stack.getItem() instanceof IStorageUpgrade upgrade) || upgrade.isStorageUpgrade() != storage
                || hasConflictingUpgrade(stack, storage ? slot : -1)))
            return false;
        if (!storage || getActiveStorage() == null) return true;
        UpgradeState previous = getUpgradeState();
        UpgradeState.Builder builder = UpgradeState.builder();
        for (int index = 0; index < storageUpgrades.length; index++)
            applyUpgrade(builder, index == slot ? stack : storageUpgrades[index]);
        for (ItemStack installed : utilityUpgrades) applyUpgrade(builder, installed);
        IStorageHandler<?, ?> handler = getActiveStorage();
        cachedUpgradeState = builder.build();
        try {
            for (int index = 0; index < handler.getStorageCount(); index++) if (handler.getSnapshot(index)
                .getAmount() > handler.getCapacity(index)) return false;
            return true;
        } finally {
            cachedUpgradeState = previous;
        }
    }

    public void setUpgradeSlot(boolean storage, int slot, @Nullable ItemStack stack) {
        ItemStack[] target = storage ? storageUpgrades : utilityUpgrades;
        if (slot < 0 || slot >= target.length) {
            return;
        }
        target[slot] = stack == null || stack.getItem() == null ? null : stack;
        upgradeCacheDirty = true;
        reconcileStorageConfiguration();
        invalidateStorageViews();
        invalidateStorageLimit();
        markDirty();
        requestUpdatePacket();
    }

    public void onUpgradeSlotChanged(boolean storage, int slot) {
        upgradeCacheDirty = true;
        reconcileStorageConfiguration();
        invalidateStorageViews();
        invalidateStorageLimit();
        markDirty();
        requestUpdatePacket();
    }

    /**
     * Applies a lock transition, retaining or clearing filters and emitting a
     * single resynchronization event for the active storage.
     *
     * @param locked new lock state
     */
    public void setLocked(boolean locked) {
        if (this.locked == locked) {
            return;
        }
        this.locked = locked;
        this.upgradeCacheDirty = true;
        IStorageHandler<?, ?> storage = getActiveStorage();
        if (storage instanceof AbstractStorageHandler) {
            ((AbstractStorageHandler<?, ?>) storage).applyLockConfiguration(locked);
        }
        invalidateStorageViews();
        invalidateStorageLimit();
        markDirty();
        requestUpdatePacket();
    }

    public void toggleLocking() {
        setLocked(!isLocked());
    }

    public void applyConfiguration(ConfigurationAction action) {
        if (action == ConfigurationAction.LOCKING) setLocked(!isLocked());
        else {
            drawerOptions.cycle(action);
            markOptionsDirty();
        }
    }

    @Nullable
    public int[] getControllerPosition() {
        return controllerX == Integer.MIN_VALUE ? null : new int[] { controllerX, controllerY, controllerZ };
    }

    public void setControllerPosition(int x, int y, int z) {
        this.controllerX = x;
        this.controllerY = y;
        this.controllerZ = z;
        markDirty();
        requestUpdatePacket();
    }

    public void clearControllerPosition() {
        this.controllerX = Integer.MIN_VALUE;
        this.controllerY = Integer.MIN_VALUE;
        this.controllerZ = Integer.MIN_VALUE;
        markDirty();
        requestUpdatePacket();
    }

    public void detachFromController(@Nonnull World world) {
        if (controllerX == Integer.MIN_VALUE) {
            return;
        }
        TileEntity tile = world.getTileEntity(controllerX, controllerY, controllerZ);
        if (tile instanceof DrawerControllerTile) {
            ((DrawerControllerTile) tile).removeDrawer(xCoord, yCoord, zCoord);
        }
        clearControllerPosition();
    }

    public int getRedstoneSignal(int side) {
        IStorageHandler<?, ?> storage = getActiveStorage();
        if (storage == null) return 0;
        int signal = 0;
        for (ItemStack stack : utilityUpgrades) {
            if (stack == null || !(stack.getItem() instanceof RedstoneUpgradeItem upgrade)) continue;
            int slot = upgrade.getSlot(stack);
            long capacity = storage.getCapacity(slot);
            if (capacity > 0) signal = Math.max(
                signal,
                redstoneForRatio(
                    storage.getSnapshot(slot)
                        .getAmount() / (double) capacity));
        }
        return signal;
    }

    public boolean hasRedstoneUpgrade() {
        return getUpgradeState().hasFeature(StorageFeature.REDSTONE_OUTPUT);
    }

    protected int calculateRedstoneSignal() {
        return 0;
    }

    /** Reports fill level even without a redstone upgrade. */
    public int getComparatorOutput() {
        return calculateRedstoneSignal();
    }

    protected static int redstoneForRatio(double ratio) {
        if (ratio <= 0D) {
            return 0;
        }
        if (ratio >= 1D) {
            return 15;
        }
        return (int) Math.ceil(ratio * 14D);
    }

    public boolean onSlotActivated(@Nonnull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        ItemStack held = player.getHeldItem();
        if (held == null) {
            if (player.isSneaking() || opensGuiOnEmptyHand()) {
                return openGui(player);
            }
            return activateItemSlot(player, slot);
        }
        if (held.getItem() == RegistrationHandler.configurationTool) {
            return false;
        }
        if (held.getItem() == RegistrationHandler.linkingTool) {
            return false;
        }
        if (held.getItem() instanceof IStorageUpgrade upgrade && upgrade.isStorageUpgrade()) {
            tryInstallStorageUpgrade(player, held);
            return true;
        }
        if (held.getItem() instanceof IStorageUpgrade) {
            tryInstallUtilityUpgrade(player, held);
            return true;
        }
        if (opensGuiOnEmptyHand()) {
            return openGui(player);
        }
        return activateItemSlot(player, slot);
    }

    protected boolean opensGuiOnEmptyHand() {
        return false;
    }

    public boolean openGui(@Nonnull EntityPlayer player) {
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        player.openGui(FunctionalStorage.instance, GuiHandler.GUI_DRAWER, worldObj, xCoord, yCoord, zCoord);
        return true;
    }

    /** Extracts one item from the selected physical slot, or a stack while sneaking. */
    public void onSlotClicked(@Nonnull EntityPlayer player, int slot) {
        if (worldObj == null || worldObj.isRemote || slot < 0) {
            return;
        }
        IBigItemHandler itemHandler = getItemHandler();
        if (itemHandler == null) {
            return;
        }
        BigItemStack snapshot = itemHandler.getSnapshot(slot);
        ItemStack template = snapshot.getTemplate();
        if (template == null) {
            return;
        }
        int amount = player.isSneaking() ? Math.max(1, template.getMaxStackSize()) : 1;
        ItemStack extracted = itemHandler.extract(slot, amount, StorageAction.EXECUTE)
            .getProcessed()
            .toItemStack();
        if (extracted == null || extracted.getItem() == null) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(extracted)) {
            player.dropPlayerItemWithRandomChoice(extracted, false);
        }
        player.inventory.markDirty();
        markDirty();
        requestUpdatePacket();
    }

    @Nonnull
    public UpgradeState getUpgradeState() {
        if (upgradeCacheDirty) {
            cachedUpgradeState = computeUpgradeState();
            upgradeCacheDirty = false;
        }
        return cachedUpgradeState;
    }

    public double calculateModifier(@Nonnull UpgradeAttribute attribute, double defaultBase) {
        return getUpgradeState().calculate(attribute, defaultBase);
    }

    public boolean hasMaxStorage() {
        return getUpgradeState().hasFeature(StorageFeature.MAX_CAPACITY);
    }

    public boolean hasEquivalentItems() {
        return getUpgradeState().hasFeature(StorageFeature.EQUIVALENT_ITEMS);
    }

    public boolean isCreative() {
        return getUpgradeState().hasFeature(StorageFeature.CREATIVE);
    }

    public boolean voidsOverflow() {
        return getUpgradeState().hasFeature(StorageFeature.VOID_OVERFLOW);
    }

    /**
     * @return whether the drawer holds nothing and has no upgrades
     */
    public boolean isEverythingEmpty() {
        for (ItemStack stack : storageUpgrades) {
            if (stack != null) {
                return false;
            }
        }
        for (ItemStack stack : utilityUpgrades) {
            if (stack != null) {
                return false;
            }
        }
        IStorageHandler<?, ?> storage = getActiveStorage();
        if (storage != null) {
            for (int index = 0; index < storage.getStorageCount(); index++) {
                if (!storage.getSnapshot(index)
                    .isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nonnull
    public ItemStack createDropStack(@Nonnull ItemStack base) {
        NBTTagCompound tileData = writeTileData(new NBTTagCompound());
        if (tileData.hasNoTags()) {
            return base;
        }
        NBTTagCompound root = base.hasTagCompound() ? base.getTagCompound() : new NBTTagCompound();
        root.setTag(KEY_TILE_DATA, tileData);
        base.setTagCompound(root);
        return base;
    }

    public void loadFromItemStack(@Nonnull ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey(KEY_TILE_DATA, 10)) {
            return;
        }
        readTileData(root.getCompoundTag(KEY_TILE_DATA));
        markDirty();
        requestUpdatePacket();
    }

    public void onBlockBroken() {
        Arrays.fill(storageUpgrades, null);
        Arrays.fill(utilityUpgrades, null);
    }

    @Nonnull
    public NBTTagCompound writeTileData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_STORAGE_UPGRADES, writeStacks(storageUpgrades));
        tag.setTag(KEY_UTILITY_UPGRADES, writeStacks(utilityUpgrades));
        tag.setTag(KEY_OPTIONS, drawerOptions.serializeNBT());
        tag.setBoolean(KEY_LOCKED, locked);
        tag.setInteger("Priority", priority);
        if (style.isConfigured()) tag.setTag(FramedDrawerStyle.NBT_KEY, style.writeToNBT());
        if (controllerX != Integer.MIN_VALUE) {
            tag.setIntArray(KEY_CONTROLLER, new int[] { controllerX, controllerY, controllerZ });
        }
        writeStorageData(tag);
        return tag;
    }

    public void readTileData(@Nonnull NBTTagCompound tag) {
        readStacks(tag.getTagList(KEY_STORAGE_UPGRADES, 10), storageUpgrades);
        readStacks(tag.getTagList(KEY_UTILITY_UPGRADES, 10), utilityUpgrades);
        if (tag.hasKey(KEY_OPTIONS, 10)) {
            drawerOptions.deserializeNBT(tag.getCompoundTag(KEY_OPTIONS));
        }
        locked = tag.getBoolean(KEY_LOCKED);
        priority = Math.max(0, tag.getInteger("Priority"));
        style = FramedDrawerStyle.fromNBT(tag.getCompoundTag(FramedDrawerStyle.NBT_KEY));
        controllerX = Integer.MIN_VALUE;
        controllerY = Integer.MIN_VALUE;
        controllerZ = Integer.MIN_VALUE;
        if (tag.hasKey(KEY_CONTROLLER)) {
            int[] position = tag.getIntArray(KEY_CONTROLLER);
            if (position.length == 3) {
                controllerX = position[0];
                controllerY = position[1];
                controllerZ = position[2];
            }
        }
        readStorageData(tag);
        upgradeCacheDirty = true;
        invalidateStorageLimit();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeTileData(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readTileData(tag);
    }

    @Override
    public void updateEntity() {
        if (!layoutValidated && worldObj != null) {
            layoutValidated = true;
            reconcileBlockLayout();
        }
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        for (int slot = 0; slot < utilityUpgrades.length; slot++) {
            ItemStack stack = utilityUpgrades[slot];
            if (stack != null && stack.getItem() instanceof AutomationUpgradeItem) {
                tickAutomation((AutomationUpgradeItem) stack.getItem(), stack, slot);
            }
        }
        flushPendingUpdatePacket();
    }

    /** Keeps each automation countdown in the upgrade NBT so its interval survives reloads. */
    private void tickAutomation(@Nonnull AutomationUpgradeItem upgrade, @Nonnull ItemStack stack, int slot) {
        int mode = UpgradeSettings.get(stack, "RedstoneMode");
        if (mode == 3) {
            boolean powered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
            boolean previous = UpgradeSettings.get(stack, "LastPowered") != 0;
            if (powered != previous) UpgradeSettings.set(stack, "LastPowered", powered ? 1 : 0);
            if (powered && !previous) upgrade.work(this, stack, slot);
            return;
        }
        if (mode != 0 && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) != (mode == 1)) {
            return;
        }
        int remaining = upgrade.getRemainingTicks(stack) - 1;
        if (remaining > 0) {
            upgrade.setRemainingTicks(stack, remaining);
            return;
        }
        upgrade.setRemainingTicks(stack, upgrade.getTickInterval(stack));
        upgrade.work(this, stack, slot);
    }

    protected final void bindStorageHandler(@Nullable IStorageHandler<?, ?> handler) {
        if (subscribedStorage == handler && !storageSubscription.isClosed()) {
            return;
        }
        if (inventoryView instanceof DrawerItemInventory inventory) {
            inventory.flushChanges();
        }
        if (worldObj != null && controllerX != Integer.MIN_VALUE
            && worldObj.blockExists(controllerX, controllerY, controllerZ)
            && worldObj
                .getTileEntity(controllerX, controllerY, controllerZ) instanceof DrawerControllerTile controller) {
            controller.invalidateNetwork();
        }
        closeStorageSubscription();
        subscribedStorage = handler;
        inventoryView = null;
        if (handler != null) {
            storageSubscription = handler.subscribe(change -> onStorageChanged());
        }
    }

    protected final void closeStorageSubscription() {
        StorageSubscription current = storageSubscription;
        storageSubscription = StorageSubscription.CLOSED;
        if (current != null) {
            current.close();
        }
    }

    protected final void rebuildStorageSubscription() {
        if (subscribedStorage != null && storageSubscription.isClosed()) {
            storageSubscription = subscribedStorage.subscribe(change -> onStorageChanged());
        }
    }

    protected final void onStorageChanged() {
        markDirty();
        requestUpdatePacket();
    }

    @Override
    public void markDirty() {
        if (inventoryView instanceof DrawerItemInventory inventory) {
            inventory.flushChanges();
        }
        super.markDirty();
        notifyRedstoneSignalChange();
    }

    private void notifyRedstoneSignalChange() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        int signal = getRedstoneSignal(0);
        if (signal == lastRedstoneSignal) {
            return;
        }
        lastRedstoneSignal = signal;
        Block block = getBlockType();
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, block);
        worldObj.func_147453_f(xCoord, yCoord, zCoord, block);
    }

    /**
     * Schedules at most one update packet for the next tick.
     */
    protected final void requestUpdatePacket() {
        pendingUpdatePacket = true;
    }

    private void flushPendingUpdatePacket() {
        if (!pendingUpdatePacket) {
            return;
        }
        pendingUpdatePacket = false;
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void sendStorageUpdate(EntityPlayer player) {
        if (pendingUpdatePacket && player instanceof EntityPlayerMP serverPlayer
            && serverPlayer.playerNetServerHandler != null) {
            serverPlayer.playerNetServerHandler.sendPacket(getDescriptionPacket());
        }
    }

    @Override
    public void validate() {
        super.validate();
        layoutValidated = false;
        rebuildStorageSubscription();
    }

    private void reconcileBlockLayout() {
        if (worldObj != null && getBlockType() instanceof DrawerBlock block) {
            NBTTagCompound storage = new NBTTagCompound();
            writeStorageData(storage);
            int slots = block.getFaceLayout()
                .getSlotCount();
            if (storage.hasKey("DrawerLayout")) {
                DrawerLayout expected = slots == 4 ? DrawerLayout.X_4
                    : slots == 2 ? DrawerLayout.X_2 : DrawerLayout.X_1;
                if (!expected.getId()
                    .equals(storage.getString("DrawerLayout"))) {
                    storage.setString("DrawerLayout", expected.getId());
                    readStorageData(storage);
                }
            } else if (storage.hasKey("DrawerSlots") && storage.getInteger("DrawerSlots") != slots) {
                storage.setInteger("DrawerSlots", slots);
                readStorageData(storage);
            }
        }
    }

    @Override
    public void onChunkUnload() {
        closeStorageSubscription();
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        closeStorageSubscription();
        super.invalidate();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeTileData(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readTileData(packet.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }

    @Override
    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y,
        int z) {
        return oldBlock != newBlock;
    }

    protected boolean activateItemSlot(EntityPlayer player, int slot) {
        IBigItemHandler handler = getItemHandler();
        if (worldObj == null || worldObj.isRemote || handler == null || slot < 0 || slot >= handler.getStorageCount()) {
            return false;
        }
        boolean repeated = recordsInteraction(player, slot);
        int target = depositTarget(slot);
        ItemStack held = player.getHeldItem();
        if (held != null && isLocked()
            && !handler.getSnapshot(slot)
                .hasTemplate()
            && handler instanceof BigItemHandler items) {
            items.setSlotFilter(slot, new BigItemStack(held, 0L));
        }
        if (held != null) {
            insertFromInventory(player, handler, target, player.inventory.currentItem);
        }
        if (repeated && acceptsDeposit(handler, slot)) {
            for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length; inventorySlot++) {
                insertFromInventory(player, handler, target, inventorySlot);
            }
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        return held == null || acceptsDeposit(handler, slot);
    }

    protected final boolean recordsInteraction(EntityPlayer player, int slot) {
        long tick = worldObj.getTotalWorldTime();
        boolean repeated = player.getUniqueID()
            .equals(lastInteractionPlayer) && slot == lastInteractionSlot
            && tick >= lastInteractionTick
            && tick - lastInteractionTick <= REPEAT_WINDOW_TICKS;
        lastInteractionPlayer = player.getUniqueID();
        lastInteractionSlot = slot;
        lastInteractionTick = tick;
        return repeated;
    }

    protected int depositTarget(int slot) {
        return slot;
    }

    protected boolean acceptsDeposit(@Nonnull IBigItemHandler handler, int slot) {
        return slot >= 0 && slot < handler.getStorageCount()
            && handler.getSnapshot(slot)
                .hasTemplate();
    }

    private void insertFromInventory(EntityPlayer player, IBigItemHandler handler, int slot, int inventorySlot) {
        ItemStack stack = player.inventory.getStackInSlot(inventorySlot);
        if (stack == null || stack.stackSize <= 0) {
            return;
        }
        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        long accepted = slot < 0 ? handler.insertRouted(request, StorageAction.EXECUTE)
            .getProcessedAmount()
            : handler.insert(slot, request, StorageAction.EXECUTE)
                .getProcessedAmount();
        if (accepted > 0) {
            stack.stackSize -= (int) Math.min(stack.stackSize, accepted);
            player.inventory.setInventorySlotContents(inventorySlot, stack.stackSize == 0 ? null : stack);
        }
    }

    protected abstract void writeStorageData(@Nonnull NBTTagCompound tag);

    protected abstract void readStorageData(@Nonnull NBTTagCompound tag);

    private boolean tryInstallStorageUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
        for (int slot = 0; slot < getStorageUpgradeSlots(); slot++) {
            if (storageUpgrades[slot] == null && canSetUpgradeSlot(true, slot, held)) {
                install(player, held, storageUpgrades, slot);
                return true;
            }
        }
        return false;
    }

    private boolean tryInstallUtilityUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
        for (int slot = 0; slot < getUtilityUpgradeSlots(); slot++) {
            if (utilityUpgrades[slot] == null && canSetUpgradeSlot(false, slot, held)) {
                install(player, held, utilityUpgrades, slot);
                return true;
            }
        }
        return false;
    }

    private void install(@Nonnull EntityPlayer player, @Nonnull ItemStack held, @Nonnull ItemStack[] target, int slot) {
        ItemStack installed = held.copy();
        installed.stackSize = 1;
        if (installed.getItem() instanceof AutomationUpgradeItem upgrade) {
            upgrade.onInventoryTick(installed, worldObj, player);
        }
        if (!player.capabilities.isCreativeMode) {
            held.stackSize--;
            if (held.stackSize <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
        }
        target[slot] = installed;
        onUpgradesChanged();
    }

    private boolean hasConflictingUpgrade(@Nonnull ItemStack candidate, int ignoredStorageSlot) {
        if (!(candidate.getItem() instanceof IStorageUpgrade candidateUpgrade)) {
            return false;
        }
        for (int slot = 0; slot < storageUpgrades.length; slot++) {
            if (slot != ignoredStorageSlot && isConflict(candidate, candidateUpgrade, storageUpgrades[slot])) {
                return true;
            }
        }
        for (ItemStack existing : utilityUpgrades) {
            if (isConflict(candidate, candidateUpgrade, existing)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConflict(@Nonnull ItemStack candidate, @Nonnull IStorageUpgrade candidateUpgrade,
        @Nullable ItemStack existing) {
        if (existing == null || !(existing.getItem() instanceof IStorageUpgrade existingUpgrade)) {
            return false;
        }
        return candidateUpgrade.conflictsWith(candidate, existing)
            || existingUpgrade.conflictsWith(existing, candidate);
    }

    private void onUpgradesChanged() {
        upgradeCacheDirty = true;
        reconcileStorageConfiguration();
        markDirty();
        requestUpdatePacket();
    }

    /**
     * Reapplies upgrade-derived state such as capacity and lock retention.
     */
    protected void reconcileStorageConfiguration() {}

    /**
     * Drops the cached insertion limit of the physical inventory view.
     *
     * <p>
     * Callers are the transitions that change capacity: installing or removing an
     * upgrade, toggling the lock, replacing the backing handler, and restoring from
     * NBT. Committing a stored amount must not call this, because an external
     * caller reads the limit once per transfer step and recomputing it walks every
     * index of an aggregated network.
     * </p>
     *
     * <p>
     * A linked drawer also drops the limit of the controller aggregating it. The
     * aggregate is the smallest per-item capacity of its drawers, so a drawer whose
     * capacity fell would otherwise leave the controller reporting room that no
     * longer exists, and a caller that trusts that figure loses whatever it believed
     * it had handed over.
     * </p>
     */
    protected final void invalidateStorageLimit() {
        if (inventoryView instanceof DrawerItemInventory inventory) {
            inventory.invalidateLimit();
        }
        if (worldObj != null && controllerX != Integer.MIN_VALUE
            && worldObj.blockExists(controllerX, controllerY, controllerZ)
            && worldObj
                .getTileEntity(controllerX, controllerY, controllerZ) instanceof DrawerControllerTile controller) {
            controller.invalidateNetwork();
        }
    }

    /**
     * Drops the memoized read view of this drawer's own storage.
     *
     * <p>
     * Toggling a lock or swapping an upgrade can change a capacity while every
     * stored amount stays put, and a storage change event never fires for that, so
     * the read memo cannot rely on events alone. Unlike {@link #invalidateStorageLimit}
     * this resolves the backing handler, which is why the NBT load path does not call
     * it: rebuilding a network must never happen while a chunk is still materializing.
     * </p>
     */
    protected final void invalidateStorageViews() {
        IBigItemHandler items = getItemHandler();
        if (items == null) {
            return;
        }
        StorageViewCache cache = items.getStorageViewCache();
        if (cache != null) {
            cache.invalidate();
        }
    }

    private UpgradeState computeUpgradeState() {
        UpgradeState.Builder builder = UpgradeState.builder();
        for (ItemStack stack : storageUpgrades) {
            applyUpgrade(builder, stack);
        }
        for (ItemStack stack : utilityUpgrades) {
            applyUpgrade(builder, stack);
        }
        return builder.build();
    }

    private void applyUpgrade(@Nonnull UpgradeState.Builder builder, @Nullable ItemStack stack) {
        if (stack != null && stack.getItem() instanceof IStorageUpgrade) {
            ((IStorageUpgrade) stack.getItem()).applyUpgrade(stack, builder);
        }
    }

    @Nullable
    private static ItemStack stackAt(@Nonnull ItemStack[] stacks, int slot) {
        return slot < 0 || slot >= stacks.length ? null : stacks[slot];
    }

    @Nonnull
    private static NBTTagList writeStacks(@Nonnull ItemStack[] stacks) {
        NBTTagList list = new NBTTagList();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == null) {
                continue;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte(KEY_SLOT, (byte) slot);
            stacks[slot].writeToNBT(entry);
            list.appendTag(entry);
        }
        return list;
    }

    private static void readStacks(@Nonnull NBTTagList list, @Nonnull ItemStack[] target) {
        Arrays.fill(target, null);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            int slot = entry.getByte(KEY_SLOT) & 0xFF;
            if (slot >= target.length || !entry.hasKey("id")) {
                continue;
            }
            ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
            target[slot] = stack == null || stack.getItem() == null ? null : stack;
        }
    }
}
