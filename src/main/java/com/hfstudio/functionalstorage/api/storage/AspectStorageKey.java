package com.hfstudio.functionalstorage.api.storage;

import javax.annotation.Nonnull;
import java.util.Objects;

import thaumcraft.api.aspects.Aspect;

/**
 * Immutable Thaumcraft essentia identity. Essentia has no NBT component, so
 * the aspect alone is a complete exact key.
 */
public class AspectStorageKey implements StorageKey {

    private final Aspect aspect;

    /**
     * Creates a key.
     *
     * @param aspect represented aspect
     * @throws NullPointerException if {@code aspect} is null
     */
    public AspectStorageKey(@Nonnull Aspect aspect) {
        this.aspect = Objects.requireNonNull(aspect, "aspect");
    }

    /**
     * @return the represented aspect
     */
    @Nonnull
    public Aspect getAspect() {
        return aspect;
    }

    /**
     * @return the stable aspect tag used for serialization
     */
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
