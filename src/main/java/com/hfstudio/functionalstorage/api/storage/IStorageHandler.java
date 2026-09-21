package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayList;
import java.util.List;
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
     * Chooses the single index an insertion should target.
     *
     * <p>
     * A slot already holding the same resource type always wins, so a resource
     * stays in the slot it occupies and a full slot simply accepts nothing more
     * rather than spilling into a neighbour. Otherwise the first empty slot that
     * would accept the request is used. Nothing is ever spread across slots,
     * which is what keeps generators and similar upgrades confined to one slot
     * instead of filling a whole drawer.
     *
     * @param request requested resource and amount
     * @return the chosen index, or {@code -1} when no slot can hold it
     */
    default int pickInsertionIndex(@Nonnull S request) {
        int count = Math.max(0, getStorageCount());
        int firstEmpty = -1;
        for (int index = 0; index < count; index++) {
            S current = getSnapshot(index);
            if (current.hasTemplate()) {
                if (current.isSameType(request)) {
                    return index;
                }
                continue;
            }
            if (firstEmpty < 0 && acceptsAny(index, request)) {
                firstEmpty = index;
            }
        }
        return firstEmpty;
    }

    /**
     * Reports whether one index would accept any of the request, without
     * changing state.
     *
     * @param index   storage index
     * @param request requested resource and amount
     * @return whether at least one unit would be accepted
     */
    default boolean acceptsAny(int index, @Nonnull S request) {
        if (index < 0 || index >= Math.max(0, getStorageCount())) {
            return false;
        }
        return insert(index, request.withAmount(1L), StorageAction.SIMULATE).getProcessedAmount() > 0L;
    }

    /**
     * Inserts into exactly one index, the one {@link #pickInsertionIndex} selects.
     *
     * @param request requested resource and amount
     * @param action  execute or simulate
     * @return the processed amount, zero when the target slot is full or absent
     */
    default long insertIntoSingleSlot(@Nonnull S request, @Nonnull StorageAction action) {
        int index = pickInsertionIndex(request);
        if (index < 0) {
            return 0L;
        }
        return insert(index, request, action).getProcessedAmount();
    }

    /**
     * Reports whether the slot an insertion would target still has spare capacity.
     *
     * <p>
     * Fullness is read from the stored amount against the slot capacity rather
     * than from an {@link #insert} result, because a void upgrade reports success
     * for resources it destroys. Producers rely on this to stop working once a
     * slot is full instead of generating only for the output to be voided.
     *
     * @param request requested resource and amount
     * @return whether the target slot can still hold more
     */
    default boolean hasRoomInSingleSlot(@Nonnull S request) {
        int index = pickInsertionIndex(request);
        if (index < 0) {
            return false;
        }
        S current = getSnapshot(index);
        if (!current.hasTemplate()) {
            return true;
        }
        return current.getAmount() < getCapacity(index);
    }

    /**
     * Orders candidate indices so that merging into an occupied slot is tried
     * before opening an empty one.
     *
     * <p>
     * Automation upgrades that already know which slots they may use pass their
     * candidates here instead of iterating them in index order. A slot holding any
     * resource moves ahead of an empty slot, so a resource already stored stays
     * consolidated in its slot instead of a neighbouring empty slot being opened
     * first. Order within each group is preserved.
     *
     * @param candidates candidate indices in caller order
     * @return occupied candidates first, then empty candidates
     */
    @Nonnull
    default List<Integer> preferMergeOrder(@Nonnull List<Integer> candidates) {
        List<Integer> occupied = new ArrayList<>(candidates.size());
        List<Integer> empty = new ArrayList<>(candidates.size());
        int count = Math.max(0, getStorageCount());
        for (int index : candidates) {
            if (index < 0 || index >= count) {
                continue;
            }
            (getSnapshot(index).hasTemplate() ? occupied : empty).add(index);
        }
        occupied.addAll(empty);
        return occupied;
    }

    /**
     * Picks the single index an insertion should target within a restricted set.
     *
     * <p>
     * Applies the rules of {@link #pickInsertionIndex} to the caller's candidates:
     * a slot already holding the same resource type wins, otherwise the first
     * empty candidate that would accept the request is used. Occupied candidates
     * are considered first, so a matching slot is found and topped up before any
     * empty slot is opened.
     *
     * @param candidates candidate indices in caller order
     * @param request    requested resource and amount
     * @return the chosen index, or {@code -1} when no candidate can hold it
     */
    default int pickInsertionIndex(@Nonnull List<Integer> candidates, @Nonnull S request) {
        int firstEmpty = -1;
        for (int index : preferMergeOrder(candidates)) {
            S current = getSnapshot(index);
            if (current.hasTemplate()) {
                if (current.isSameType(request)) {
                    return index;
                }
                continue;
            }
            if (firstEmpty < 0 && acceptsAny(index, request)) {
                firstEmpty = index;
            }
        }
        return firstEmpty;
    }

    /**
     * Reports whether any candidate slot that already holds a resource is close to
     * full, so callers can skip work that would only be rejected.
     *
     * @param candidates candidate indices in caller order
     * @param request    requested resource and amount
     * @return whether a matching candidate still has room
     */
    default boolean hasRoomInSingleSlot(@Nonnull List<Integer> candidates, @Nonnull S request) {
        int index = pickInsertionIndex(candidates, request);
        if (index < 0) {
            return false;
        }
        S current = getSnapshot(index);
        if (!current.hasTemplate()) {
            return true;
        }
        return current.getAmount() < getCapacity(index);
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
