package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Immutable outcome of a storage request. The processed snapshot describes
 * what the caller may insert, remove, or otherwise consume. For void and
 * creative storage this amount need not equal a physical state change.
 *
 * @param <S> concrete snapshot type
 * @param <K> immutable resource key type
 */
public class TransferResult<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    @Getter
    private final long requestedAmount;
    private final S processed;
    private final StorageAction action;

    /**
     * Creates a validated result.
     *
     * @param requestedAmount requested amount
     * @param processed       amount actually accepted or produced
     * @param action          action that produced this result
     * @throws NullPointerException     if {@code processed} or {@code action} is null
     * @throws IllegalArgumentException if amounts are negative, processed exceeds
     *                                  requested, or emptiness contradicts the amount
     */
    public TransferResult(long requestedAmount, @Nonnull S processed, @Nonnull StorageAction action) {
        this.processed = Objects.requireNonNull(processed, "processed");
        this.action = Objects.requireNonNull(action, "action");
        long processedAmount = processed.getAmount();
        if (requestedAmount < 0L) {
            throw new IllegalArgumentException("requestedAmount must be non-negative");
        }
        if (processedAmount < 0L || processedAmount > requestedAmount) {
            throw new IllegalArgumentException("processed amount must be between zero and requestedAmount");
        }
        if (processed.isEmpty() != (processedAmount == 0L)) {
            throw new IllegalArgumentException("processed emptiness must match its amount");
        }
        this.requestedAmount = requestedAmount;
    }

    /**
     * @return the processed snapshot
     */
    @Nonnull
    public S getProcessed() {
        return processed;
    }

    /**
     * @return the processed amount
     */
    public long getProcessedAmount() {
        return processed.getAmount();
    }

    /**
     * @return the action that produced this result
     */
    @Nonnull
    public StorageAction getAction() {
        return action;
    }

    /**
     * @return the amount the storage could not process
     */
    public long getRemainingAmount() {
        return requestedAmount - processed.getAmount();
    }

    /**
     * @return whether the whole request was processed
     */
    public boolean isComplete() {
        return getRemainingAmount() == 0L;
    }
}
