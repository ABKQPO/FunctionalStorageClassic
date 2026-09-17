package com.hfstudio.functionalstorage.common.tile;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerInventoryAccess;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class ArmoryCabinetTile extends ControllableDrawerTile implements DrawerInventoryAccess {

    private static final String KEY_ITEMS = "Items";

    private final BigItemHandler handler;

    public ArmoryCabinetTile() {
        this.handler = new BigItemHandler(slotCount()) {

            @Override
            public boolean isLocked() {
                return false;
            }

            @Override
            protected boolean acceptsResource(BigItemStack resource) {
                ItemStack item = resource.getTemplate();
                return item != null && item.getMaxStackSize() == 1;
            }

            @Override
            protected long capacityLimit() {
                return 1L;
            }
        };
        bindStorageHandler(handler);
    }

    private static int slotCount() {
        return Math.max(1, FunctionalStorageConfig.GENERAL.armoryCabinetSize);
    }

    @Nonnull
    @Override
    public IBigItemHandler getItemHandler() {
        return handler;
    }

    @Override
    protected void writeStorageData(@Nonnull NBTTagCompound tag) {
        tag.setTag(KEY_ITEMS, handler.serializeNBT());
    }

    @Override
    protected void readStorageData(@Nonnull NBTTagCompound tag) {
        handler.deserializeNBT(tag.hasKey(KEY_ITEMS, 10) ? tag.getCompoundTag(KEY_ITEMS) : null);
    }

    @Override
    protected boolean opensGuiOnEmptyHand() {
        return true;
    }

    @Override
    public int getStorageUpgradeSlots() {
        return 0;
    }

    @Override
    public int getUtilityUpgradeSlots() {
        return 0;
    }

    @Override
    protected int calculateRedstoneSignal() {
        int capacity = handler.getStorageCount();
        if (capacity <= 0) {
            return 0;
        }
        int stored = 0;
        for (int index = 0; index < capacity; index++) {
            if (!handler.getSnapshot(index)
                .isEmpty()) {
                stored++;
            }
        }
        return redstoneForRatio(stored / (double) capacity);
    }
}
