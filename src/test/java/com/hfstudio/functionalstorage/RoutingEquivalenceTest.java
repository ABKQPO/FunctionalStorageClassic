package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Pins routing behaviour for both kinds of item storage.
 *
 * <p>
 * Routing skips its compatibility pass when nothing but an exact match can ever be
 * compatible, because a probe against a slot holding a different resource would then
 * be guaranteed to fail. That shortcut is only correct while every storage that does
 * widen matching says so, so these checks exercise both kinds and assert that a
 * widened storage still shares a slot while an exact one never does.
 * </p>
 */
public class RoutingEquivalenceTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("an exact storage fills its matching slot before opening an empty one")
    void exactStoragePrefersItsOwnSlot() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(0, indexOf(handler, item), "the first insert must take an index");

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(20L, StorageFixtures.total(handler, item), "the second insert must top up the same index");
        assertEquals(1, countPopulated(handler), "a matching index must be reused rather than a new one opened");
    }

    @Test
    @DisplayName("an exact storage never merges two different resources")
    void exactStorageKeepsResourcesApart() {
        BigItemHandler handler = new BigItemHandler(4);
        Item first = StorageFixtures.newItem();
        Item second = StorageFixtures.newItem();

        handler.insertRouted(new BigItemStack(StorageFixtures.one(first), 10L), StorageAction.EXECUTE);
        handler.insertRouted(new BigItemStack(StorageFixtures.one(second), 10L), StorageAction.EXECUTE);

        assertEquals(10L, StorageFixtures.total(handler, first), "the first resource must be intact");
        assertEquals(10L, StorageFixtures.total(handler, second), "the second resource must take its own index");
        assertEquals(2, countPopulated(handler), "two different resources must occupy two indices");
        assertFalse(handler.allowsEquivalentResources(), "an exact storage must report that it widens nothing");
    }

    @Test
    @DisplayName("an exact storage reports a resource it holds and refuses one it does not")
    void exactStorageReportsMatchingResource() {
        BigItemHandler handler = new BigItemHandler(4);
        Item stored = StorageFixtures.newItem();
        Item absent = StorageFixtures.newItem();

        handler.insertRouted(new BigItemStack(StorageFixtures.one(stored), 10L), StorageAction.EXECUTE);

        assertTrue(
            handler.hasMatchingResource(new BigItemStack(StorageFixtures.one(stored), 1L)),
            "a held resource must be reported as known");
        assertFalse(
            handler.hasMatchingResource(new BigItemStack(StorageFixtures.one(absent), 1L)),
            "an unknown resource must be refused so a new pile is not started");
    }

    @Test
    @DisplayName("a widened storage merges a resource that is not exactly equal")
    void widenedStorageStillSharesASlot() {
        // Stands in for an ore dictionary drawer: resources with the same metadata are
        // interchangeable even though they are different items.
        BigItemHandler widened = new BigItemHandler(4) {

            @Override
            protected boolean allowsEquivalentItems() {
                return true;
            }

            @Override
            protected boolean isCompatible(BigItemStack template, BigItemStack candidate) {
                return template.getTemplate()
                    .getItemDamage()
                    == candidate.getTemplate()
                        .getItemDamage();
            }
        };

        assertTrue(widened.allowsEquivalentResources(), "a widened storage must say so, or routing skips its probe");

        Item first = StorageFixtures.newItem();
        Item second = StorageFixtures.newItem();

        widened.insertRouted(new BigItemStack(StorageFixtures.one(first), 10L), StorageAction.EXECUTE);
        widened.insertRouted(new BigItemStack(StorageFixtures.one(second), 10L), StorageAction.EXECUTE);

        assertEquals(1, countPopulated(widened), "a widened storage must merge into one index");
        assertEquals(
            20L,
            StorageFixtures.total(widened, first) + StorageFixtures.total(widened, second),
            "both resources must be stored");
    }

    @Test
    @DisplayName("a controller over a widened drawer keeps widening its matching")
    void aggregateForwardsWidening() {
        BigItemHandler widened = new BigItemHandler(4) {

            @Override
            protected boolean allowsEquivalentItems() {
                return true;
            }
        };
        BigItemHandler exact = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<IBigItemHandler> widenedChildren = new ArrayList<>();
        widenedChildren.add(widened);
        AggregatedStorage.Items widenedNetwork = new AggregatedStorage.Items();
        widenedNetwork.rebuild(widenedChildren);

        List<IBigItemHandler> exactChildren = new ArrayList<>();
        exactChildren.add(exact);
        AggregatedStorage.Items exactNetwork = new AggregatedStorage.Items();
        exactNetwork.rebuild(exactChildren);

        assertTrue(
            widenedNetwork.allowsEquivalentResources(),
            "an aggregate must report widening when any spanned storage widens");
        assertFalse(exactNetwork.allowsEquivalentResources(), "an aggregate of exact storages must report no widening");

        widenedNetwork.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        assertEquals(10L, StorageFixtures.total(widened, item), "a widened network must still store a resource");
    }

    @Test
    @DisplayName("insertion still reaches every index when the whole storage is taken")
    void insertionSpillsAcrossIndices() {
        BigItemHandler handler = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * 4;

        handler.insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);

        assertEquals(capacity, StorageFixtures.total(handler, item), "the whole capacity must fill");
        assertEquals(4, countPopulated(handler), "every index must be used");
        assertEquals(
            0L,
            handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 1L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a full storage must accept nothing more");
    }

    private static int indexOf(BigItemHandler handler, Item item) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigItemStack snapshot = handler.getSnapshot(index);
            if (snapshot.isSameType(new BigItemStack(StorageFixtures.one(item), 1L))) {
                return index;
            }
        }
        return -1;
    }

    private static int countPopulated(BigItemHandler handler) {
        int count = 0;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (handler.getSnapshot(index)
                .hasTemplate()) {
                count++;
            }
        }
        return count;
    }

    private static ItemStorageKey keyOf(Item item) {
        return new ItemStorageKey(new ItemStack(item, 1));
    }
}
