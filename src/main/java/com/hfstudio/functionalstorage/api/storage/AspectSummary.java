package com.hfstudio.functionalstorage.api.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import thaumcraft.api.aspects.Aspect;

/**
 * What an essentia storage holds and what it would accept, from one walk of the
 * storage.
 *
 * <p>
 * Thaumcraft asks a container for its contents and its suction far more often than
 * they change, and a tube asks every neighbouring side on its own timer, so a handler
 * spanning many indices memoizes this and drops it whenever something changes.
 * </p>
 *
 * <p>
 * The two suction accessors follow different rules and both are reproduced exactly.
 * The amount reports whether any index would take another unit, preferring the
 * stronger figure while a filter is retained. The type answers null as soon as any
 * unlocked index holds nothing, and otherwise names the first held aspect with room.
 * </p>
 */
public class AspectSummary {

    private static final AspectSummary EMPTY = new AspectSummary(Collections.emptyMap(), false, false, 0, null);

    private static final int UNLOCKED_SUCTION = 32;

    private static final int LOCKED_SUCTION = 64;

    private final Map<Aspect, Long> totals;
    private final boolean acceptsMore;
    private final boolean openToAnything;
    private final int suctionAmount;
    @Nullable
    private final Aspect suctionType;

    private AspectSummary(Map<Aspect, Long> totals, boolean acceptsMore, boolean openToAnything, int suctionAmount,
        @Nullable Aspect suctionType) {
        this.totals = totals;
        this.acceptsMore = acceptsMore;
        this.openToAnything = openToAnything;
        this.suctionAmount = suctionAmount;
        this.suctionType = suctionType;
    }

    @Nonnull
    public static AspectSummary of(@Nonnull IBigAspectHandler handler) {
        Map<Aspect, Long> totals = new LinkedHashMap<>();
        boolean acceptsMore = false;
        boolean openToAnything = false;
        int amount = 0;
        Aspect firstAccepting = null;
        int count = Math.max(0, handler.getStorageCount());

        for (int index = 0; index < count; index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            Aspect aspect = snapshot.getAspect();
            if (aspect != null && !snapshot.isEmpty()) {
                totals.merge(aspect, snapshot.getAmount(), AspectSummary::saturatedAdd);
            }

            if (!snapshot.hasTemplate()) {
                // The lock is read per index: a storage spanning several drawers carries
                // one lock per drawer, so asking it as a whole would advertise room that
                // the drawer behind the index will refuse.
                if (handler.isLocked(index)) {
                    continue;
                }
                openToAnything = true;
                if (!acceptsMore && handler.getCapacity(index) > 0L) {
                    amount = UNLOCKED_SUCTION;
                    acceptsMore = true;
                }
                continue;
            }

            // The probe writes into the handler in simulation, so it runs only while it
            // can still change an answer.
            boolean needsAmount = !acceptsMore;
            boolean needsType = !openToAnything && firstAccepting == null;
            if (!needsAmount && !needsType) {
                continue;
            }

            if (!handler.insert(index, snapshot.withAmount(1L), StorageAction.SIMULATE)
                .isComplete()) {
                continue;
            }
            if (needsAmount) {
                // The original amount scan returned at the first index that accepted, so
                // an index that accepted earlier owns the figure.
                amount = handler.isLocked(index) ? LOCKED_SUCTION : UNLOCKED_SUCTION;
                acceptsMore = true;
            }
            if (needsType && aspect != null) {
                firstAccepting = aspect;
            }
        }

        if (totals.isEmpty() && !acceptsMore && !openToAnything) {
            return EMPTY;
        }
        return new AspectSummary(
            Collections.unmodifiableMap(totals),
            acceptsMore,
            openToAnything,
            amount,
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

    public long getTotal(@Nullable Aspect aspect) {
        if (aspect == null) {
            return 0L;
        }
        return totals.getOrDefault(aspect, 0L);
    }

    public boolean acceptsMore() {
        return acceptsMore;
    }

    public int suctionAmount() {
        return acceptsMore ? suctionAmount : 0;
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
