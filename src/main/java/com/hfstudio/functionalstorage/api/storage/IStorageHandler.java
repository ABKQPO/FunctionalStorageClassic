package com.hfstudio.functionalstorage.api.storage;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

/**
 * Generic indexed long-capacity storage. Implementations follow the threading
 * model of their owning tile or capability and are not implicitly thread-safe.
 *
 * @param <S> immutable snapshot self type
 * @param <K> immutable exact resource key type
 */
public interface IStorageHandler<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    int getStorageCount();

    /**
     * @param index storage index
     * @return detached immutable snapshot, or an unconfigured empty snapshot for an invalid index
     */
    @Nonnull
    S getSnapshot(int index);

    /**
     * @param index storage index
     * @return non-negative long capacity, or zero for an invalid index
     */
    long getCapacity(int index);

    /**
     * Inserts into exactly one index.
     *
     * @param index   storage index
     * @param request requested resource and amount
     * @param action  execute or simulate
     * @return the processed amount
     */
    @Nonnull
    TransferResult<S, K> insert(int index, @Nonnull S request, @Nonnull StorageAction action);

    /**
     * Extracts from exactly one index.
     *
     * @param index  storage index
     * @param amount requested amount
     * @param action execute or simulate
     * @return the processed amount
     */
    @Nonnull
    TransferResult<S, K> extract(int index, long amount, @Nonnull StorageAction action);

    /**
     * @return whether empty storage retains and enforces a resource filter
     */
    default boolean isLocked() {
        return false;
    }

    /**
     * @return whether compatible overflow is consumed instead of returned
     */
    default boolean voidsOverflow() {
        return false;
    }

    /**
     * @param index storage index
     * @return whether the indexed storage consumes compatible overflow
     */
    default boolean voidsOverflow(int index) {
        return voidsOverflow();
    }

    /**
     * @return whether extraction can report resources without consuming storage
     */
    default boolean isCreative() {
        return false;
    }

    default double getMultiplier() {
        return 1.0D;
    }

    /**
     * @return stable physical storage identity; wrappers should forward their
     *         target identity so aggregate handlers can remove duplicates
     */
    @Nonnull
    default Object getStorageIdentity() {
        return this;
    }

    /**
     * Publishes a completed observable change. Stateless compatibility handlers
     * may keep the default no-op; mutable handlers should delegate to a
     * {@link StorageChangeDispatcher}.
     *
     * @param change completed change
     */
    default void onChange(@Nonnull StorageChange<S, K> change) {}

    /**
     * Subscribes to observable storage changes. Handlers without an event
     * source return an already-closed subscription.
     *
     * @param listener notified for each subsequent change
     * @return a subscription that stops notifications when closed
     */
    @Nonnull
    default StorageSubscription subscribe(@Nonnull Consumer<? super StorageChange<S, K>> listener) {
        return StorageSubscription.CLOSED;
    }
}
