package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.Getter;

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

    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> delta(int index,
        @Nonnull S before, @Nonnull S after) {
        return delta(Collections.singletonList(new Entry<>(index, before, after)));
    }

    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> delta(
        @Nonnull List<? extends Entry<S, K>> entries) {
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

    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> StorageChange<S, K> reset() {
        return new StorageChange<>(Type.RESET, Collections.emptyList());
    }

    @Nonnull
    public Type getType() {
        return type;
    }

    @Nonnull
    public List<Entry<S, K>> getEntries() {
        return entries;
    }

    public boolean isDelta() {
        return type == Type.DELTA;
    }

    public boolean isReset() {
        return type == Type.RESET;
    }

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

        @Getter
        private final int index;
        private final S before;
        private final S after;

        public Entry(int index, @Nonnull S before, @Nonnull S after) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
            this.index = index;
            this.before = Objects.requireNonNull(before, "before");
            this.after = Objects.requireNonNull(after, "after");
        }

        @Nonnull
        public S getBefore() {
            return before;
        }

        @Nonnull
        public S getAfter() {
            return after;
        }
    }
}
