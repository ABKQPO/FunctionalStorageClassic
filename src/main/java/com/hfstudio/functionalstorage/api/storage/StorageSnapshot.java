package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Immutable view of one stored resource and its non-negative long amount.
 *
 * @param <S> concrete self type
 * @param <K> immutable resource key type
 */
public interface StorageSnapshot<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    /**
     * @return the exact resource key, or {@code null} for an unconfigured slot
     */
    @Nullable
    K getKey();

    /**
     * @return the represented amount, always zero or greater
     */
    long getAmount();

    /**
     * Creates an immutable snapshot with the same key and a different amount.
     * A zero amount must retain the key when this snapshot has one.
     *
     * @param amount new represented amount
     * @return a snapshot with the requested amount
     */
    @Nonnull
    S withAmount(long amount);

    /**
     * @return whether this snapshot retains a resource template or filter
     */
    default boolean hasTemplate() {
        return getKey() != null;
    }

    /**
     * @return whether the represented physical amount is zero
     */
    default boolean isEmpty() {
        return getAmount() == 0L;
    }

    /**
     * @return whether both snapshots have equal non-null exact keys
     */
    default boolean isSameType(@Nullable S other) {
        K key = getKey();
        return key != null && other != null && Objects.equals(key, other.getKey());
    }
}
