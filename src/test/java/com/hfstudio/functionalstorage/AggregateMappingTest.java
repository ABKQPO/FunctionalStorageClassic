package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts that a controller addresses the same drawer index a drawer would.
 * <p>
 * A controller presents every linked drawer as one long run of indices, so each
 * aggregate index has to map back to one drawer and one index within it. Drawers may
 * expose different numbers of slots, which makes the mapping a running sum rather than
 * a fixed stride: a mapping that drifted would make a caller read one drawer while
 * writing another, so this pins each boundary with a distinct item per index.
 */
public class AggregateMappingTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("every aggregate index addresses the drawer and slot it should")
    void everyIndexMapsToItsOwner() {
        // Drawers of differing sizes, so a fixed stride would misalign immediately.
        int[] sizes = { 1, 4, 2, 3 };
        List<IBigItemHandler> children = new ArrayList<>();
        List<Item[]> markers = new ArrayList<>();

        for (int size : sizes) {
            BigItemHandler child = new BigItemHandler(size);
            Item[] items = new Item[size];
            for (int slot = 0; slot < size; slot++) {
                items[slot] = StorageFixtures.newItem();
                child.insert(slot, new BigItemStack(StorageFixtures.one(items[slot]), 10L), StorageAction.EXECUTE);
            }
            children.add(child);
            markers.add(items);
        }

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        int expectedTotal = 0;
        for (int size : sizes) {
            expectedTotal += size;
        }
        assertEquals(expectedTotal, network.getStorageCount(), "the aggregate must span every drawer slot");

        int aggregateIndex = 0;
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            Item[] items = markers.get(childIndex);
            for (int slot = 0; slot < items.length; slot++) {
                BigItemStack snapshot = network.getSnapshot(aggregateIndex);
                assertEquals(
                    items[slot],
                    snapshot.getKey()
                        .getItem(),
                    "aggregate index " + aggregateIndex + " must address drawer " + childIndex + " slot " + slot);
                assertEquals(10L, snapshot.getAmount(), "the mapped slot must report its own amount");
                assertEquals(
                    10L,
                    StorageFixtures.total(network, items[slot]),
                    "each marker item must be reachable through the aggregate exactly once");
                aggregateIndex++;
            }
        }
    }

    @Test
    @DisplayName("writing through the aggregate reaches only the mapped drawer")
    void writingThroughAggregateTouchesOneDrawer() {
        BigItemHandler first = new BigItemHandler(2);
        BigItemHandler second = new BigItemHandler(3);

        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item item = StorageFixtures.newItem();
        // Aggregate index 2 is the first slot of the second drawer.
        network.insert(2, new BigItemStack(StorageFixtures.one(item), 25L), StorageAction.EXECUTE);

        assertEquals(0L, StorageFixtures.total(first, item), "the first drawer must be untouched");
        assertEquals(25L, StorageFixtures.total(second, item), "the second drawer must hold the write");
        assertEquals(
            25L,
            second.getSnapshot(0)
                .getAmount(),
            "the write must land in the mapped slot");
        assertEquals(
            0L,
            second.getSnapshot(1)
                .getAmount(),
            "a neighbouring slot must stay empty");
    }

    @Test
    @DisplayName("an index past the end reads empty and refuses writes")
    void outOfRangeIndexIsHarmless() {
        BigItemHandler child = new BigItemHandler(2);
        List<IBigItemHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item item = StorageFixtures.newItem();

        assertTrue(
            network.getSnapshot(2)
                .isEmpty(),
            "an index past the end must read empty");
        assertTrue(
            network.getSnapshot(-1)
                .isEmpty(),
            "an index below zero must read empty");
        assertEquals(0L, network.getCapacity(2), "an index past the end must report no capacity");
        assertEquals(
            0L,
            network.insert(2, new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "an index past the end must accept nothing");
        assertEquals(0L, StorageFixtures.total(child, item), "a refused write must not reach any drawer");
    }

    @Test
    @DisplayName("an empty aggregate spans nothing and answers harmlessly")
    void emptyAggregateIsHarmless() {
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(List.of());

        Item item = StorageFixtures.newItem();
        assertEquals(0, network.getStorageCount(), "an aggregate with no children must span nothing");
        assertTrue(
            network.getSnapshot(0)
                .isEmpty(),
            "an empty aggregate must read empty");
        assertEquals(0L, network.getCapacity(0), "an empty aggregate must report no capacity");
        assertEquals(
            0L,
            network.insertRouted(new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "an empty aggregate must accept nothing");
    }

    @Test
    @DisplayName("the same drawer linked twice is still spanned once")
    void duplicateChildIsDeduplicated() {
        BigItemHandler child = new BigItemHandler(2);
        List<IBigItemHandler> children = new ArrayList<>();
        children.add(child);
        children.add(child);

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        assertEquals(2, network.getStorageCount(), "a drawer listed twice must be spanned once");
        Item item = StorageFixtures.newItem();
        network.insert(0, new BigItemStack(StorageFixtures.one(item), 7L), StorageAction.EXECUTE);
        assertEquals(7L, StorageFixtures.total(child, item), "the write must not be counted twice");
    }

    @Test
    @DisplayName("rebuilding with the same drawers keeps the index mapping")
    void rebuildKeepsMapping() {
        BigItemHandler first = new BigItemHandler(2);
        BigItemHandler second = new BigItemHandler(2);

        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item item = StorageFixtures.newItem();
        network.insert(3, new BigItemStack(StorageFixtures.one(item), 9L), StorageAction.EXECUTE);

        // The second drawer is unchanged, so rebuilding must report no change and keep
        // the same mapping rather than reshuffling indices.
        assertTrue(!network.rebuild(children), "an unchanged membership must not rebuild");
        assertEquals(
            9L,
            second.getSnapshot(1)
                .getAmount(),
            "the mapping must survive a no-op rebuild");
        assertEquals(9L, StorageFixtures.total(network, item), "the aggregate must still report the write");
    }

    @Test
    @DisplayName("growing a drawer extends the aggregate without disturbing earlier indices")
    void growingADrawerKeepsEarlierIndices() {
        BigItemHandler first = new BigItemHandler(2);
        BigItemHandler second = new BigItemHandler(2);

        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item marker = StorageFixtures.newItem();
        network.insert(0, new BigItemStack(StorageFixtures.one(marker), 5L), StorageAction.EXECUTE);

        // A drawer that reports more slots shifts every later index.
        BigItemHandler larger = new BigItemHandler(5);
        List<IBigItemHandler> replaced = new ArrayList<>();
        replaced.add(first);
        replaced.add(larger);
        network.rebuild(replaced);

        assertEquals(7, network.getStorageCount(), "the aggregate must span the new sizes");
        BigItemStack atZero = network.getSnapshot(0);
        assertSame(
            marker,
            atZero.getKey()
                .getItem(),
            "the first index must still address the first drawer's first slot");
        assertEquals(5L, atZero.getAmount(), "the earlier contents must be intact");
    }
}
