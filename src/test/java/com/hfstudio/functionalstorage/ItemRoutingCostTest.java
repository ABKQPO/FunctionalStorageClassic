package com.hfstudio.functionalstorage;

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
 * Guards the cost of asking a controller for a resource it does not hold.
 *
 * <p>
 * An external caller asks whether a drawer network already knows a resource before
 * depositing it, and asks for the resource itself when extracting. Both questions have
 * to walk the indices to answer, and a network that does not hold the resource is the
 * common case: an unfamiliar stack arrives, is refused, and arrives again. Since a
 * storage that never widens matching can only accept an exact match, those walks skip
 * the compatibility probe that could never succeed, and this checks that the per-index
 * cost does not grow with the network.
 * </p>
 */
public class ItemRoutingCostTest {

    private static final int PER_DRAWER = 4;
    private static final int SMALL_DRAWERS = 16;
    private static final int LARGE_DRAWERS = 512;

    /** A flat walk stays near one per index; a probing one grows several times over. */
    private static final long MAX_GROWTH = 4L;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("refusing an unknown resource stays linear in the index count")
    void unknownResourceRefusalStaysLinear() {
        long small = perIndexNanos(SMALL_DRAWERS);
        long large = perIndexNanos(LARGE_DRAWERS);

        System.out.println(
            "MEASURE itemRouting unknownNsPerIndex small=" + small
                + " (drawers="
                + SMALL_DRAWERS
                + ')'
                + " large="
                + large
                + " (drawers="
                + LARGE_DRAWERS
                + ')');

        assertTrue(
            large <= Math.max(1L, small) * MAX_GROWTH,
            "per-index cost grew " + (large / Math.max(1L, small))
                + "x from "
                + SMALL_DRAWERS
                + " to "
                + LARGE_DRAWERS
                + " drawers, so refusing an unknown resource is probing every occupied index");
    }

    /**
     * Measures the two questions an external caller asks about an unknown resource and
     * divides by the number of indices walked.
     */
    private static long perIndexNanos(int drawers) {
        Item stored = StorageFixtures.newItem();
        Item absent = StorageFixtures.newItem();

        List<IBigItemHandler> children = new ArrayList<>(drawers);
        for (int index = 0; index < drawers; index++) {
            BigItemHandler child = new BigItemHandler(PER_DRAWER);
            for (int slot = 0; slot < PER_DRAWER; slot++) {
                child.insert(slot, new BigItemStack(StorageFixtures.one(stored), 500L), StorageAction.EXECUTE);
            }
            children.add(child);
        }

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);
        int indices = Math.max(1, network.getStorageCount());

        BigItemStack unknown = new BigItemStack(StorageFixtures.one(absent), 64L);

        return time(() -> {
            boolean matched = network.hasMatchingResource(unknown);
            network.insertRouted(unknown, StorageAction.SIMULATE);
            network.extractRouted(unknown, StorageAction.SIMULATE);
            if (matched) {
                throw new IllegalStateException("an unknown resource must not be reported as known");
            }
        }) / indices;
    }

    private static long time(Runnable action) {
        for (int index = 0; index < 50; index++) {
            action.run();
        }
        int iterations = 500;
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 3; round++) {
            long start = System.nanoTime();
            for (int index = 0; index < iterations; index++) {
                action.run();
            }
            best = Math.min(best, (System.nanoTime() - start) / iterations);
        }
        return best;
    }
}
