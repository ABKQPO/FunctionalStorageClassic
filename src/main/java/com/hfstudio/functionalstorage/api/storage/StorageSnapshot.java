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

    @Nullable
    K getKey();

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

    default boolean hasTemplate() {
        return getKey() != null;
    }

    default boolean isEmpty() {
        return getAmount() == 0L;
    }

    default boolean isSameType(@Nullable S other) {
        K key = getKey();
        return key != null && other != null && Objects.equals(key, other.getKey());
    }
}
