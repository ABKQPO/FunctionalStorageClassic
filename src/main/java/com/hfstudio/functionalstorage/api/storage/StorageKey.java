package com.hfstudio.functionalstorage.api.storage;

/**
 * Immutable identity of a stored resource type.
 *
 * <p>
 * Implementations must use value-based {@link Object#equals(Object)} and
 * {@link Object#hashCode()} implementations whose results remain stable for
 * the lifetime of the key. Mutable game objects must therefore be copied or
 * reduced to immutable identity fields before being retained by a key.
 * </p>
 */
public interface StorageKey {
}
