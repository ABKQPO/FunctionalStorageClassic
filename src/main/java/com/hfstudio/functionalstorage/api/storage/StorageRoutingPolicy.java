package com.hfstudio.functionalstorage.api.storage;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Resource-specific routing contract used by generic indexed handlers.
 * Priorities are ordered from lower to higher; a negative priority excludes a
 * candidate. Compatibility aliases are stable value keys such as ore IDs.
 *
 * @param <S> concrete snapshot type
 * @param <K> immutable resource key type
 */
public interface StorageRoutingPolicy<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    @Nullable
    default K getExactKey(@Nonnull S snapshot) {
        return snapshot.getKey();
    }

    @Nonnull
    default Collection<? extends StorageKey> getCompatibleAliases(@Nonnull S snapshot) {
        return Collections.emptyList();
    }

    boolean isEmptySlotEligible(@Nonnull IStorageHandler<S, K> handler, int index, @Nonnull S request);

    /**
     * Returns the candidate priority for the current snapshot and request.
     * Lower values are attempted first; a negative value means ineligible.
     *
     * @param handler storage handler being routed into
     * @param index   candidate index
     * @param current current snapshot of the candidate index
     * @param request requested resource
     * @return the candidate priority
     */
    int getCandidatePriority(@Nonnull IStorageHandler<S, K> handler, int index, @Nonnull S current, @Nonnull S request);
}
