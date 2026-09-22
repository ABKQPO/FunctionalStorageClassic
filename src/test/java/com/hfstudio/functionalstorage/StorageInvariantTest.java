package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.FilteredItemStorage;
import com.hfstudio.functionalstorage.common.inventory.SelectedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Asserts the laws that must hold for every storage, on every resource kind.
 * <p>
 * Each law is stated once and then applied to items, fluids, aspects, a restricted
 * view, a filtered view, and an aggregate, because a defect in one kind is usually a
 * defect in the shared core that the other kinds merely fail to expose. The checks
 * concern what a caller is entitled to: that a simulation changes nothing, that a
 * reported amount matches what actually moved, that nothing is created or destroyed,
 * and that a restriction never lets a caller reach outside it.
 */
public class StorageInvariantTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("a simulation leaves every kind of storage untouched")
    void simulateIsPure() {
        Item item = StorageFixtures.newItem();
        BigItemHandler items = new BigItemHandler(4);
        items.insertRouted(new BigItemStack(StorageFixtures.one(item), 40L), StorageAction.EXECUTE);

        long itemBefore = StorageFixtures.total(items, item);
        for (int round = 0; round < 50; round++) {
            items.insertRouted(new BigItemStack(StorageFixtures.one(item), 1_000L), StorageAction.SIMULATE);
            items.extractRouted(new BigItemStack(StorageFixtures.one(item), 1_000L), StorageAction.SIMULATE);
            items.insertRouted(new BigItemStack(StorageFixtures.one(item), 7L), StorageAction.SIMULATE);
        }
        assertEquals(itemBefore, StorageFixtures.total(items, item), "a simulated item request must not move items");

        String fluidName = "pure_fluid";
        BigFluidHandler fluids = new BigFluidHandler(4);
        fluids.fillRouted(
            new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid(fluidName), 1), 500L),
            StorageAction.EXECUTE);
        long fluidBefore = StorageFixtures.fluidTotal(fluids, StorageFixtures.fluid(fluidName));
        for (int round = 0; round < 50; round++) {
            fluids.fillRouted(
                new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid(fluidName), 1), 9_000L),
                StorageAction.SIMULATE);
            fluids.drainRouted(9_000L, StorageAction.SIMULATE);
        }
        assertEquals(
            fluidBefore,
            StorageFixtures.fluidTotal(fluids, StorageFixtures.fluid(fluidName)),
            "a simulated fluid request must not move fluid");

        Aspect aspect = AspectBootstrap.firstAspect();
        BigAspectHandler aspects = new BigAspectHandler(4);
        aspects.insertRouted(new BigAspectStack(aspect, 30L), StorageAction.EXECUTE);
        long aspectBefore = AspectBootstrap.total(aspects, aspect);
        for (int round = 0; round < 50; round++) {
            aspects.insertRouted(new BigAspectStack(aspect, 5_000L), StorageAction.SIMULATE);
            aspects.extractRouted(new BigAspectStack(aspect, 5_000L), StorageAction.SIMULATE);
        }
        assertEquals(
            aspectBefore,
            AspectBootstrap.total(aspects, aspect),
            "a simulated aspect request must not move essentia");
    }

    @Test
    @DisplayName("a reported amount matches what actually moved")
    void reportedAmountMatchesMovement() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(4);

        long reported = handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 100L), StorageAction.EXECUTE)
            .getProcessedAmount();
        long stored = StorageFixtures.total(handler, item);
        assertEquals(reported, stored, "a plain insert must report exactly what it stored");

        long extractReported = handler
            .extractRouted(new BigItemStack(StorageFixtures.one(item), 40L), StorageAction.EXECUTE)
            .getProcessedAmount();
        long remaining = StorageFixtures.total(handler, item);
        assertEquals(extractReported, stored - remaining, "an extract must report exactly what it removed");
    }

    @Test
    @DisplayName("no kind of storage holds more than its capacity")
    void storedNeverExceedsCapacity() {
        Item item = StorageFixtures.newItem();
        BigItemHandler items = new BigItemHandler(4);
        items.insertRouted(new BigItemStack(StorageFixtures.one(item), Long.MAX_VALUE), StorageAction.EXECUTE);
        for (int index = 0; index < items.getStorageCount(); index++) {
            BigItemStack snapshot = items.getSnapshot(index);
            assertTrue(
                snapshot.getAmount() <= items.getCapacity(index),
                "index " + index + " holds " + snapshot.getAmount() + " over capacity " + items.getCapacity(index));
        }

        String fluidName = "over_fluid";
        BigFluidHandler fluids = new BigFluidHandler(4);
        fluids.fillRouted(
            new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid(fluidName), 1), Long.MAX_VALUE),
            StorageAction.EXECUTE);
        for (int index = 0; index < fluids.getStorageCount(); index++) {
            assertTrue(
                fluids.getSnapshot(index)
                    .getAmount() <= fluids.getCapacity(index),
                "fluid index " + index + " exceeds its capacity");
        }

        Aspect aspect = AspectBootstrap.firstAspect();
        BigAspectHandler aspects = new BigAspectHandler(4);
        aspects.insertRouted(new BigAspectStack(aspect, Long.MAX_VALUE), StorageAction.EXECUTE);
        for (int index = 0; index < aspects.getStorageCount(); index++) {
            assertTrue(
                aspects.getSnapshot(index)
                    .getAmount() <= aspects.getCapacity(index),
                "aspect index " + index + " exceeds its capacity");
        }
    }

    @Test
    @DisplayName("an aggregate holds exactly what its children hold")
    void aggregateIsAdditive() {
        Item item = StorageFixtures.newItem();
        List<IBigItemHandler> children = new ArrayList<>();
        long expected = 0L;
        for (int index = 0; index < 12; index++) {
            BigItemHandler child = new BigItemHandler(4);
            long amount = 7L * (index + 1);
            child.insertRouted(new BigItemStack(StorageFixtures.one(item), amount), StorageAction.EXECUTE);
            expected += amount;
            children.add(child);
        }

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        assertEquals(expected, StorageFixtures.total(network, item), "the aggregate must sum its children exactly");
        assertEquals(children.size() * 4, network.getStorageCount(), "the aggregate must span every child index");
    }

    @Test
    @DisplayName("draining the reported total empties the storage exactly")
    void drainingTotalEmptiesExactly() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(8);
        long stored = handler.insertRouted(new BigItemStack(StorageFixtures.one(item), 5_000L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertTrue(stored > 0L, "the fixture must store something");

        long drained = 0L;
        for (int round = 0; round < 100_000; round++) {
            TransferResult<BigItemStack, ItemStorageKey> result = handler
                .extractRouted(new BigItemStack(StorageFixtures.one(item), 1_000L), StorageAction.EXECUTE);
            drained += result.getProcessedAmount();
            if (result.getProcessedAmount() == 0L) {
                break;
            }
        }

        assertEquals(stored, drained, "draining must yield exactly what was stored, no more and no less");
        assertEquals(0L, StorageFixtures.total(handler, item), "an exhausted storage must hold nothing");
    }

    @Test
    @DisplayName("a restricted view never lets a caller reach an unselected index")
    void restrictedViewConfiniesAccess() {
        Item item = StorageFixtures.newItem();
        BigItemHandler backing = new BigItemHandler(4);
        // Give every index contents so an escape would be visible.
        for (int index = 0; index < 4; index++) {
            backing.insert(index, new BigItemStack(StorageFixtures.one(item), 100L), StorageAction.EXECUTE);
        }

        int[] allowed = { 1, 2 };
        SelectedStorage<BigItemStack, ItemStorageKey> view = new SelectedStorage<>(
            backing,
            allowed,
            BigItemStack.empty());

        // Every index is offered the same request, so an escape shows up as movement
        // on an index that was never selected.
        for (int index = 0; index < backing.getStorageCount(); index++) {
            view.insert(index, new BigItemStack(StorageFixtures.one(item), 500L), StorageAction.EXECUTE);
            view.extract(index, 1_000L, StorageAction.EXECUTE);
        }

        assertEquals(
            100L,
            backing.getSnapshot(0)
                .getAmount(),
            "index 0 is unselected and must never be touched");
        assertEquals(
            100L,
            backing.getSnapshot(3)
                .getAmount(),
            "index 3 is unselected and must never be touched");
        assertFalse(view.isLocked(0), "an unselected index must not report a lock");
        assertEquals(0L, view.getCapacity(0), "an unselected index must report no capacity");
        assertTrue(
            view.getSnapshot(0)
                .isEmpty(),
            "an unselected index must read as empty");
    }

    @Test
    @DisplayName("a filtered view admits only matching resources and loses nothing")
    void filteredViewAdmitsOnlyMatching() {
        Item allowed = StorageFixtures.newItem();
        Item blocked = StorageFixtures.newItem();
        BigItemHandler backing = new BigItemHandler(4);
        Predicate<Item> permitted = candidate -> candidate == allowed;

        FilteredItemStorage view = new FilteredItemStorage(
            backing,
            stack -> stack != null && permitted.test(stack.getItem()),
            null);

        long blockedProcessed = view
            .insertRouted(new BigItemStack(StorageFixtures.one(blocked), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(0L, blockedProcessed, "a non-matching resource must be refused");
        assertEquals(
            0L,
            StorageFixtures.total(backing, blocked),
            "a refused resource must not reach the backing store");

        long allowedProcessed = view
            .insertRouted(new BigItemStack(StorageFixtures.one(allowed), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(64L, allowedProcessed, "a matching resource must be accepted");
        assertEquals(64L, StorageFixtures.total(backing, allowed), "an accepted resource must be stored intact");
    }

    @Test
    @DisplayName("filtering out an index hides its contents without disturbing them")
    void filteringAnIndexIsReadOnly() {
        Item item = StorageFixtures.newItem();
        BigItemHandler backing = new BigItemHandler(4);
        backing.insert(2, new BigItemStack(StorageFixtures.one(item), 300L), StorageAction.EXECUTE);

        FilteredItemStorage view = new FilteredItemStorage(backing, stack -> true, new int[] { 0, 1 });

        assertTrue(
            view.getSnapshot(2)
                .isEmpty(),
            "an index outside the selection must read as empty through the view");
        assertEquals(
            300L,
            backing.getSnapshot(2)
                .getAmount(),
            "the hidden contents must remain in the backing store");
        assertFalse(view.isLocked(2), "an index outside the selection must not report a lock");
        assertEquals(0L, view.getCapacity(2), "an index outside the selection must report no capacity");
    }

    @Test
    @DisplayName("essentia suction never advertises an index that would refuse")
    void suctionNeverOverAdvertises() {
        Aspect aspect = AspectBootstrap.firstAspect();
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(2, locked);

        // Configure one index, then lock, so any suction offered must be of the
        // retained kind and must correspond to an index that truly accepts.
        handler.insertRouted(new BigAspectStack(aspect, 10L), StorageAction.EXECUTE);
        locked[0] = true;

        AspectSummary summary = handler.summary();
        if (summary.acceptsMore()) {
            assertTrue(
                handler.insertRouted(new BigAspectStack(aspect, 1L), StorageAction.SIMULATE)
                    .getProcessedAmount() > 0L,
                "the storage advertises suction but refuses the unit it invited");
        }
        assertTrue(summary.suctionAmount() >= 0, "suction must never be negative");
    }

    @Test
    @DisplayName("a request larger than the remaining room reports only what fits")
    void oversizedRequestReportsWhatFits() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(1);
        long capacity = handler.getCapacity(0);
        assertTrue(capacity > 0L, "the fixture must have capacity");

        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .insert(0, new BigItemStack(StorageFixtures.one(item), capacity + 1_000L), StorageAction.EXECUTE);

        assertEquals(capacity, result.getProcessedAmount(), "only the room available may be reported");
        assertTrue(result.getRemainingAmount() >= 1_000L, "the surplus must be reported as unprocessed");
        assertEquals(capacity, StorageFixtures.total(handler, item), "the storage must hold exactly its capacity");
    }

    @Test
    @DisplayName("fluid helpers agree with the routed operations they wrap")
    void fluidHelpersMatchRouted() {
        String fluidName = "agree_fluid";
        BigFluidHandler handler = new BigFluidHandler(4);
        BigFluidStack request = new BigFluidStack(
            StorageFixtures.fluidStack(StorageFixtures.fluid(fluidName), 1),
            777L);

        long routed = handler.fillRouted(request, StorageAction.SIMULATE)
            .getProcessedAmount();
        long helper = handler.fill(StorageFixtures.fluidStack(StorageFixtures.fluid(fluidName), 777), false);

        assertEquals(routed, helper, "the boolean helper must agree with its routed counterpart");
        assertEquals(
            0L,
            StorageFixtures.fluidTotal(handler, StorageFixtures.fluid(fluidName)),
            "a simulation must store nothing");
    }
}
