package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

/**
 * Generic fluid capability bridge for a long-capacity storage handler. Indexed
 * state changes use {@link IStorageHandler}; the methods below add fluid
 * routing and the Forge fluid surface.
 */
public interface IBigFluidHandler extends IStorageHandler<BigFluidStack, FluidStorageKey> {

    /**
     * Reports whether a generic index currently supports filling.
     *
     * @param index internal storage index
     * @return whether filling is allowed
     */
    default boolean supportsFill(int index) {
        return index >= 0 && index < Math.max(0, getStorageCount());
    }

    /**
     * Reports whether a generic index currently supports draining.
     *
     * @param index internal storage index
     * @return whether draining is allowed
     */
    default boolean supportsDrain(int index) {
        return index >= 0 && index < Math.max(0, getStorageCount());
    }

    /**
     * Reports whether a generic index supports a fluid type.
     *
     * @param index internal storage index
     * @param fluid requested fluid
     * @return whether the fluid may be handled at the index
     */
    default boolean supportsFluid(int index, @Nonnull BigFluidStack fluid) {
        return index >= 0 && index < Math.max(0, getStorageCount()) && fluid.hasTemplate();
    }

    /**
     * Bridges Forge fill to routed generic insertion.
     *
     * @param resource fluid to insert
     * @param doFill   whether to execute or simulate
     * @return the accepted amount
     */
    default int fill(@Nullable FluidStack resource, boolean doFill) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }
        BigFluidStack request = new BigFluidStack(resource, resource.amount);
        TransferResult<BigFluidStack, FluidStorageKey> result = fillRouted(
            request,
            StorageAction.fromSimulation(!doFill));
        long processed = Math.min(request.getAmount(), Math.max(0L, result.getProcessedAmount()));
        return processed >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) processed;
    }

    /**
     * Bridges Forge typed drain to routed generic extraction.
     *
     * @param resource fluid to drain
     * @param doDrain  whether to execute or simulate
     * @return the drained stack, or {@code null}
     */
    @Nullable
    default FluidStack drain(@Nullable FluidStack resource, boolean doDrain) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return null;
        }
        BigFluidStack request = new BigFluidStack(resource, resource.amount);
        TransferResult<BigFluidStack, FluidStorageKey> result = drainRouted(
            request,
            StorageAction.fromSimulation(!doDrain));
        long processed = Math.min(request.getAmount(), Math.max(0L, result.getProcessedAmount()));
        return processed == 0L ? null
            : result.getProcessed()
                .withAmount(processed)
                .toFluidStack();
    }

    /**
     * Bridges Forge untyped drain to the first available fluid type.
     *
     * @param maxDrain requested amount
     * @param doDrain  whether to execute or simulate
     * @return the drained stack, or {@code null}
     */
    @Nullable
    default FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        TransferResult<BigFluidStack, FluidStorageKey> result = drainRouted(
            maxDrain,
            StorageAction.fromSimulation(!doDrain));
        long processed = Math.min(maxDrain, Math.max(0L, result.getProcessedAmount()));
        return processed == 0L ? null
            : result.getProcessed()
                .withAmount(processed)
                .toFluidStack();
    }

    /**
     * Routes filling through compatible configured indices before empty indices.
     *
     * @param request requested fluid and amount
     * @param action  execute or simulate
     * @return the routed result
     */
    @Nonnull
    default TransferResult<BigFluidStack, FluidStorageKey> fillRouted(@Nonnull BigFluidStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigFluidStack.empty(), action);
        }
        long processedTotal = 0L;
        int count = Math.max(0, getStorageCount());
        for (int pass = 0; pass < 2 && processedTotal < requested; pass++) {
            for (int index = 0; index < count && processedTotal < requested; index++) {
                if (!supportsFill(index) || !supportsFluid(index, request)) {
                    continue;
                }
                BigFluidStack current = getSnapshot(index);
                boolean hasTemplate = current.hasTemplate();
                if ((pass == 0 && (!hasTemplate || !current.isSameType(request))) || (pass == 1 && hasTemplate)) {
                    continue;
                }
                long remaining = requested - processedTotal;
                TransferResult<BigFluidStack, FluidStorageKey> result = insert(
                    index,
                    request.withAmount(remaining),
                    action);
                long processed = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
                processedTotal = saturatedAdd(processedTotal, processed);
            }
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    /**
     * Routes typed extraction through matching generic indices.
     *
     * @param request requested fluid and amount
     * @param action  execute or simulate
     * @return the routed result
     */
    @Nonnull
    default TransferResult<BigFluidStack, FluidStorageKey> drainRouted(@Nonnull BigFluidStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigFluidStack.empty(), action);
        }
        long processedTotal = 0L;
        int count = Math.max(0, getStorageCount());
        for (int index = 0; index < count && processedTotal < requested; index++) {
            if (!supportsDrain(index) || !supportsFluid(index, request)) {
                continue;
            }
            BigFluidStack current = getSnapshot(index);
            if (current.isEmpty() || !current.isSameType(request)) {
                continue;
            }
            long remaining = requested - processedTotal;
            TransferResult<BigFluidStack, FluidStorageKey> result = extract(index, remaining, action);
            long processed = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
            processedTotal = saturatedAdd(processedTotal, processed);
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    /**
     * Routes untyped extraction by selecting the first available fluid type.
     *
     * @param amount requested amount
     * @param action execute or simulate
     * @return the routed result
     */
    @Nonnull
    default TransferResult<BigFluidStack, FluidStorageKey> drainRouted(long amount, @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = Math.max(0L, amount);
        if (requested == 0L) {
            return new TransferResult<>(0L, BigFluidStack.empty(), action);
        }
        int count = Math.max(0, getStorageCount());
        for (int index = 0; index < count; index++) {
            BigFluidStack current = getSnapshot(index);
            if (!current.isEmpty() && supportsDrain(index) && supportsFluid(index, current)) {
                return drainRouted(current.withAmount(requested), action);
            }
        }
        return new TransferResult<>(requested, BigFluidStack.empty(), action);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
