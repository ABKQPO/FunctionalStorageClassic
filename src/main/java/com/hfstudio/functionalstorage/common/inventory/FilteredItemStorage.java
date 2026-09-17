package com.hfstudio.functionalstorage.common.inventory;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

public class FilteredItemStorage implements IBigItemHandler {

    private final IBigItemHandler storage;
    private final Predicate<ItemStack> filter;
    private final int[] slots;

    public FilteredItemStorage(IBigItemHandler storage, Predicate<ItemStack> filter, int[] slots) {
        this.storage = storage;
        this.filter = filter;
        this.slots = slots == null ? null : slots.clone();
    }

    private boolean selected(int index) {
        if (index < 0 || index >= storage.getStorageCount()) return false;
        if (slots == null) return true;
        for (int slot : slots) if (slot == index) return true;
        return false;
    }

    @Override
    public int getStorageCount() {
        return storage.getStorageCount();
    }

    @Override
    public BigItemStack getSnapshot(int index) {
        if (!selected(index)) return BigItemStack.empty();
        BigItemStack snapshot = storage.getSnapshot(index);
        return !snapshot.hasTemplate() || filter.test(snapshot.getTemplate()) ? snapshot : BigItemStack.empty();
    }

    @Override
    public long getCapacity(int index) {
        return selected(index) ? storage.getCapacity(index) : 0;
    }

    @Override
    public TransferResult<BigItemStack, ItemStorageKey> insert(int index, BigItemStack request, StorageAction action) {
        return selected(index) && request.hasTemplate() && filter.test(request.getTemplate())
            ? storage.insert(index, request, action)
            : new TransferResult<>(request.getAmount(), BigItemStack.empty(), action);
    }

    @Override
    public TransferResult<BigItemStack, ItemStorageKey> extract(int index, long amount, StorageAction action) {
        return selected(index) && getSnapshot(index).hasTemplate() ? storage.extract(index, amount, action)
            : new TransferResult<>(Math.max(0, amount), BigItemStack.empty(), action);
    }

    @Override
    public boolean isEmptyStorageAvailable(int index) {
        return selected(index) && storage.isEmptyStorageAvailable(index);
    }

    @Override
    public boolean voidsOverflow(int index) {
        return selected(index) && storage.voidsOverflow(index);
    }

    @Override
    public Object getStorageIdentity() {
        return storage.getStorageIdentity();
    }
}
