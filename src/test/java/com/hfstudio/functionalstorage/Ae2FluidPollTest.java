package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Guards the cost of one AE2 fluid storage bus poll.
 *
 * <p>
 * The bus asks for the tank list and then asks {@code drain} for a single unit once
 * per tank it was just told about. Since it repeats that on a timer, an untyped drain
 * that walks from index zero makes one poll cost the square of the tank count, and an
 * empty network is the worst case because no index can answer and every request then
 * walks the whole array.
 * </p>
 *
 * <p>
 * The bus itself decides how many calls a poll makes, so the figure that matters is
 * the cost per tank rather than per poll: a poll over a larger network is expected to
 * take longer simply because it asks more questions, while a per-tank cost that grows
 * with the network is the quadratic shape this guards against.
 * </p>
 */
public class Ae2FluidPollTest {

    private static final ForgeDirection SIDE = ForgeDirection.UP;
    private static final int PER_DRAWER = 4;
    private static final int SMALL_DRAWERS = 16;
    private static final int LARGE_DRAWERS = 512;

    /** A memoized drain stays near one per tank; a scanning one grows past thirty. */
    private static final long MAX_GROWTH = 8L;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("an empty network answers a whole bus poll without walking its tanks")
    void emptyPollDoesNotScan() {
        long small = perTankNanos(SMALL_DRAWERS, false);
        long large = perTankNanos(LARGE_DRAWERS, false);

        System.out.println(
            "MEASURE busPoll emptyNsPerTank small=" + small
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
            "per-tank cost of an empty poll grew " + growth(small, large)
                + "x from "
                + SMALL_DRAWERS
                + " to "
                + LARGE_DRAWERS
                + " drawers, so each tank request is walking the network");
    }

    @Test
    @DisplayName("a poll against a full network stays flat per tank as the network grows")
    void filledPollStaysFlat() {
        long small = perTankNanos(SMALL_DRAWERS, true);
        long large = perTankNanos(LARGE_DRAWERS, true);

        System.out.println(
            "MEASURE busPoll filledNsPerTank small=" + small
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
            "per-tank cost of a filled poll grew " + growth(small, large)
                + "x from "
                + SMALL_DRAWERS
                + " to "
                + LARGE_DRAWERS
                + " drawers, so an untyped drain is scanning the network");
    }

    @Test
    @DisplayName("an untyped drain still finds the tank that holds fluid")
    void untypedDrainStillFindsFluid() {
        Fluid fluid = StorageFixtures.fluid("untyped");
        AggregatedStorage.Fluids network = network(8, PER_DRAWER, fluid, true);
        DrawerFluidHandler forge = new DrawerFluidHandler(network);

        assertTrue(forge.drain(SIDE, 1, false) != null, "a populated network must report fluid");
        assertTrue(forge.drain(SIDE, 1, true) != null, "a populated network must surrender fluid");
        assertTrue(
            forge.drain(SIDE, 1, false) != null,
            "the network must still hold fluid after draining a single unit");
    }

    @Test
    @DisplayName("an untyped drain reports nothing once the network is empty")
    void untypedDrainReportsNothingWhenEmpty() {
        Fluid fluid = StorageFixtures.fluid("drained");
        AggregatedStorage.Fluids network = network(8, PER_DRAWER, fluid, true);
        DrawerFluidHandler forge = new DrawerFluidHandler(network);

        long total = 0L;
        for (int index = 0; index < network.getStorageCount(); index++) {
            total += network.getSnapshot(index)
                .getAmount();
        }
        assertTrue(total > 0L, "the fixture must hold fluid before it is drained");

        long drained = 0L;
        for (int round = 0; round < 10_000; round++) {
            TransferResult<BigFluidStack, FluidStorageKey> result = network
                .drainRouted(1_000_000L, StorageAction.EXECUTE);
            drained += result.getProcessedAmount();
            if (result.getProcessedAmount() == 0L) {
                break;
            }
        }

        assertTrue(drained <= total, "a drain must never produce more than was stored");
        assertTrue(forge.drain(SIDE, 1, false) == null, "an emptied network must report nothing to drain");
    }

    private static long growth(long small, long large) {
        return large / Math.max(1L, small);
    }

    private static AggregatedStorage.Fluids network(int drawers, int perDrawer, Fluid fluid, boolean fill) {
        List<IBigFluidHandler> children = new ArrayList<>(drawers);
        for (int index = 0; index < drawers; index++) {
            BigFluidHandler child = new BigFluidHandler(perDrawer);
            if (fill) {
                for (int slot = 0; slot < perDrawer; slot++) {
                    child.insert(
                        slot,
                        new BigFluidStack(StorageFixtures.fluidStack(fluid, 1), 500L),
                        StorageAction.EXECUTE);
                }
            }
            children.add(child);
        }
        AggregatedStorage.Fluids network = new AggregatedStorage.Fluids();
        network.rebuild(children);
        return network;
    }

    /**
     * Measures one whole poll and divides it by the number of tanks reported.
     *
     * @param drawers how many drawers the network spans
     * @param fill    whether every tank starts populated
     * @return nanoseconds per reported tank
     */
    private static long perTankNanos(int drawers, boolean fill) {
        DrawerFluidHandler forge = new DrawerFluidHandler(
            network(drawers, PER_DRAWER, StorageFixtures.fluid("poll_" + drawers + '_' + fill), fill));
        int tanks = Math.max(1, drawers * PER_DRAWER);
        return time(() -> poll(forge)) / tanks;
    }

    /**
     * Reproduces one poll of AE2's fluid monitor.
     *
     * @param forge handler under test
     */
    private static void poll(DrawerFluidHandler forge) {
        int reported = forge.getTankInfo(SIDE).length;
        int extractable = 0;
        for (int index = 0; index < reported; index++) {
            if (forge.drain(SIDE, 1, false) != null) {
                extractable++;
            }
        }
        if (extractable < 0) {
            throw new IllegalStateException("unreachable " + extractable);
        }
    }

    private static long time(Runnable action) {
        for (int index = 0; index < 20; index++) {
            action.run();
        }
        int iterations = 200;
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
