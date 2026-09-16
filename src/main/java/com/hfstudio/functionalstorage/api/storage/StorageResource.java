package com.hfstudio.functionalstorage.api.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Describes one storable resource kind so a single generic indexed handler can
 * serve items, fluids, essentia, or any future kind without duplication.
 *
 * <p>
 * Implementations must be stateless and thread-safe: a single instance is
 * shared by every handler of that kind.
 * </p>
 *
 * @param <S> concrete snapshot type
 * @param <K> immutable resource key type
 */
public interface StorageResource<S extends StorageSnapshot<S, K>, K extends StorageKey> {

    /**
     * @return stable resource identifier used in NBT and diagnostics
     */
    @Nonnull
    String getId();

    /**
     * @return a snapshot representing an empty slot
     */
    @Nonnull
    S empty();

    /**
     * @param snapshot any snapshot of this kind
     * @return whether the snapshot carries a usable template
     */
    boolean hasTemplate(@Nonnull S snapshot);

    /**
     * @param snapshot candidate snapshot
     * @return an amount-independent template snapshot, or empty
     */
    @Nonnull
    S templateOf(@Nonnull S snapshot);

    /**
     * Compares two snapshots for exact resource identity, ignoring amount.
     *
     * @param left  first snapshot
     * @param right second snapshot
     * @return whether both represent the same exact resource
     */
    boolean matches(@Nonnull S left, @Nonnull S right);

    /**
     * @param template  configured slot template
     * @param candidate incoming snapshot
     * @return whether the candidate may share the template's slot
     */
    boolean accepts(@Nonnull S template, @Nonnull S candidate);

    /**
     * Returns the unit capacity of one slot holding the given template.
     *
     * @param template slot template, or empty for an unconfigured slot
     * @return capacity in resource units
     */
    long capacityFor(@Nonnull S template);

    /**
     * @return capacity of an unconfigured slot
     */
    long defaultCapacity();

    /**
     * @return capacity divisor applied to storage upgrades
     */
    int upgradeDivisor();

    /**
     * @param snapshot snapshot to persist
     * @return a fresh tag holding the template, or {@code null} when empty
     */
    @Nullable
    NBTTagCompound writeTemplate(@Nonnull S snapshot);

    /**
     * Rebuilds a snapshot from persisted data.
     *
     * @param tag    template tag previously produced by {@link #writeTemplate}
     * @param amount stored amount
     * @return the restored snapshot, or empty when the data is unusable
     */
    @Nonnull
    S readSnapshot(@Nullable NBTTagCompound tag, long amount);
}
