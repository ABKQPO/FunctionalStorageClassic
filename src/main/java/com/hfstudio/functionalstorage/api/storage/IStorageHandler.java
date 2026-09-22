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
     * @return detached immutable snapshot, or an unconfigured empty snapshot for an
     *         invalid index
     */
    @Nonnull
    S getSnapshot(int index);

    /**
     * @return non-negative long capacity, or zero for an invalid index
     */
    long getCapacity(int index);

    @Nonnull
    TransferResult<S, K> insert(int index, @Nonnull S request, @Nonnull StorageAction action);

    @Nonnull
    TransferResult<S, K> extract(int index, long amount, @Nonnull StorageAction action);

    /**
     * @return whether empty storage retains and enforces a resource filter
     */
    default boolean isLocked() {
        return false;
    }

    /**
     * Locking is a property of a single storage, but one that spans several drawers
     * carries one lock per drawer, so a caller asking about one index must use this
     * rather than {@link #isLocked()}.
     *
     * @return whether that index refuses resources it does not already retain
     */
    default boolean isLocked(int index) {
        return isLocked();
    }

    /**
     * Chooses the single index an insertion should target. A slot already holding the
     * same resource type always wins, so a resource stays where it is and a full slot
     * accepts nothing more rather than spilling into a neighbour. Otherwise the first
     * empty slot that would accept the request is used. Nothing is ever spread across
     * slots, which keeps generators and similar upgrades confined to one slot.
     *
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
     * @return whether at least one unit would be accepted, without changing state
     */
    default boolean acceptsAny(int index, @Nonnull S request) {
        if (index < 0 || index >= Math.max(0, getStorageCount())) {
            return false;
        }
        return insert(index, request.withAmount(1L), StorageAction.SIMULATE).getProcessedAmount() > 0L;
    }

    /**
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
     * Reports whether this storage already holds the requested resource type.
     *
     * <p>
     * Callers that top up storage with whatever a player hands over, rather than
     * targeting a slot, use this to decide whether they may write at all. An
     * unfamiliar resource is refused, so such a caller never starts a new pile beside
     * resources that are already stored. An index whose contents are full still
     * answers {@code true}, because the type is known and a caller may legitimately
     * deposit into a further index of the same drawer, while a retained filter counts
     * for the same reason even though the index is empty.
     * </p>
     */
    default boolean hasMatchingResource(@Nonnull S request) {
        int count = Math.max(0, getStorageCount());
        // A storage that never treats different resources as interchangeable can only
        // accept into a slot already holding the exact type, so probing every occupied
        // index for compatibility would only confirm what the exact comparison decided.
        boolean probeCompatible = allowsEquivalentResources();
        for (int index = 0; index < count; index++) {
            S current = getSnapshot(index);
            if (!current.hasTemplate()) {
                continue;
            }
            if (current.isSameType(request) || (probeCompatible && acceptsAny(index, request))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports whether the slot an insertion would target still has spare capacity.
     * Fullness is read from the stored amount against the slot capacity rather than
     * from an {@link #insert} result, because a void upgrade reports success for
     * resources it destroys; producers rely on this to stop working once a slot is
     * full instead of generating only for the output to be voided.
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
     * Orders candidate indices so that merging into an occupied slot is tried before
     * opening an empty one, which keeps a stored resource consolidated instead of
     * opening a neighbouring empty slot first. Out-of-range candidates are dropped,
     * and order within each group is preserved.
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
     * Applies {@link #pickInsertionIndex} to a restricted candidate set rather than to
     * every index.
     *
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
     * @return whether the chosen candidate still has room, so callers can skip work
     *         that would only be rejected
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
     * Reports whether resources that are not exactly equal may still be
     * interchangeable, such as two items sharing an ore dictionary entry.
     *
     * <p>
     * Routing uses this to decide whether a compatibility probe is worth making: when
     * only an exact match can be compatible, a probe against a slot holding a different
     * resource is guaranteed to fail, so that walk is skipped. Reporting {@code false}
     * while a subclass does accept equivalents would silently stop such resources from
     * sharing a slot.
     * </p>
     */
    default boolean allowsEquivalentResources() {
        return false;
    }

    /**
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
     * @return stable physical storage identity; wrappers must forward their target
     *         identity so aggregates can remove duplicates
     */
    @Nonnull
    default Object getStorageIdentity() {
        return this;
    }

    /**
     * Stateless compatibility handlers may keep the default no-op; mutable handlers
     * should delegate to a {@link StorageChangeDispatcher}.
     */
    default void onChange(@Nonnull StorageChange<S, K> change) {}

    /**
     * @return a subscription that stops notifications when closed; handlers without an
     *         event source return an already-closed subscription
     */
    @Nonnull
    default StorageSubscription subscribe(@Nonnull Consumer<? super StorageChange<S, K>> listener) {
        return StorageSubscription.CLOSED;
    }
}
