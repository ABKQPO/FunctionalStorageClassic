package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageViewCache;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Attacks every memo introduced by the read optimizations.
 * <p>
 * A memo that outlives the state it describes is worse than no memo at all: the same
 * request that used to be merely slow now answers wrongly, and a wrong answer to a
 * routing question can lose resources or refuse a legitimate deposit. Each check
 * therefore changes state through a route that does not fire the hook the memo relies
 * on, and asserts the very next read already sees the change.
 */
public class CacheInvalidationTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("a fluid memo notices a child drained with no listener attached")
    void fluidMemoNoticesSilentChildChange() {
        BigFluidHandler child = new BigFluidHandler(4);
        BigFluidStack request = new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid("memo"), 1), 10L);
        child.insert(0, request, StorageAction.EXECUTE);

        List<IBigFluidHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Fluids network = new AggregatedStorage.Fluids();
        network.rebuild(children);

        // Warm the memo, then drain the child directly, which is how a player or a
        // tube reaches it. Nothing about the aggregate is touched.
        assertEquals(0, network.firstPopulatedIndex(), "the populated index must be found");
        child.extract(0, 10L, StorageAction.EXECUTE);

        assertEquals(-1, network.firstPopulatedIndex(), "the memo must drop once the child is emptied");
        assertEquals(
            0L,
            network.drainRouted(10L, StorageAction.SIMULATE)
                .getProcessedAmount(),
            "an emptied network must offer nothing");
    }

    @Test
    @DisplayName("a fluid memo notices a child filled with no listener attached")
    void fluidMemoNoticesSilentChildFill() {
        BigFluidHandler child = new BigFluidHandler(4);
        List<IBigFluidHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Fluids network = new AggregatedStorage.Fluids();
        network.rebuild(children);

        assertEquals(-1, network.firstPopulatedIndex(), "an empty network must report no populated index");

        child.insert(
            2,
            new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid("late"), 1), 5L),
            StorageAction.EXECUTE);

        assertEquals(2, network.firstPopulatedIndex(), "the memo must see the newly filled index");
        assertEquals(
            5L,
            network.drainRouted(5L, StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a drain must find fluid added after the memo was warmed");
    }

    @Test
    @DisplayName("an essentia memo notices a child changed with no listener attached")
    void aspectMemoNoticesSilentChildChange() {
        BigAspectHandler child = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();

        List<IBigAspectHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Aspects network = new AggregatedStorage.Aspects();
        network.rebuild(children);

        assertEquals(
            0L,
            network.summary()
                .getTotal(aspect),
            "the memo must start empty");

        child.insertRouted(new BigAspectStack(aspect, 40L), StorageAction.EXECUTE);

        assertEquals(
            40L,
            network.summary()
                .getTotal(aspect),
            "the memo must see essentia added to the child");
    }

    @Test
    @DisplayName("an essentia memo notices a child drained at the moment it was already full")
    void aspectMemoNoticesFreedCapacity() {
        boolean[] locked = { false };
        BigAspectHandler child = AspectBootstrap.lockableHandler(1, locked);
        Aspect aspect = AspectBootstrap.firstAspect();
        long capacity = child.getCapacity(0);

        List<IBigAspectHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Aspects network = new AggregatedStorage.Aspects();
        network.rebuild(children);

        child.insertRouted(new BigAspectStack(aspect, capacity), StorageAction.EXECUTE);

        // With one index and a lock-free fill, the storage is full and must refuse. The
        // memo that records this is exactly the value the early exit trusts.
        locked[0] = true;
        assertFalse(
            network.summary()
                .acceptsMore(),
            "a full locked storage must invite nothing");
        assertEquals(
            0L,
            network.insertRouted(new BigAspectStack(aspect, 1L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a full storage must refuse another unit");

        // Freeing room must be honoured on the very next call, or the refusal sticks.
        child.extractRouted(new BigAspectStack(aspect, capacity / 2L), StorageAction.EXECUTE);

        assertTrue(
            network.summary()
                .acceptsMore(),
            "a storage with room must invite more");
        assertTrue(
            network.insertRouted(new BigAspectStack(aspect, 1L), StorageAction.SIMULATE)
                .getProcessedAmount() > 0L,
            "the early exit must not keep refusing after room was freed");
    }

    @Test
    @DisplayName("an item view memo notices a child changed with no listener attached")
    void itemViewMemoNoticesSilentChildChange() {
        BigItemHandler child = new BigItemHandler(4);
        Item item = StorageFixtures.newItem();

        List<IBigItemHandler> children = new ArrayList<>();
        children.add(child);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        StorageViewCache cache = network.getStorageViewCache();
        assertNotNull(cache, "the aggregate must expose a view memo");

        assertTrue(
            cache.views(network)
                .isEmpty(),
            "the memo must start with no views");

        child.insert(1, new BigItemStack(StorageFixtures.one(item), 25L), StorageAction.EXECUTE);

        assertFalse(
            cache.views(network)
                .isEmpty(),
            "the memo must see the new child contents");
        assertEquals(
            25L,
            cache.views(network)
                .get(0)
                .toItemStack().stackSize,
            "the memo must report the stored amount");
    }

    @Test
    @DisplayName("a rebuilt aggregate drops its memos instead of describing the old membership")
    void rebuildDropsEveryMemo() {
        BigFluidHandler first = new BigFluidHandler(4);
        BigFluidHandler second = new BigFluidHandler(4);
        BigAspectHandler aspects = new BigAspectHandler(4);

        // Give the replacement real contents, so the answer has to change rather than
        // merely staying "nothing" and passing for the wrong reason.
        second.insert(
            3,
            new BigFluidStack(StorageFixtures.fluidStack(StorageFixtures.fluid("swap"), 1), 42L),
            StorageAction.EXECUTE);

        List<IBigFluidHandler> fluids = new ArrayList<>();
        fluids.add(first);
        AggregatedStorage.Fluids network = new AggregatedStorage.Fluids();
        network.rebuild(fluids);
        assertEquals(-1, network.firstPopulatedIndex(), "the first network must report nothing populated");

        List<IBigFluidHandler> replaced = new ArrayList<>();
        replaced.add(second);
        assertTrue(network.rebuild(replaced), "swapping a child must rebuild");
        assertEquals(
            3,
            network.firstPopulatedIndex(),
            "a rebuild must describe the new membership rather than keep the old memo");

        AspectSummary cached = aspects.summary();
        assertSame(cached, aspects.summary(), "an unchanged handler must reuse its memo");

        aspects.insertRouted(new BigAspectStack(AspectBootstrap.firstAspect(), 7L), StorageAction.EXECUTE);

        assertEquals(
            7L,
            aspects.summary()
                .getTotal(AspectBootstrap.firstAspect()),
            "a single drawer's memo must notice its own change");
    }

    @Test
    @DisplayName("a lock transition that changes capacity drops the memo even with nothing stored")
    void lockTransitionDropsMemo() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(2, locked);
        assertTrue(
            handler.summary()
                .acceptsMore(),
            "an unlocked empty storage invites essentia");
        assertTrue(
            handler.summary()
                .suctionAmount() > 0,
            "an unlocked empty storage has suction");

        locked[0] = true;
        handler.applyLockConfiguration(true);

        assertFalse(
            handler.summary()
                .acceptsMore(),
            "a locked empty storage must invite nothing");
        assertEquals(
            0,
            handler.summary()
                .suctionAmount(),
            "a locked empty storage must have no suction");
    }
}
