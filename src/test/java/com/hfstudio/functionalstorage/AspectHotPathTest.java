package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Guards the cost of the essentia reads Thaumcraft repeats.
 *
 * <p>
 * A tube asks a container for its suction and its contents on a timer, for every
 * neighbouring side, and the container API is polled while a player watches a jar.
 * Those answers only change when essentia moves, which is far less often than they
 * are asked, so each read must stay cheap no matter how large the network behind it
 * grows. Walking the network per read is what made these calls cost hundreds of
 * microseconds on a linked controller.
 * </p>
 *
 * <p>
 * The check compares two network sizes rather than asserting an absolute time, so it
 * means the same thing on any machine: a memoized read barely changes with size,
 * while a scanning one grows with it.
 * </p>
 */
public class AspectHotPathTest {

    private static final int SLOTS_PER_DRAWER = 4;
    private static final int SMALL_DRAWERS = 16;
    private static final int LARGE_DRAWERS = 512;

    /** A memoized read stays near one; a scanning one grows past thirty. */
    private static final long MAX_GROWTH = 4L;

    @BeforeAll
    static void installVanillaState() {
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("suction and contents stay cheap as a linked network grows")
    void essentiaReadsDoNotGrowWithNetworkSize() {
        long smallSuction = suctionNanos(SMALL_DRAWERS);
        long largeSuction = suctionNanos(LARGE_DRAWERS);
        long smallContains = containsNanos(SMALL_DRAWERS);
        long largeContains = containsNanos(LARGE_DRAWERS);

        System.out.println(
            "MEASURE essentia suctionNs small=" + smallSuction
                + " (drawers="
                + SMALL_DRAWERS
                + ')'
                + " large="
                + largeSuction
                + " (drawers="
                + LARGE_DRAWERS
                + ')'
                + " containsNs small="
                + smallContains
                + " large="
                + largeContains);

        assertTrue(
            largeSuction <= Math.max(1L, smallSuction) * MAX_GROWTH,
            "suction cost grew " + growth(smallSuction, largeSuction)
                + "x from "
                + SMALL_DRAWERS
                + " to "
                + LARGE_DRAWERS
                + " drawers, so it is scanning the network");
        assertTrue(
            largeContains <= Math.max(1L, smallContains) * MAX_GROWTH,
            "contents cost grew " + growth(smallContains, largeContains)
                + "x from "
                + SMALL_DRAWERS
                + " to "
                + LARGE_DRAWERS
                + " drawers, so it is scanning the network");
    }

    @Test
    @DisplayName("a change is visible on the very next read")
    void memoReflectsChangesImmediately() {
        AggregatedStorage.Aspects network = network(LARGE_DRAWERS);
        Aspect aspect = AspectBootstrap.firstAspect();

        long before = network.summary()
            .getTotal(aspect);
        assertTrue(before > 0L, "the fixture must hold essentia before the change");

        // Drain through the aggregate, which is how a tube or the container API does
        // it, so the change arrives by the same route as in game.
        network.extractRouted(new BigAspectStack(aspect, 1_000_000L), StorageAction.EXECUTE);

        long after = network.summary()
            .getTotal(aspect);
        assertTrue(after < before, "a change must be visible on the next read, saw " + after + " from " + before);
    }

    private static long growth(long small, long large) {
        return large / Math.max(1L, small);
    }

    private static AggregatedStorage.Aspects network(int drawers) {
        List<IBigAspectHandler> children = new ArrayList<>(drawers);
        for (int index = 0; index < drawers; index++) {
            children.add(new BigAspectHandler(SLOTS_PER_DRAWER));
        }
        AggregatedStorage.Aspects network = new AggregatedStorage.Aspects();
        network.rebuild(children);

        Aspect aspect = AspectBootstrap.firstAspect();
        for (IBigAspectHandler child : children) {
            for (int slot = 0; slot < SLOTS_PER_DRAWER; slot++) {
                child.insert(slot, new BigAspectStack(aspect, 1_000_000L), StorageAction.EXECUTE);
            }
        }
        network.summary();
        return network;
    }

    private static long suctionNanos(int drawers) {
        AggregatedStorage.Aspects network = network(drawers);
        return time(
            () -> network.summary()
                .suctionAmount());
    }

    private static long containsNanos(int drawers) {
        AggregatedStorage.Aspects network = network(drawers);
        Aspect aspect = AspectBootstrap.firstAspect();
        return time(
            () -> network.summary()
                .getTotal(aspect));
    }

    private static long time(Runnable action) {
        for (int index = 0; index < 500; index++) {
            action.run();
        }
        int iterations = 20_000;
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
