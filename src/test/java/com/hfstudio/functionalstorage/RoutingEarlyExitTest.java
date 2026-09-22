package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Attacks the early exits added to routing and suction.
 * <p>
 * Each early exit answers without walking the storage, so it is only correct while the
 * value it consults describes the current state exactly. These checks drive a storage
 * into each state that changes the answer, from both sides, and assert that the short
 * path and the walk agree. A void drawer matters here because it reports acceptance it
 * does not store, which is the case most likely to make a "full" verdict wrong.
 */
public class RoutingEarlyExitTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("a void drawer keeps accepting after it is full while a plain one refuses")
    void voidDrawerAcceptsWhenFull() {
        BigItemHandler voiding = new BigItemHandler(2) {

            @Override
            public boolean voidsOverflow() {
                return true;
            }
        };
        BigItemHandler plain = new BigItemHandler(2);
        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * 2;

        voiding.insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);
        plain.insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);

        long voidProcessed = voiding
            .insertRouted(new BigItemStack(StorageFixtures.one(item), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();
        long plainProcessed = plain
            .insertRouted(new BigItemStack(StorageFixtures.one(item), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();

        assertEquals(64L, voidProcessed, "a void drawer must report the overflow it destroys");
        assertEquals(0L, plainProcessed, "a plain full drawer must accept nothing");
    }

    @Test
    @DisplayName("routing into a full network refuses without reporting phantom acceptance")
    void fullNetworkRefusesInsertion() {
        List<IBigItemHandler> children = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            children.add(new BigItemHandler(4));
        }
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * 4 * 8;
        network.insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);

        assertEquals(
            0L,
            network.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a full network must accept nothing");
        assertEquals(
            1L,
            network.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.SIMULATE)
                .getRemainingAmount(),
            "a simulation must report the whole request as unprocessed");
    }

    @Test
    @DisplayName("freeing one unit reopens routing on the very next call")
    void freeingRoomReopensRouting() {
        BigItemHandler handler = new BigItemHandler(1);
        Item item = StorageFixtures.newItem();
        long capacity = handler.getCapacity(0);

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);
        assertEquals(
            0L,
            handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a full drawer must refuse");

        handler.extract(0, 1L, StorageAction.EXECUTE);

        assertEquals(
            1L,
            handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a drawer with one unit of room must accept one unit");
    }

    @Test
    @DisplayName("a locked essentia storage invites more of what it already retains")
    void lockedEssentiaInvitesMoreOfWhatItRetains() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(1, locked);
        Aspect aspect = AspectBootstrap.firstAspect();

        // Configure while unlocked: a lock refuses resources it does not already
        // retain, so a locked-from-empty storage can never be filled to begin with.
        handler.insertRouted(new BigAspectStack(aspect, 5L), StorageAction.EXECUTE);
        locked[0] = true;

        assertTrue(
            handler.summary()
                .acceptsMore(),
            "a locked storage with room invites more of its own aspect");
        assertEquals(
            64,
            handler.summary()
                .suctionAmount(),
            "a locked storage invites at the strong rate");
        assertEquals(
            aspect,
            handler.summary()
                .getSuctionType(),
            "a locked storage names the aspect it retains");
    }

    @Test
    @DisplayName("an essentia network routes to the drawer that still has room")
    void partiallyFilledNetworkStillRoutes() {
        Aspect aspect = AspectBootstrap.firstAspect();
        boolean[] locked = { false };

        BigAspectHandler first = AspectBootstrap.lockableHandler(1, locked);
        BigAspectHandler second = AspectBootstrap.lockableHandler(1, locked);
        long secondCapacity = second.getCapacity(0);

        // Fill the first drawer completely so the second is the only one with room.
        first.insertRouted(new BigAspectStack(aspect, first.getCapacity(0)), StorageAction.EXECUTE);

        List<IBigAspectHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);
        AggregatedStorage.Aspects network = new AggregatedStorage.Aspects();
        network.rebuild(children);

        assertTrue(
            network.summary()
                .acceptsMore(),
            "one drawer with room must keep the network inviting");
        assertEquals(
            10L,
            network.insertRouted(new BigAspectStack(aspect, 10L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "routing must find the drawer that still has room");
        assertEquals(
            0L,
            network.summary()
                .getTotal(AspectBootstrap.secondAspect()),
            "an unrelated aspect must not be reported as held");

        assertTrue(secondCapacity > 0L, "the fixture must have capacity");
    }

    @Test
    @DisplayName("a network of locked empty drawers advertises nothing a tube can deliver")
    void lockedEmptyNetworkAdvertisesNothing() {
        boolean[] locked = { false };
        List<IBigAspectHandler> children = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            children.add(AspectBootstrap.lockableHandler(4, locked));
        }
        AggregatedStorage.Aspects network = new AggregatedStorage.Aspects();
        network.rebuild(children);

        assertTrue(
            network.summary()
                .acceptsMore(),
            "an unlocked empty network invites essentia");

        // Locking every drawer must withdraw the invitation, because an insertion into
        // a locked drawer with no retained filter is refused. Advertising suction here
        // would make a tube deliver essentia that is then rejected. The lock is applied
        // through the real entry point, which is what notifies the network of the
        // change, rather than by flipping the flag alone.
        locked[0] = true;
        for (IBigAspectHandler child : children) {
            ((BigAspectHandler) child).applyLockConfiguration(true);
        }

        assertEquals(
            0,
            network.summary()
                .suctionAmount(),
            "a locked empty network must have no suction");
        assertEquals(
            null,
            network.summary()
                .getSuctionType(),
            "a locked empty network must invite no specific aspect");
        assertEquals(
            0L,
            network.insertRouted(new BigAspectStack(AspectBootstrap.firstAspect(), 1L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a locked empty network must accept nothing");
    }
}
