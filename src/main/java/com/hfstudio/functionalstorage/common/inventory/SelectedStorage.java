package com.hfstudio.functionalstorage.common.inventory;

import java.util.function.Consumer;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/** Restricts indexed access without changing slot identities or routing order. */
public class SelectedStorage<S extends StorageSnapshot<S, K>, K extends StorageKey> implements IStorageHandler<S, K> {

    private final IStorageHandler<S, K> storage;
    private final int[] slots;
    private final S empty;

    public SelectedStorage(IStorageHandler<S, K> storage, int[] slots, S empty) {
        this.storage = storage;
        this.slots = slots.clone();
        this.empty = empty;
    }

    protected boolean selected(int index) {
        if (index < 0 || index >= storage.getStorageCount()) return false;
        for (int slot : slots) if (slot == index) return true;
        return false;
    }

    @Override
    public int getStorageCount() {
        return storage.getStorageCount();
    }

    @Override
    public S getSnapshot(int index) {
        return selected(index) ? storage.getSnapshot(index) : empty;
    }

    @Override
    public long getCapacity(int index) {
        return selected(index) ? storage.getCapacity(index) : 0;
    }

    @Override
    public TransferResult<S, K> insert(int index, S request, StorageAction action) {
        return selected(index) ? storage.insert(index, request, action)
            : new TransferResult<>(request.getAmount(), empty, action);
    }

    @Override
    public TransferResult<S, K> extract(int index, long amount, StorageAction action) {
        return selected(index) ? storage.extract(index, amount, action)
            : new TransferResult<>(Math.max(0, amount), empty, action);
    }

    @Override
    public boolean isLocked() {
        return storage.isLocked();
    }

    @Override
    public boolean isCreative() {
        return storage.isCreative();
    }

    @Override
    public boolean voidsOverflow(int index) {
        return selected(index) && storage.voidsOverflow(index);
    }

    @Override
    public Object getStorageIdentity() {
        return storage.getStorageIdentity();
    }

    @Override
    public StorageSubscription subscribe(Consumer<? super StorageChange<S, K>> listener) {
        return storage.subscribe(listener);
    }

    public static class Fluid extends SelectedStorage<BigFluidStack, FluidStorageKey> implements IBigFluidHandler {

        private final IBigFluidHandler storage;

        public Fluid(IBigFluidHandler storage, int[] slots) {
            super(storage, slots, BigFluidStack.empty());
            this.storage = storage;
        }

        @Override
        public boolean supportsFill(int index) {
            return selected(index) && storage.supportsFill(index);
        }

        @Override
        public boolean supportsDrain(int index) {
            return selected(index) && storage.supportsDrain(index);
        }

        @Override
        public boolean supportsFluid(int index, BigFluidStack fluid) {
            return selected(index) && storage.supportsFluid(index, fluid);
        }
    }

    public static class Aspect extends SelectedStorage<BigAspectStack, AspectStorageKey> implements IBigAspectHandler {

        public Aspect(IBigAspectHandler storage, int[] slots) {
            super(storage, slots, BigAspectStack.empty());
        }
    }
}
