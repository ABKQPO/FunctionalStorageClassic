package com.hfstudio.functionalstorage.common.inventory.adapter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

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
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

public class AdaptiveDepositHandlers {

    public abstract static class Port<S extends StorageSnapshot<S, K>, K extends StorageKey, H extends IStorageHandler<S, K>>
        implements IStorageHandler<S, K> {

        protected final Supplier<H> target;
        private final S empty;

        protected Port(Supplier<H> target, S empty) {
            this.target = target;
            this.empty = empty;
        }

        @Override
        public int getStorageCount() {
            return 1;
        }

        @Override
        @Nonnull
        public S getSnapshot(int index) {
            return empty;
        }

        @Override
        public long getCapacity(int index) {
            return index == 0 ? Long.MAX_VALUE : 0L;
        }

        @Override
        @Nonnull
        public TransferResult<S, K> insert(int index, @Nonnull S request, @Nonnull StorageAction action) {
            H handler = index == 0 && !request.isEmpty() ? target.get() : null;
            return handler == null ? new TransferResult<>(request.getAmount(), empty, action)
                : route(handler, request, action);
        }

        protected abstract TransferResult<S, K> route(H handler, S request, StorageAction action);

        @Override
        @Nonnull
        public TransferResult<S, K> extract(int index, long amount, @Nonnull StorageAction action) {
            return new TransferResult<>(Math.max(0L, amount), empty, action);
        }

        @Override
        public @NotNull StorageSubscription subscribe(@NotNull Consumer<? super StorageChange<S, K>> listener) {
            return StorageSubscription.CLOSED;
        }
    }

    public static class Items extends Port<BigItemStack, ItemStorageKey, IBigItemHandler> implements IBigItemHandler {

        public Items(Supplier<IBigItemHandler> target) {
            super(target, BigItemStack.empty());
        }

        @Override
        protected TransferResult<BigItemStack, ItemStorageKey> route(IBigItemHandler handler, BigItemStack request,
            StorageAction action) {
            return handler.insertRouted(request, action);
        }
    }

    public static class Fluids extends Port<BigFluidStack, FluidStorageKey, IBigFluidHandler>
        implements IBigFluidHandler {

        public Fluids(Supplier<IBigFluidHandler> target) {
            super(target, BigFluidStack.empty());
        }

        @Override
        public boolean supportsDrain(int index) {
            return false;
        }

        @Override
        protected TransferResult<BigFluidStack, FluidStorageKey> route(IBigFluidHandler handler, BigFluidStack request,
            StorageAction action) {
            return handler.fillRouted(request, action);
        }
    }

    public static class Aspects extends Port<BigAspectStack, AspectStorageKey, IBigAspectHandler>
        implements IBigAspectHandler {

        public Aspects(Supplier<IBigAspectHandler> target) {
            super(target, BigAspectStack.empty());
        }

        @Override
        protected TransferResult<BigAspectStack, AspectStorageKey> route(IBigAspectHandler handler,
            BigAspectStack request, StorageAction action) {
            return handler.insertRouted(request, action);
        }
    }
}
