package com.hfstudio.functionalstorage.api.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import thaumcraft.api.aspects.Aspect;

/**
 * Immutable summary of every aspect an essentia storage holds.
 *
 * <p>
 * Thaumcraft asks a container the same questions over and over: what it holds, how
 * much of one aspect it holds, and whether it would accept more. Answering them by
 * walking the storage costs one pass per question, and a tube polls suction for
 * every neighbouring side on its own timer, so an aggregated network is walked
 * thousands of times a second for an answer that only changes when an item moves.
 * </p>
 *
 * <p>
 * One pass produces every answer at once. Suction deserves a note because its two
 * accessors follow different rules. The amount reports whether any index would take
 * another unit, preferring the stronger figure while a filter is retained. The type
 * refuses outright, answering null, as soon as any unlocked index holds nothing,
 * and otherwise names the first held aspect that still has room. Both rules are
 * reproduced exactly rather than approximated.
 * </p>
 */
public class AspectSummary {

    private static final AspectSummary EMPTY = new AspectSummary(Collections.emptyMap(), false, false, false, null);

    private final Map<Aspect, Long> totals;
    private final boolean acceptsMore;
    private final boolean locked;
    private final boolean openToAnything;
    @Nullable
    private final Aspect suctionType;

    private AspectSummary(Map<Aspect, Long> totals, boolean acceptsMore, boolean locked, boolean openToAnything,
        @Nullable Aspect suctionType) {
        this.totals = totals;
        this.acceptsMore = acceptsMore;
        this.locked = locked;
        this.openToAnything = openToAnything;
        this.suctionType = suctionType;
    }

    /**
     * Summarizes a handler in a single pass over its indices.
     *
     * @param handler handler to read
     * @return an immutable summary of its current contents
     */
    @Nonnull
    public static AspectSummary of(@Nonnull IBigAspectHandler handler) {
        Map<Aspect, Long> totals = new LinkedHashMap<>();
        boolean locked = handler.isLocked();
        boolean acceptsMore = false;
        boolean openToAnything = false;
        Aspect firstAccepting = null;
        int count = Math.max(0, handler.getStorageCount());

        for (int index = 0; index < count; index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            Aspect aspect = snapshot.getAspect();
            if (aspect != null && !snapshot.isEmpty()) {
                totals.merge(aspect, snapshot.getAmount(), AspectSummary::saturatedAdd);
            }

            if (!snapshot.hasTemplate()) {
                // An unlocked index holding nothing invites whatever arrives, which the
                // amount accessor treats as room and the type accessor as a refusal.
                if (!locked) {
                    openToAnything = true;
                    if (handler.getCapacity(index) > 0L) {
                        acceptsMore = true;
                    }
                }
                continue;
            }

            if (handler.insert(index, snapshot.withAmount(1L), StorageAction.SIMULATE)
                .isComplete()) {
                acceptsMore = true;
                if (firstAccepting == null && aspect != null) {
                    firstAccepting = aspect;
                }
            }
        }

        if (totals.isEmpty() && !acceptsMore && !locked && !openToAnything) {
            return EMPTY;
        }
        return new AspectSummary(
            Collections.unmodifiableMap(totals),
            acceptsMore,
            locked,
            openToAnything,
            firstAccepting);
    }

    @Nonnull
    public static AspectSummary empty() {
        return EMPTY;
    }

    /**
     * @return every held aspect and its total, in the order the indices were walked
     */
    @Nonnull
    public Map<Aspect, Long> getTotals() {
        return totals;
    }

    /**
     * @param aspect aspect to total
     * @return the amount stored under that aspect, or zero
     */
    public long getTotal(@Nullable Aspect aspect) {
        if (aspect == null) {
            return 0L;
        }
        return totals.getOrDefault(aspect, 0L);
    }

    /**
     * @return whether any index would accept one more unit of what it holds
     */
    public boolean acceptsMore() {
        return acceptsMore;
    }

    /**
     * @return the suction a tube sees, which is stronger while a filter is retained
     */
    public int suctionAmount() {
        if (!acceptsMore) {
            return 0;
        }
        return locked ? 64 : 32;
    }

    /**
     * @return the aspect the storage invites, or {@code null} when it is open to
     *         anything or full
     */
    @Nullable
    public Aspect getSuctionType() {
        return openToAnything ? null : suctionType;
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
