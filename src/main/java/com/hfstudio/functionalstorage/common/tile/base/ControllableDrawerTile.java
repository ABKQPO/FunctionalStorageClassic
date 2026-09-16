package com.hfstudio.functionalstorage.common.tile.base;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.client.render.DrawerOptions;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.AbstractStorageHandler;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeItem;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

/**
 * Base tile entity for every drawer. Owns the upgrade slots, the rendering
 * options, the lock flag, and the single storage-event subscription so
 * subclasses only have to provide their storage handler.
 */
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

    private final ItemStack[] storageUpgrades = new ItemStack[STORAGE_UPGRADE_SLOTS];
    private final ItemStack[] utilityUpgrades = new ItemStack[UTILITY_UPGRADE_SLOTS];
    private final DrawerOptions drawerOptions = new DrawerOptions();

    private boolean locked;
    private boolean upgradeCacheDirty = true;
    private UpgradeState cachedUpgradeState = UpgradeState.empty();

    private int controllerX = Integer.MIN_VALUE;
    private int controllerY = Integer.MIN_VALUE;
    private int controllerZ = Integer.MIN_VALUE;

    private StorageSubscription storageSubscription = StorageSubscription.CLOSED;
    private IStorageHandler<?, ?> subscribedStorage;
    private boolean pendingUpdatePacket;
    private IInventory inventoryView;

    /**
     * @return the item storage handler, or {@code null} when this drawer stores something else
     */
    @Nullable
    public IBigItemHandler getItemHandler() {
        return null;
    }

    /**
     * @return the fluid storage handler, or {@code null} when this drawer stores something else
     */
    @Nullable
    public IBigFluidHandler getFluidHandler() {
        return null;
    }

    /**
     * @return the essentia storage handler, or {@code null} when this drawer stores something else
     */
    @Nullable
    public IBigAspectHandler getAspectHandler() {
        return null;
    }

    /**
     * @return the active storage handler regardless of resource kind
     */
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

    /**
     * @return the number of storage upgrade slots
     */
    public int getStorageUpgradeSlots() {
        return STORAGE_UPGRADE_SLOTS;
    }

    /**
     * @return the number of utility upgrade slots
     */
    public int getUtilityUpgradeSlots() {
        return UTILITY_UPGRADE_SLOTS;
    }

    /**
     * @return whether this drawer has a GUI with upgrade slots
     */
    public boolean hasUpgradeSlots() {
        return getStorageUpgradeSlots() > 0 || getUtilityUpgradeSlots() > 0;
    }

    /**
     * Returns the vanilla inventory view of this drawer's item storage, created
     * on first use. Drawers that do not store items return {@code null}, which
     * is how the GUI decides whether to show storage slots.
     *
     * @return the inventory view, or {@code null}
     */
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

    /**
     * @return the rendering options of this drawer
     */
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

    /**
     * @param slot upgrade slot index
     * @return the storage upgrade stack, or {@code null} when the slot is empty
     */
    @Nullable
    public ItemStack getStorageUpgrade(int slot) {
        return stackAt(storageUpgrades, slot);
    }

    /**
     * @param slot upgrade slot index
     * @return the utility upgrade stack, or {@code null} when the slot is empty
     */
    @Nullable
    public ItemStack getUtilityUpgrade(int slot) {
        return stackAt(utilityUpgrades, slot);
    }

    /**
     * Installs or clears an upgrade slot from a GUI interaction.
     *
     * @param storage whether the slot is a storage upgrade slot
     * @param slot    upgrade slot index
     * @param stack   new stack, or {@code null} to clear
     */
    public void setUpgradeSlot(boolean storage, int slot, @Nullable ItemStack stack) {
        ItemStack[] target = storage ? storageUpgrades : utilityUpgrades;
        if (slot < 0 || slot >= target.length) {
            return;
        }
        target[slot] = stack == null || stack.getItem() == null ? null : stack;
        upgradeCacheDirty = true;
        reconcileStorageConfiguration();
        markDirty();
        requestUpdatePacket();
    }

    /**
     * Applies a change made through the GUI directly to the tile.
     *
     * @param storage whether the slot is a storage upgrade slot
     * @param slot    upgrade slot index
     */
    public void onUpgradeSlotChanged(boolean storage, int slot) {
        upgradeCacheDirty = true;
        reconcileStorageConfiguration();
        markDirty();
        requestUpdatePacket();
    }

    /**
     * @return whether the drawer retains filters for empty slots
     */
    public boolean isLocked() {
        return locked;
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
        markDirty();
        requestUpdatePacket();
    }

    /**
     * Flips the lock state.
     */
    public void toggleLocking() {
        setLocked(!locked);
    }

    /**
     * @return the controller coordinates as a three element array, or {@code null} when unlinked
     */
    @Nullable
    public int[] getControllerPosition() {
        return controllerX == Integer.MIN_VALUE ? null : new int[] { controllerX, controllerY, controllerZ };
    }

    /**
     * Binds this drawer to a controller.
     *
     * @param x controller x
     * @param y controller y
     * @param z controller z
     */
    public void setControllerPosition(int x, int y, int z) {
        this.controllerX = x;
        this.controllerY = y;
        this.controllerZ = z;
        markDirty();
    }

    /**
     * Removes the controller link.
     */
    public void clearControllerPosition() {
        this.controllerX = Integer.MIN_VALUE;
        this.controllerY = Integer.MIN_VALUE;
        this.controllerZ = Integer.MIN_VALUE;
        markDirty();
    }

    /**
     * Releases the controller link and asks the controller to forget this drawer.
     *
     * @param world world containing this drawer
     */
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

    /**
     * @param side side the signal is queried from
     * @return the current redstone output
     */
    public int getRedstoneSignal(int side) {
        return hasRedstoneUpgrade() ? calculateRedstoneSignal() : 0;
    }

    /**
     * @return whether a redstone upgrade is installed
     */
    public boolean hasRedstoneUpgrade() {
        return getUpgradeState().hasFeature(StorageFeature.REDSTONE_OUTPUT);
    }

    /**
     * @return the comparator-style signal derived from the fill ratio
     */
    protected int calculateRedstoneSignal() {
        return 0;
    }

    /**
     * Comparator output based on how full the drawer is. This always reports the
     * fill level, unlike {@link #getRedstoneSignal(int)} which depends on an
     * installed redstone upgrade.
     *
     * @return a signal strength between zero and fifteen
     */
    public int getComparatorOutput() {
        return calculateRedstoneSignal();
    }

    /**
     * Converts a fill ratio into a redstone signal strength.
     *
     * @param ratio fill ratio in the range zero to one
     * @return the signal strength
     */
    protected static int redstoneForRatio(double ratio) {
        if (ratio <= 0D) {
            return 0;
        }
        if (ratio >= 1D) {
            return 15;
        }
        return (int) Math.ceil(ratio * 14D);
    }

    /**
     * Handles a right-click on one drawer slot.
     *
     * @param player interacting player
     * @param side   clicked face ordinal
     * @param hitX   local hit x
     * @param hitY   local hit y
     * @param hitZ   local hit z
     * @param slot   resolved slot index, or {@code -1}
     * @return whether the interaction was consumed
     */
    public boolean onSlotActivated(@Nonnull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        ItemStack held = player.getHeldItem();
        if (held == null) {
            // Sneaking with an empty hand opens the interface, which is the only
            // place upgrades can be rearranged without breaking the block.
            return player.isSneaking() && hasUpgradeSlots() && openGui(player);
        }
        if (held.getItem() == RegistrationHandler.configurationTool) {
            return false;
        }
        if (held.getItem() == RegistrationHandler.linkingTool) {
            return false;
        }
        if (held.getItem() instanceof IStorageUpgrade && tryInstallStorageUpgrade(player, held)) {
            return true;
        }
        if (held.getItem() instanceof UpgradeItem && tryInstallUtilityUpgrade(player, held)) {
            return true;
        }
        return false;
    }

    /**
     * Opens this drawer's configuration screen for a player.
     *
     * @param player player to open the screen for
     * @return whether the screen was opened
     */
    public boolean openGui(@Nonnull EntityPlayer player) {
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        player.openGui(FunctionalStorage.instance, GuiHandler.GUI_DRAWER, worldObj, xCoord, yCoord, zCoord);
        return true;
    }

    /**
     * Handles a left-click on one drawer slot. Extracts a single item from that
     * slot, or a full stack while the player is sneaking.
     *
     * @param player interacting player
     * @param slot   resolved slot index, or {@code -1}
     */
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
        ItemStack extracted = itemHandler.extractRouted(new BigItemStack(template, amount), StorageAction.EXECUTE)
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

    /**
     * @return the current upgrade contributions, recomputed when stale
     */
    @Nonnull
    public UpgradeState getUpgradeState() {
        if (upgradeCacheDirty) {
            cachedUpgradeState = computeUpgradeState();
            upgradeCacheDirty = false;
        }
        return cachedUpgradeState;
    }

    /**
     * @param attribute   attribute to evaluate
     * @param defaultBase base value supplied by the storage implementation
     * @return the evaluated value
     */
    public double calculateModifier(@Nonnull UpgradeAttribute attribute, double defaultBase) {
        return getUpgradeState().calculate(attribute, defaultBase);
    }

    /**
     * @return whether the installed upgrades remove the capacity ceiling
     */
    public boolean hasMaxStorage() {
        return getUpgradeState().hasFeature(StorageFeature.MAX_CAPACITY);
    }

    /**
     * @return whether the ore dictionary upgrade is installed
     */
    public boolean hasEquivalentItems() {
        return getUpgradeState().hasFeature(StorageFeature.EQUIVALENT_ITEMS);
    }

    /**
     * @return whether the creative vending upgrade is installed
     */
    public boolean isCreative() {
        return getUpgradeState().hasFeature(StorageFeature.CREATIVE);
    }

    /**
     * @return whether compatible overflow is voided
     */
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

    /**
     * Writes this drawer's persistent state onto a drop stack.
     *
     * @param base base drop stack
     * @return the drop stack carrying contents, upgrades, and options
     */
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

    /**
     * Restores this drawer from a drop stack.
     *
     * @param stack drop stack
     */
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
    }

    /**
     * Clears every upgrade slot when the block is broken.
     */
    public void onBlockBroken() {
        for (int slot = 0; slot < storageUpgrades.length; slot++) {
            storageUpgrades[slot] = null;
        }
        for (int slot = 0; slot < utilityUpgrades.length; slot++) {
            utilityUpgrades[slot] = null;
        }
    }

    /**
     * @param tag destination tag
     * @return the supplied tag with the full persistent state of this drawer
     */
    @Nonnull
    public NBTTagCompound writeTileData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_STORAGE_UPGRADES, writeStacks(storageUpgrades));
        tag.setTag(KEY_UTILITY_UPGRADES, writeStacks(utilityUpgrades));
        tag.setTag(KEY_OPTIONS, drawerOptions.serializeNBT());
        tag.setBoolean(KEY_LOCKED, locked);
        if (controllerX != Integer.MIN_VALUE) {
            tag.setIntArray(KEY_CONTROLLER, new int[] { controllerX, controllerY, controllerZ });
        }
        writeStorageData(tag);
        return tag;
    }

    /**
     * Restores persistent state.
     *
     * @param tag previously produced by {@link #writeTileData}
     */
    public void readTileData(@Nonnull NBTTagCompound tag) {
        readStacks(tag.getTagList(KEY_STORAGE_UPGRADES, 10), storageUpgrades);
        readStacks(tag.getTagList(KEY_UTILITY_UPGRADES, 10), utilityUpgrades);
        if (tag.hasKey(KEY_OPTIONS, 10)) {
            drawerOptions.deserializeNBT(tag.getCompoundTag(KEY_OPTIONS));
        }
        locked = tag.getBoolean(KEY_LOCKED);
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
        flushPendingUpdatePacket();
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        for (int slot = 0; slot < utilityUpgrades.length; slot++) {
            ItemStack stack = utilityUpgrades[slot];
            if (stack != null && stack.getItem() instanceof AutomationUpgradeItem) {
                tickAutomation((AutomationUpgradeItem) stack.getItem(), stack, slot);
            }
        }
    }

    /**
     * Runs one automation upgrade on its own interval. Keeping the countdown in
     * the upgrade's NBT means the interval survives reloads and stays per
     * upgrade rather than per drawer.
     *
     * @param upgrade installed automation upgrade
     * @param stack   installed upgrade stack
     * @param slot    utility slot index
     */
    private void tickAutomation(@Nonnull AutomationUpgradeItem upgrade, @Nonnull ItemStack stack, int slot) {
        int remaining = upgrade.getRemainingTicks(stack) - 1;
        if (remaining > 0) {
            upgrade.setRemainingTicks(stack, remaining);
            return;
        }
        upgrade.setRemainingTicks(stack, upgrade.getTickInterval());
        upgrade.work(this, stack, slot);
    }

    /**
     * Binds the tile-owned subscription to a storage handler.
     *
     * @param handler storage handler, or {@code null} to detach
     */
    protected final void bindStorageHandler(@Nullable IStorageHandler<?, ?> handler) {
        if (subscribedStorage == handler && !storageSubscription.isClosed()) {
            return;
        }
        closeStorageSubscription();
        subscribedStorage = handler;
        if (handler != null) {
            storageSubscription = handler.subscribe(change -> onStorageChanged());
        }
    }

    /**
     * Closes the tile-owned subscription; safe to call repeatedly.
     */
    protected final void closeStorageSubscription() {
        StorageSubscription current = storageSubscription;
        storageSubscription = StorageSubscription.CLOSED;
        if (current != null) {
            current.close();
        }
    }

    /**
     * Rebuilds a closed subscription after a chunk reload.
     */
    protected final void rebuildStorageSubscription() {
        if (subscribedStorage != null && storageSubscription.isClosed()) {
            storageSubscription = subscribedStorage.subscribe(change -> onStorageChanged());
        }
    }

    /**
     * Marks the tile dirty and schedules one update packet.
     */
    protected final void onStorageChanged() {
        markDirty();
        requestUpdatePacket();
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
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeTileData(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readTileData(packet.func_148857_g());
    }

    /**
     * Writes resource-specific storage state.
     *
     * @param tag destination tag
     */
    protected abstract void writeStorageData(@Nonnull NBTTagCompound tag);

    /**
     * Reads resource-specific storage state.
     *
     * @param tag source tag
     */
    protected abstract void readStorageData(@Nonnull NBTTagCompound tag);

    private boolean tryInstallStorageUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
        for (int slot = 0; slot < storageUpgrades.length; slot++) {
            if (storageUpgrades[slot] == null && !hasConflictingUpgrade(held, slot)) {
                install(player, held, storageUpgrades, slot);
                return true;
            }
        }
        return false;
    }

    private boolean tryInstallUtilityUpgrade(@Nonnull EntityPlayer player, @Nonnull ItemStack held) {
        for (int slot = 0; slot < utilityUpgrades.length; slot++) {
            if (utilityUpgrades[slot] == null && !hasConflictingUpgrade(held, -1)) {
                install(player, held, utilityUpgrades, slot);
                return true;
            }
        }
        return false;
    }

    private void install(@Nonnull EntityPlayer player, @Nonnull ItemStack held, @Nonnull ItemStack[] target, int slot) {
        ItemStack installed = held.copy();
        installed.stackSize = 1;
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
        if (!(candidate.getItem() instanceof IStorageUpgrade)) {
            return false;
        }
        IStorageUpgrade candidateUpgrade = (IStorageUpgrade) candidate.getItem();
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
        if (existing == null || !(existing.getItem() instanceof IStorageUpgrade)) {
            return false;
        }
        IStorageUpgrade existingUpgrade = (IStorageUpgrade) existing.getItem();
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
        for (int index = 0; index < target.length; index++) {
            target[index] = null;
        }
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
