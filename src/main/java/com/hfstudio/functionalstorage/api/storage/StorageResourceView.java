package com.hfstudio.functionalstorage.api.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.Getter;

/**
 * Read-only aggregate view for one resource kind.
 *
 * <p>
 * The view keeps the exact resource key and the summed long amount while
 * leaving every mutation on the owning handler. A zero amount is retained when
 * the handler still carries a template, which preserves locked display entries.
 * </p>
 *
 * @param <S> concrete snapshot type
 * @param <K> exact resource key type
 */
public class StorageResourceView<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    @Getter
    private final S snapshot;
    @Getter
    private final long capacity;
    @Getter
    private final boolean voidsOverflow;

    public StorageResourceView(@Nonnull S snapshot, long capacity, boolean voidsOverflow) {
        this.snapshot = snapshot;
        this.capacity = Math.max(0L, capacity);
        this.voidsOverflow = voidsOverflow;
    }

    @Nonnull
    public static <S extends StorageSnapshot<S, K>, K extends StorageKey> List<StorageResourceView<S, K>> aggregate(
        @Nonnull IStorageHandler<S, K> handler) {
        Map<K, StorageResourceView<S, K>> byKey = new LinkedHashMap<>();
        int count = Math.max(0, handler.getStorageCount());
        for (int index = 0; index < count; index++) {
            S current = handler.getSnapshot(index);
            if (!current.hasTemplate()) {
                continue;
            }
            K key = current.getKey();
            StorageResourceView<S, K> previous = byKey.get(key);
            if (previous == null) {
                byKey.put(
                    key,
                    new StorageResourceView<>(current, handler.getCapacity(index), handler.voidsOverflow(index)));
            } else {
                byKey.put(
                    key,
                    new StorageResourceView<>(
                        previous.snapshot.withAmount(saturatedAdd(previous.snapshot.getAmount(), current.getAmount())),
                        saturatedAdd(previous.capacity, handler.getCapacity(index)),
                        previous.voidsOverflow || handler.voidsOverflow(index)));
            }
        }
        return List.copyOf(byKey.values());
    }

    private static long saturatedAdd(long left, long right) {
        long safeLeft = Math.max(0L, left);
        long safeRight = Math.max(0L, right);
        return safeLeft > Long.MAX_VALUE - safeRight ? Long.MAX_VALUE : safeLeft + safeRight;
    }
}
