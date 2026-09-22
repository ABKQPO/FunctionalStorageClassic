package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Pins the essentia summary to the exact behaviour of the scans it replaced.
 *
 * <p>
 * Thaumcraft answers suction and contents from two different rules, and a memo that
 * approximates either would make a tube pull the wrong essentia or go idle. Each
 * check states the rule it protects, so a later change that quietly alters a rule
 * fails here rather than in game.
 * </p>
 */
public class AspectSummaryTest {

    @BeforeAll
    static void installVanillaState() {
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("the summary reports every held aspect and its total")
    void reportsHeldAspects() {
        BigAspectHandler handler = new BigAspectHandler(8);
        Aspect first = AspectBootstrap.firstAspect();
        Aspect second = AspectBootstrap.secondAspect();

        handler.insertRouted(new BigAspectStack(first, 300L), StorageAction.EXECUTE);
        handler.insertRouted(new BigAspectStack(second, 70L), StorageAction.EXECUTE);

        AspectSummary summary = handler.summary();
        assertEquals(300L, summary.getTotal(first), "the first aspect total must be exact");
        assertEquals(70L, summary.getTotal(second), "the second aspect total must be exact");
        assertEquals(0L, summary.getTotal(null), "a null aspect holds nothing");
        assertEquals(
            2,
            summary.getTotals()
                .size(),
            "only held aspects may appear");
    }

    @Test
    @DisplayName("the memo is reused until something changes, then recomputed")
    void memoIsReusedUntilInvalidated() {
        BigAspectHandler handler = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();

        assertSame(handler.summary(), handler.summary(), "an unchanged storage must reuse its memo");

        handler.insertRouted(new BigAspectStack(aspect, 10L), StorageAction.EXECUTE);
        AspectSummary afterInsert = handler.summary();
        assertEquals(10L, afterInsert.getTotal(aspect), "the memo must reflect the insert");
        assertSame(afterInsert, handler.summary(), "the memo must be reused after it is recomputed");

        handler.extractRouted(new BigAspectStack(aspect, 10L), StorageAction.EXECUTE);
        assertEquals(
            0L,
            handler.summary()
                .getTotal(aspect),
            "the memo must reflect the drain");
    }

    @Test
    @DisplayName("an unlocked storage that holds nothing invites anything, so its suction names no aspect")
    void openStorageNamesNoSuctionType() {
        BigAspectHandler handler = new BigAspectHandler(4);
        AspectSummary summary = handler.summary();

        assertNull(summary.getSuctionType(), "an empty unlocked storage must not name an aspect");
        assertEquals(32, summary.suctionAmount(), "an empty unlocked storage must still invite essentia");
    }

    @Test
    @DisplayName("a storage whose every index is taken by one aspect names that aspect")
    void fullyConfiguredStorageNamesItsAspect() {
        // One slot, so no empty index can open the storage to anything else. That is
        // what the original rule keys on: any unlocked empty index answers the type
        // accessor with null, regardless of what other indices hold.
        BigAspectHandler handler = new BigAspectHandler(1);
        Aspect aspect = AspectBootstrap.firstAspect();
        handler.insertRouted(new BigAspectStack(aspect, 5L), StorageAction.EXECUTE);

        AspectSummary summary = handler.summary();
        assertEquals(aspect, summary.getSuctionType(), "the held aspect must be named while it has room");
        assertEquals(32, summary.suctionAmount(), "an unlocked storage invites at the weaker rate");
    }

    @Test
    @DisplayName("one unlocked empty index is enough to name no aspect, whatever else is held")
    void anyEmptyIndexOpensTheStorage() {
        BigAspectHandler handler = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();
        handler.insertRouted(new BigAspectStack(aspect, 5L), StorageAction.EXECUTE);

        AspectSummary summary = handler.summary();
        assertNull(summary.getSuctionType(), "an unlocked empty index must open the storage to anything");
        assertEquals(32, summary.suctionAmount(), "an open storage still invites essentia");
        assertEquals(5L, summary.getTotal(aspect), "the held amount must still be reported");
    }

    @Test
    @DisplayName("a locked storage invites at the stronger rate and keeps naming its aspect")
    void lockedStorageInvitesMoreStrongly() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(4, locked);
        Aspect aspect = AspectBootstrap.firstAspect();
        handler.insertRouted(new BigAspectStack(aspect, 5L), StorageAction.EXECUTE);
        locked[0] = true;

        AspectSummary summary = handler.summary();
        assertEquals(aspect, summary.getSuctionType(), "a locked storage still names the aspect it retains");
        assertEquals(64, summary.suctionAmount(), "a locked storage must invite at the stronger rate");
    }

    @Test
    @DisplayName("a full storage invites nothing")
    void fullStorageInvitesNothing() {
        BigAspectHandler handler = new BigAspectHandler(1);
        Aspect aspect = AspectBootstrap.firstAspect();
        long capacity = handler.getCapacity(0);

        handler.insertRouted(new BigAspectStack(aspect, capacity), StorageAction.EXECUTE);

        AspectSummary summary = handler.summary();
        assertEquals(0, summary.suctionAmount(), "a storage with no room must not invite essentia");
        assertTrue(
            summary.getTotal(aspect) >= capacity,
            "the held amount must be reported in full, was " + summary.getTotal(aspect));
    }

    @Test
    @DisplayName("a locked empty slot retains its filter instead of opening the storage")
    void lockedFilterStaysClosed() {
        boolean[] locked = { true };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(2, locked);

        AspectSummary summary = handler.summary();
        assertEquals(0, summary.suctionAmount(), "a locked storage with no retained aspect invites nothing");
        assertNull(summary.getSuctionType(), "a locked storage with no retained aspect names nothing");
    }

    @Test
    @DisplayName("a locked storage keeps whatever it already held and invites more of it")
    void lockedStorageRetainsItsContent() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(2, locked);
        Aspect aspect = AspectBootstrap.firstAspect();
        handler.insertRouted(new BigAspectStack(aspect, 40L), StorageAction.EXECUTE);

        // A lock transition clears retained filters on empty slots but leaves stored
        // amounts alone, so the filled slot survives with its aspect.
        locked[0] = true;

        long total = handler.summary()
            .getTotal(aspect);
        assertTrue(total >= 40L, "a locked storage must keep what it held, was " + total);
    }
}
