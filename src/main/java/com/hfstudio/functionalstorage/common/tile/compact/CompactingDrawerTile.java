package com.hfstudio.functionalstorage.common.tile.compact;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.common.inventory.CompactingItemHandler;
import com.hfstudio.functionalstorage.common.storage.CompactingTier;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.util.CompactingUtil;

/**
 * Compacting drawer tile. Stores one shared amount in lowest-tier units and
 * exposes one visible slot per configured compression tier.
 */
public class CompactingDrawerTile extends ControllableDrawerTile {

    private static final String KEY_COMPACTING = "Compacting";

    private final int slotCount;
    private CompactingItemHandler handler;
    private boolean recipesChecked;

    public CompactingDrawerTile() {
        this(3);
    }

    public CompactingDrawerTile(int slotCount) {
        this.slotCount = Math.max(1, slotCount);
        this.handler = createHandler();
        bindStorageHandler(handler);
    }

    /**
     * @return the number of visible compression tiers
     */
    public int getSlotCount() {
        return slotCount;
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote || recipesChecked) {
            return;
        }
        recipesChecked = true;
        if (!handler.isConfigured()) {
            return;
        }
        ItemStack anchor = firstConfiguredTemplate();
        if (anchor == null) {
            return;
        }
        List<CompactingTier> results = CompactingUtil
            .anchoredResults(worldObj, anchor, handler.getStorageCount(), firstConfiguredSlot());
        if (!results.isEmpty()) {
            handler.configureTiers(results);
            markDirty();
            requestUpdatePacket();
        }
    }

    @Override
    public boolean onSlotActivated(@Nonnull EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        int slot) {
        if (super.onSlotActivated(player, side, hitX, hitY, hitZ, slot)) {
            return true;
        }
        if (worldObj == null || worldObj.isRemote || slot < 0) {
            return false;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() == null) {
            return false;
        }

        if (!handler.isConfigured()) {
            List<CompactingTier> results = CompactingUtil
                .anchoredResults(worldObj, held, handler.getStorageCount(), slot);
            if (!results.isEmpty()) {
                handler.configureTiers(results);
                markDirty();
                requestUpdatePacket();
            }
        }

        if (!handler.isConfigured()) {
            return false;
        }
        ItemStack remainder = insertStack(slot, held, true);
        if (remainder == null || remainder.stackSize != held.stackSize) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, insertStack(slot, held, false));
            return true;
        }
        return false;
    }

    @Override
    public void onSlotClicked(@Nonnull EntityPlayer player, int slot) {
        if (worldObj == null || worldObj.isRemote || slot < 0) {
            return;
        }
        BigItemStack snapshot = handler.getSnapshot(slot);
        ItemStack template = snapshot.getTemplate();
        if (template == null) {
            return;
        }
        int amount = player.isSneaking() ? Math.max(1, template.getMaxStackSize()) : 1;
        ItemStack extracted = extractStack(slot, amount, false);
        if (extracted != null && extracted.getItem() != null) {
            if (!player.inventory.addItemStackToInventory(extracted)) {
                player.dropPlayerItemWithRandomChoice(extracted, false);
            }
        }
    }

    /**
     * Inserts a stack into the shared amount through one visible tier.
     *
     * @param slot     visible tier index
     * @param stack    stack to insert
     * @param simulate whether to only report the outcome
     * @return the leftover stack, or {@code null} when everything fit
     */
    @Nullable
    public ItemStack insertStack(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.getItem() == null || stack.stackSize <= 0) {
            return stack.copy();
        }
        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .insert(slot, new BigItemStack(stack, stack.stackSize), StorageAction.fromSimulation(simulate));
        if (result.isComplete()) {
            return null;
        }
        ItemStack remainder = stack.copy();
        remainder.stackSize = (int) Math.min(result.getRemainingAmount(), stack.stackSize);
        return remainder;
    }

    /**
     * Extracts from the shared amount through one visible tier.
     *
     * @param slot     visible tier index
     * @param amount   requested item count
     * @param simulate whether to only report the outcome
     * @return the extracted stack, or {@code null}
     */
    @Nullable
    public ItemStack extractStack(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return null;
        }
        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .extract(slot, amount, StorageAction.fromSimulation(simulate));
        return result.getProcessed()
            .toItemStack();
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_COMPACTING, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_COMPACTING, 10) ? tag.getCompoundTag(KEY_COMPACTING) : null);
        recipesChecked = false;
    }

    @Override
    protected void reconcileStorageConfiguration() {
        handler.applyLockConfiguration(isLocked());
    }

    @Override
    protected int calculateRedstoneSignal() {
        long capacity = handler.getTotalBaseCapacity();
        if (capacity <= 0L) {
            return 0;
        }
        return redstoneForRatio(handler.getStoredBaseAmount() / (double) capacity);
    }

    @Override
    public boolean isEverythingEmpty() {
        return super.isEverythingEmpty() && handler.getStoredBaseAmount() == 0L && !handler.isConfigured();
    }

    private CompactingItemHandler createHandler() {
        return new CompactingItemHandler(slotCount) {

            @Override
            public double getMultiplier() {
                return calculateModifier(UpgradeAttribute.ITEM_CAPACITY, 1D);
            }

            @Override
            protected boolean allowsEquivalentItems() {
                return CompactingDrawerTile.this.hasEquivalentItems();
            }

            @Override
            public boolean isLocked() {
                return CompactingDrawerTile.this.isLocked();
            }

            @Override
            public boolean voidsOverflow() {
                return CompactingDrawerTile.this.voidsOverflow();
            }

            @Override
            public boolean isCreative() {
                return CompactingDrawerTile.this.isCreative();
            }

            @Override
            public boolean hasMaxStorage() {
                return CompactingDrawerTile.this.hasMaxStorage();
            }
        };
    }

    @Nullable
    private ItemStack firstConfiguredTemplate() {
        for (CompactingTier tier : handler.getTiers()) {
            if (tier.hasTemplate()) {
                return tier.getTemplate();
            }
        }
        return null;
    }

    private int firstConfiguredSlot() {
        List<CompactingTier> tiers = handler.getTiers();
        for (int index = 0; index < tiers.size(); index++) {
            if (tiers.get(index)
                .hasTemplate()) {
                return index;
            }
        }
        return 0;
    }
}
