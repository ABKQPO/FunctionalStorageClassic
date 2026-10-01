package com.hfstudio.functionalstorage.common.integration.logisticspipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler;
import logisticspipes.utils.item.ItemIdentifier;

public class LogisticsPipesInventoryHandler extends SpecialInventoryHandler {

    private final IBigItemHandler storage;
    private final boolean hideOnePerStack;
    private final boolean hideOne;
    private final int cropStart;
    private final int cropEnd;

    public LogisticsPipesInventoryHandler() {
        storage = null;
        hideOnePerStack = false;
        hideOne = false;
        cropStart = 0;
        cropEnd = 0;
    }

    private LogisticsPipesInventoryHandler(ControllableDrawerTile drawer, boolean hideOnePerStack, boolean hideOne,
        int cropStart, int cropEnd) {
        storage = drawer.getItemHandler();
        this.hideOnePerStack = hideOnePerStack;
        this.hideOne = hideOne;
        this.cropStart = Math.max(0, cropStart);
        this.cropEnd = Math.max(0, cropEnd);
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean isType(TileEntity tile) {
        return tile instanceof ControllableDrawerTile drawer && drawer.getItemHandler() != null;
    }

    @Override
    public SpecialInventoryHandler getUtilForTile(TileEntity tile, ForgeDirection dir, boolean hideOnePerStack,
        boolean hideOne, int cropStart, int cropEnd) {
        return new LogisticsPipesInventoryHandler(
            (ControllableDrawerTile) tile,
            hideOnePerStack,
            hideOne,
            cropStart,
            cropEnd);
    }

    @Override
    public int itemCount(ItemIdentifier item) {
        long count = 0L;
        boolean first = true;
        for (int index = firstIndex(); index < lastIndex(); index++) {
            BigItemStack snapshot = storage.getSnapshot(index);
            if (!matches(snapshot, item) || snapshot.getAmount() <= 0L) {
                continue;
            }
            long hidden = hideOnePerStack || (hideOne && first) ? 1L : 0L;
            count = saturatedAdd(count, Math.max(0L, snapshot.getAmount() - hidden));
            first = false;
        }
        return toInt(count);
    }

    @Override
    public ItemStack getMultipleItems(ItemIdentifier item, int count) {
        if (count <= 0) {
            return null;
        }
        int remaining = count;
        ItemStack result = null;
        boolean first = true;
        for (int index = firstIndex(); index < lastIndex() && remaining > 0; index++) {
            BigItemStack snapshot = storage.getSnapshot(index);
            if (!matches(snapshot, item) || snapshot.getAmount() <= 0L) {
                continue;
            }
            long hidden = hideOnePerStack || (hideOne && first) ? 1L : 0L;
            long available = Math.max(0L, snapshot.getAmount() - hidden);
            first = false;
            if (available == 0L) {
                continue;
            }
            long requested = Math.min(available, remaining);
            long extracted = storage.extract(index, requested, StorageAction.EXECUTE)
                .getProcessedAmount();
            if (extracted <= 0L) {
                continue;
            }
            ItemStack extractedStack = snapshot.withAmount(extracted)
                .toItemStack();
            if (extractedStack == null) {
                continue;
            }
            if (result == null) {
                result = extractedStack;
            } else {
                result.stackSize += extractedStack.stackSize;
            }
            remaining -= extractedStack.stackSize;
        }
        return result;
    }

    @Override
    public Set<ItemIdentifier> getItems() {
        Set<ItemIdentifier> result = new TreeSet<>();
        for (int index = firstIndex(); index < lastIndex(); index++) {
            BigItemStack snapshot = storage.getSnapshot(index);
            if (snapshot.getAmount() > 0L && snapshot.getTemplate() != null) {
                result.add(ItemIdentifier.get(snapshot.getTemplate()));
            }
        }
        return result;
    }

    @Override
    public HashMap<ItemIdentifier, Integer> getItemsAndCount() {
        HashMap<ItemIdentifier, Integer> result = new HashMap<>();
        Map<ItemIdentifier, Boolean> seen = new HashMap<>();
        for (int index = firstIndex(); index < lastIndex(); index++) {
            BigItemStack snapshot = storage.getSnapshot(index);
            if (snapshot.getAmount() <= 0L || snapshot.getTemplate() == null) {
                continue;
            }
            ItemIdentifier item = ItemIdentifier.get(snapshot.getTemplate());
            long hidden = hideOnePerStack || (hideOne && !seen.containsKey(item)) ? 1L : 0L;
            long visible = Math.max(0L, snapshot.getAmount() - hidden);
            result.put(item, saturatedAdd(result.getOrDefault(item, 0), visible));
            seen.put(item, true);
        }
        return result;
    }

    @Override
    public ItemStack getSingleItem(ItemIdentifier item) {
        return getMultipleItems(item, 1);
    }

    @Override
    public boolean containsUndamagedItem(ItemIdentifier item) {
        for (int index = firstIndex(); index < lastIndex(); index++) {
            BigItemStack snapshot = storage.getSnapshot(index);
            ItemStack template = snapshot.getTemplate();
            if (template != null && ItemIdentifier.get(template)
                .getUndamaged()
                .equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int roomForItem(ItemIdentifier item) {
        return roomForItem(item, 0);
    }

    @Override
    public int roomForItem(ItemIdentifier item, int count) {
        long requested = count > 0 ? count : Long.MAX_VALUE;
        long room = storage.insertRouted(new BigItemStack(item.makeNormalStack(1), requested), StorageAction.SIMULATE)
            .getProcessedAmount();
        return toInt(room);
    }

    @Override
    public ItemStack add(ItemStack stack, ForgeDirection from, boolean doAdd) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return stack == null ? null : stack.copy();
        }
        long accepted = storage
            .insertRouted(
                new BigItemStack(stack, stack.stackSize),
                doAdd ? StorageAction.EXECUTE : StorageAction.SIMULATE)
            .getProcessedAmount();
        ItemStack result = stack.copy();
        result.stackSize = toInt(Math.min(stack.stackSize, Math.max(0L, accepted)));
        return result;
    }

    @Override
    public boolean isSpecialInventory() {
        return true;
    }

    @Override
    public int getSizeInventory() {
        return lastIndex() - firstIndex();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= getSizeInventory()) {
            return null;
        }
        return storage.getSnapshot(firstIndex() + index)
            .toItemStack();
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index < 0 || index >= getSizeInventory() || count <= 0) {
            return null;
        }
        return storage.extract(firstIndex() + index, count, StorageAction.EXECUTE)
            .getProcessed()
            .toItemStack();
    }

    private int firstIndex() {
        return Math.min(storage.getStorageCount(), cropStart);
    }

    private int lastIndex() {
        return Math.max(firstIndex(), storage.getStorageCount() - cropEnd);
    }

    private static boolean matches(BigItemStack snapshot, ItemIdentifier item) {
        ItemStack template = snapshot.getTemplate();
        return template != null && ItemIdentifier.get(template)
            .equals(item);
    }

    private static int toInt(long amount) {
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, amount);
    }

    private static long saturatedAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static int saturatedAdd(int left, long right) {
        if (right <= 0L) {
            return left;
        }
        return right >= Integer.MAX_VALUE - (long) left ? Integer.MAX_VALUE : left + (int) right;
    }
}
