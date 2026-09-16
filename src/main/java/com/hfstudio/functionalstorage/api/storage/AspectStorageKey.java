package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;

import thaumcraft.api.aspects.Aspect;

/**
 * Immutable Thaumcraft essentia identity. Essentia has no NBT component, so
 * the aspect alone is a complete exact key.
 */
public class AspectStorageKey implements StorageKey {

    private final Aspect aspect;

    public AspectStorageKey(@Nonnull Aspect aspect) {
        this.aspect = Objects.requireNonNull(aspect, "aspect");
    }

    @Nonnull
    public Aspect getAspect() {
        return aspect;
    }

    @Nonnull
    public String getTag() {
        return aspect.getTag();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AspectStorageKey)) {
            return false;
        }
        return aspect == ((AspectStorageKey) object).aspect;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(aspect);
    }

    @Override
    public String toString() {
        return "AspectStorageKey{" + aspect.getTag() + '}';
    }
}
