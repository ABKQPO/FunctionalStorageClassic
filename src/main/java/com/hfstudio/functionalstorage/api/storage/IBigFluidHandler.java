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

    default boolean supportsFill(int index) {
        return index >= 0 && index < Math.max(0, getStorageCount());
    }

    default boolean supportsDrain(int index) {
        return index >= 0 && index < Math.max(0, getStorageCount());
    }

    default boolean supportsFluid(int index, @Nonnull BigFluidStack fluid) {
        return index >= 0 && index < Math.max(0, getStorageCount()) && fluid.hasTemplate();
    }

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
        // Starting at the first index that holds anything is what keeps this cheap.
        // A caller that asks for an unspecified fluid repeats the same question for
        // every tank it was told about, and AE2's storage bus does exactly that, so a
        // walk from zero turned one poll into a walk per tank. Any index before the
        // first populated one is empty and could never answer anyway.
        int start = firstPopulatedIndex();
        if (start < 0) {
            return new TransferResult<>(requested, BigFluidStack.empty(), action);
        }
        int count = Math.max(0, getStorageCount());
        for (int index = start; index < count; index++) {
            BigFluidStack current = getSnapshot(index);
            if (!current.isEmpty() && supportsDrain(index) && supportsFluid(index, current)) {
                return drainRouted(current.withAmount(requested), action);
            }
        }
        return new TransferResult<>(requested, BigFluidStack.empty(), action);
    }

    /**
     * Reports the lowest index holding any fluid.
     *
     * <p>
     * Handlers that span many indices should memoize this and drop the memo whenever
     * contents change, because the alternative is a full walk for every untyped
     * request and such requests arrive once per reported tank. An empty handler is
     * the worst case, since every one of those requests then walks the whole array to
     * discover the same nothing.
     * </p>
     *
     * @return the lowest populated index, or {@code -1} when nothing is stored
     */
    default int firstPopulatedIndex() {
        int count = Math.max(0, getStorageCount());
        for (int index = 0; index < count; index++) {
            if (!getSnapshot(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
