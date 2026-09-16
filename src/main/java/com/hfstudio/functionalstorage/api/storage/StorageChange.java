package com.hfstudio.functionalstorage.api.storage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable batch describing either indexed before/after deltas or an explicit
 * full reset. A reset never carries entries; a delta always carries at least
 * one entry and cannot name the same index twice.
 */
public class StorageChange<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    private final Type type;
    private final List<Entry<S, K>> entries;

    private StorageChange(Type type, List<Entry<S, K>> entries) {
        this.type = type;
        this.entries = entries;
    }

    /**
     * Creates a one-index delta.
     *
     * @param index  changed index
     * @param before state before the change
     * @param after  state after the change
     * @return an immutable delta
     */
    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> delta(int index, @Nonnull S before, @Nonnull S after) {
        return delta(Collections.singletonList(new Entry<>(index, before, after)));
    }

    /**
     * Creates a validated immutable multi-index delta.
     *
     * @param entries changed indices
     * @return an immutable delta
     */
    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> delta(@Nonnull List<? extends Entry<S, K>> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("DELTA requires at least one entry");
        }
        List<Entry<S, K>> copy = new ArrayList<>(entries.size());
        Set<Integer> indexes = new HashSet<>();
        for (Entry<S, K> entry : entries) {
            Entry<S, K> present = Objects.requireNonNull(entry, "entry");
            if (!indexes.add(present.getIndex())) {
                throw new IllegalArgumentException("duplicate storage index: " + present.getIndex());
            }
            copy.add(present);
        }
        return new StorageChange<>(Type.DELTA, Collections.unmodifiableList(copy));
    }

    /**
     * Creates an explicit full-resynchronization event.
     *
     * @return an immutable reset event
     */
    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> reset() {
        return new StorageChange<>(Type.RESET, Collections.emptyList());
    }

    /**
     * @return the event kind
     */
    @Nonnull
    public Type getType() {
        return type;
    }

    /**
     * @return immutable ordered delta entries, empty only for RESET
     */
    @Nonnull
    public List<Entry<S, K>> getEntries() {
        return entries;
    }

    /**
     * @return whether this event carries per-index entries
     */
    public boolean isDelta() {
        return type == Type.DELTA;
    }

    /**
     * @return whether this event requests a full resynchronization
     */
    public boolean isReset() {
        return type == Type.RESET;
    }

    /**
     * Event kinds.
     */
    public enum Type {
        DELTA,
        RESET
    }

    /**
     * Immutable before/after transition for one storage index.
     *
     * @param <S> concrete snapshot type
     * @param <K> immutable resource key type
     */
    public static class Entry<S extends StorageSnapshot<S, K>, K extends StorageKey> {

        private final int index;
        private final S before;
        private final S after;

        /**
         * Creates an entry.
         *
         * @param index  non-negative storage index
         * @param before state before the change
         * @param after  state after the change
         */
        public Entry(int index, @Nonnull S before, @Nonnull S after) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
            this.index = index;
            this.before = Objects.requireNonNull(before, "before");
            this.after = Objects.requireNonNull(after, "after");
        }

        /**
         * @return the changed index
         */
        public int getIndex() {
            return index;
        }

        /**
         * @return the state before the change
         */
        @Nonnull
        public S getBefore() {
            return before;
        }

        /**
         * @return the state after the change
         */
        @Nonnull
        public S getAfter() {
            return after;
        }
    }
}
