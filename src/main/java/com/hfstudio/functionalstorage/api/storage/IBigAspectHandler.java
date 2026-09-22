package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import thaumcraft.api.aspects.Aspect;

/**
 * Generic essentia capability surface for a long-capacity storage handler.
 * Thaumcraft's native {@code IAspectContainer} is int-based, so this interface
 * carries the long-capacity operations while the tile adapts it to the
 * Thaumcraft API.
 */
public interface IBigAspectHandler extends IStorageHandler<BigAspectStack, AspectStorageKey> {

    /**
     * Returns a memo of this storage's contents, or {@code null} to compute one on
     * each request.
     *
     * <p>
     * Thaumcraft polls a container for its contents and its suction repeatedly, and a
     * tube asks every neighbouring side on its own timer, so the same walk is
     * repeated long after the answer stopped changing. A handler that spans many
     * indices should keep a memo it drops on every change; one that does not is
     * summarized per request, which is correct and merely slower.
     * </p>
     *
     * @return this handler's memo, or {@code null} to summarize per request
     */
    @Nullable
    default AspectSummary getSummary() {
        return null;
    }

    /**
     * Summarizes this storage, reusing the handler's memo when it keeps one.
     *
     * @return an immutable summary of the current contents
     */
    @Nonnull
    default AspectSummary summary() {
        AspectSummary cached = getSummary();
        return cached == null ? AspectSummary.of(this) : cached;
    }

    default boolean supportsAspect(int index, @Nullable Aspect aspect) {
        if (aspect == null || index < 0 || index >= Math.max(0, getStorageCount())) {
            return false;
        }
        BigAspectStack current = getSnapshot(index);
        return !current.hasTemplate() || current.isSameType(aspect);
    }

    /**
     * Routes insertion through matching configured indices before empty indices.
     *
     * @param request requested aspect and amount
     * @param action  execute or simulate
     * @return the routed result
     */
    @Nonnull
    default TransferResult<BigAspectStack, AspectStorageKey> insertRouted(@Nonnull BigAspectStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigAspectStack.empty(), action);
        }
        // Nothing that refuses a single unit can accept any amount, because an index
        // either has room and takes at least one, or has none and takes nothing. A
        // full network therefore answers immediately instead of walking every index
        // twice to discover it is full.
        if (!summary().acceptsMore()) {
            return new TransferResult<>(requested, request.withAmount(0L), action);
        }
        long processedTotal = 0L;
        int count = Math.max(0, getStorageCount());
        for (int pass = 0; pass < 2 && processedTotal < requested; pass++) {
            for (int index = 0; index < count && processedTotal < requested; index++) {
                if (!supportsAspect(index, request.getAspect())) {
                    continue;
                }
                BigAspectStack current = getSnapshot(index);
                boolean hasTemplate = current.hasTemplate();
                if ((pass == 0 && (!hasTemplate || !current.isSameType(request))) || (pass == 1 && hasTemplate)) {
                    continue;
                }
                long remaining = requested - processedTotal;
                TransferResult<BigAspectStack, AspectStorageKey> result = insert(
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
     * Routes extraction through matching generic indices.
     *
     * @param request requested aspect and amount
     * @param action  execute or simulate
     * @return the routed result
     */
    @Nonnull
    default TransferResult<BigAspectStack, AspectStorageKey> extractRouted(@Nonnull BigAspectStack request,
        @Nonnull StorageAction action) {
        Objects.requireNonNull(action, "action");
        long requested = request.isEmpty() ? 0L : request.getAmount();
        if (requested == 0L) {
            return new TransferResult<>(0L, BigAspectStack.empty(), action);
        }
        long processedTotal = 0L;
        int count = Math.max(0, getStorageCount());
        for (int index = 0; index < count && processedTotal < requested; index++) {
            BigAspectStack current = getSnapshot(index);
            if (current.isEmpty() || !current.isSameType(request)) {
                continue;
            }
            long remaining = requested - processedTotal;
            TransferResult<BigAspectStack, AspectStorageKey> result = extract(index, remaining, action);
            long processed = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
            processedTotal = saturatedAdd(processedTotal, processed);
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
