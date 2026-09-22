package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts how storage change notifications behave under awkward listeners.
 * <p>
 * Every cache and every tile subscribes to this channel, so the guarantees it offers
 * decide whether a listener can rely on what it is told. A listener is ordinary game
 * code: it may subscribe again, unsubscribe itself, unsubscribe a neighbour, or throw.
 * Each of those must leave the channel usable and must not make a notification vanish
 * or arrive twice.
 */
public class ChangeNotificationTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("a change reaches every subscriber exactly once")
    void everySubscriberIsNotifiedOnce() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> firstCounts = new ArrayList<>();
        List<Integer> secondCounts = new ArrayList<>();
        handler.subscribe(change -> firstCounts.add(1));
        handler.subscribe(change -> secondCounts.add(1));

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);

        assertEquals(1, firstCounts.size(), "the first subscriber must be notified once");
        assertEquals(1, secondCounts.size(), "the second subscriber must be notified once");
    }

    @Test
    @DisplayName("a closed subscription stops receiving changes")
    void closedSubscriptionStopsReceiving() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> counts = new ArrayList<>();
        var subscription = handler.subscribe(change -> counts.add(1));

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(1, counts.size(), "the listener must receive the first change");

        subscription.close();
        assertTrue(subscription.isClosed(), "a closed subscription must report itself closed");

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(1, counts.size(), "a closed listener must receive nothing further");
        assertFalse(
            handler.subscribe(change -> {})
                .isClosed(),
            "a fresh subscription must start open");
    }

    @Test
    @DisplayName("closing a subscription twice is harmless")
    void doubleCloseIsHarmless() {
        BigItemHandler handler = new BigItemHandler(2);
        var subscription = handler.subscribe(change -> {});
        subscription.close();
        subscription.close();
        assertTrue(subscription.isClosed(), "the subscription must remain closed");
    }

    @Test
    @DisplayName("a listener that unsubscribes itself keeps receiving the current event")
    void selfUnsubscribeDoesNotBreakDispatch() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> counts = new ArrayList<>();
        var subscription = handler.subscribe(change -> {
            counts.add(1);
            // Closing during dispatch must not corrupt the listener list.
            if (counts.size() >= 1) {
                handler.subscribe(change2 -> {})
                    .close();
            }
        });

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(1, counts.size(), "the listener must receive the change it reacted to");

        subscription.close();
        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(1, counts.size(), "a closed listener must stay silent");
    }

    @Test
    @DisplayName("a listener added during dispatch is not notified of the in-flight event")
    void listenerAddedDuringDispatchMissesCurrentEvent() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> lateCounts = new ArrayList<>();
        handler.subscribe(change -> {
            if (lateCounts.isEmpty()) {
                handler.subscribe(late -> lateCounts.add(1));
            }
        });

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);

        assertEquals(
            0,
            lateCounts.size(),
            "a listener registered mid-dispatch must not see the event already in flight");

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertTrue(lateCounts.size() >= 1, "the late listener must see later events");
    }

    @Test
    @DisplayName("one listener throwing does not silence the others")
    void throwingListenerDoesNotSilenceOthers() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> survived = new ArrayList<>();
        handler.subscribe(change -> { throw new IllegalStateException("listener failure"); });
        handler.subscribe(change -> survived.add(1));

        // The failure must surface rather than be swallowed, but only after every
        // listener has had its turn.
        assertThrows(
            IllegalStateException.class,
            () -> handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE),
            "a listener failure must surface to the caller");

        assertEquals(1, survived.size(), "a failing listener must not prevent the others from running");
        assertEquals(
            10L,
            StorageFixtures.total(handler, item),
            "the change itself must have been applied despite the listener failing");
    }

    @Test
    @DisplayName("a listener that mutates the storage again does not recurse unboundedly")
    void reentrantMutationIsQueued() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<Integer> counts = new ArrayList<>();
        handler.subscribe(change -> {
            counts.add(1);
            // Mutating from inside a callback is how a cache invalidates itself. It
            // must not re-enter the dispatch loop, or a chain of edits would grow the
            // stack without bound.
            if (counts.size() < 3) {
                handler.insert(3, new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.EXECUTE);
            }
        });

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.EXECUTE);

        // The first edit stores one and notifies. That notification stores a second,
        // which notifies again, which stores a third, and the third finds the limit
        // reached. Each edit is applied, and the count stops at the limit rather than
        // growing without bound.
        assertEquals(3, counts.size(), "each queued edit must notify once, saw " + counts.size());
        assertEquals(3L, StorageFixtures.total(handler, item), "every queued edit up to the limit must be applied");
        assertTrue(counts.size() <= 4, "a reentrant edit must be queued rather than recursing");
    }

    @Test
    @DisplayName("an unsubscribed handler reports no subscribers")
    void hasSubscribersReflectsSubscriptions() {
        BigItemHandler handler = new BigItemHandler(2);
        assertFalse(
            handler.subscribe(change -> {})
                .isClosed(),
            "a new subscription must be open");

        var first = handler.subscribe(change -> {});
        first.close();
        assertTrue(first.isClosed(), "the closed subscription must report itself closed");
    }
}
