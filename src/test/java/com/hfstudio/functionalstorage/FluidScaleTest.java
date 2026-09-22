package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.TestFluidStack;

/**
 * Long-capacity fluid storage driven through Forge's int-counted fluid surface.
 *
 * <p>
 * A fluid drawer holds far more than an int can express, while every pipe and
 * machine in the game speaks millibuckets as an int. These checks hold a thousand
 * distinct fluids and drive millions of those int-sized interactions, asserting
 * that the long total moves by exactly the reported int amount each time. A
 * mismatch means fluid was created or destroyed at the boundary.
 * </p>
 *
 * <p>
 * Stacks are reused and only their amount is changed between calls. The storage
 * core copies whatever template it is handed, so this is what a real caller does,
 * and it keeps the measurement on the code under test instead of on the harness.
 * </p>
 */
public class FluidScaleTest {

    private static final int FLUID_KINDS = 1000;
    private static final int TANKS = 64;
    private static final ForgeDirection SIDE = ForgeDirection.UP;

    @Test
    @DisplayName("a thousand fluids coexist, each in its own tank, without identity bleed")
    void thousandFluidsStayDistinct() {
        BigFluidHandler handler = StorageFixtures.fluidHandler(FLUID_KINDS);
        DrawerFluidHandler forge = new DrawerFluidHandler(handler);
        Fluid[] fluids = StorageFixtures.fluids(FLUID_KINDS);
        TestFluidStack[] requests = reusableRequests(fluids);

        for (int index = 0; index < FLUID_KINDS; index++) {
            requests[index].amount = 1000 + index;
            int filled = forge.fill(SIDE, requests[index], true);
            assertEquals(1000 + index, filled, "tank " + index + " must accept its whole request");
        }

        for (int index = 0; index < FLUID_KINDS; index++) {
            assertEquals(
                1000 + index,
                StorageFixtures.fluidTotal(handler, fluids[index]),
                "fluid " + index + " is not stored under its own identity");
        }

        long expected = 0L;
        for (int index = 0; index < FLUID_KINDS; index++) {
            expected += 1000 + index;
        }
        assertEquals(expected, StorageFixtures.fluidGrandTotal(handler), "the grand total must match every fill");
    }

    @Test
    @DisplayName("long capacity past the int boundary saturates on read but never truncates on write")
    void longCapacitySaturatesWithoutLosingFluid() {
        double multiplier = StorageFixtures.intOverflowingMultiplier(StorageFixtures.fluidCapacity(), 8D);
        BigFluidHandler handler = StorageFixtures.largeFluidHandler(1, multiplier);
        DrawerFluidHandler forge = new DrawerFluidHandler(handler);
        Fluid fluid = StorageFixtures.fluid("bulk");

        long capacity = handler.getCapacity(0);
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must exceed the int boundary, was " + capacity);

        // Forge reports the room as a saturated int, and a caller may then hand back
        // exactly that saturated figure. Neither may lose or invent fluid.
        int reported = forge.getTankInfo(SIDE)[0].capacity;
        assertEquals(Integer.MAX_VALUE, reported, "capacity beyond the int boundary must saturate, not wrap");

        TestFluidStack request = StorageFixtures.reusableFluidStack(fluid);
        request.amount = reported;
        assertEquals(Integer.MAX_VALUE, forge.fill(SIDE, request, true), "the fill must report the saturated amount");
        assertEquals(
            Integer.MAX_VALUE,
            StorageFixtures.fluidTotal(handler, fluid),
            "stored amount must match the fill");

        FluidStack drained = forge.drain(SIDE, Integer.MAX_VALUE, true);
        assertEquals(Integer.MAX_VALUE, drained.amount, "the drain must return what was stored");
        assertEquals(0L, StorageFixtures.fluidGrandTotal(handler), "a full drain must empty the tank");
    }

    @Test
    @DisplayName("three million int-sized fluid interactions conserve the long total")
    void millionsOfInteractionsConserveFluid() {
        int interactions = 3_000_000;
        BigFluidHandler handler = StorageFixtures.largeFluidHandler(TANKS, 200_000D);
        DrawerFluidHandler forge = new DrawerFluidHandler(handler);
        Fluid[] fluids = StorageFixtures.fluids(FLUID_KINDS);
        TestFluidStack[] requests = reusableRequests(fluids);

        long capacity = handler.getCapacity(0) * TANKS;
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must hold more than an int can express");

        Random random = new Random(20260922L);
        long expected = 0L;
        long filled = 0L;
        long drainedTotal = 0L;

        long start = System.nanoTime();
        for (int step = 0; step < interactions; step++) {
            TestFluidStack request = requests[random.nextInt(FLUID_KINDS)];
            request.amount = 1 + random.nextInt(100_000);

            if (random.nextBoolean()) {
                int accepted = forge.fill(SIDE, request, true);
                expected += accepted;
                filled++;
            } else {
                FluidStack drained = forge.drain(SIDE, request, true);
                expected -= drained == null ? 0 : drained.amount;
                drainedTotal++;
            }

            if (step % 500_000 == 0) {
                assertEquals(
                    expected,
                    StorageFixtures.fluidGrandTotal(handler),
                    "the grand total diverged at interaction " + step);
            }
        }
        long elapsed = System.nanoTime() - start;

        assertEquals(
            expected,
            StorageFixtures.fluidGrandTotal(handler),
            "the grand total diverged after " + interactions + " interactions");
        assertTrue(expected > 0L, "the run must end with fluid still stored");

        System.out.println(
            "MEASURE fluid interactions=" + interactions
                + " elapsedMs="
                + (elapsed / 1_000_000L)
                + " nsPerInteraction="
                + (elapsed / interactions)
                + " kinds="
                + FLUID_KINDS
                + " tanks="
                + TANKS
                + " fills="
                + filled
                + " drains="
                + drainedTotal
                + " storedMb="
                + StorageFixtures.fluidGrandTotal(handler)
                + " capacityMb="
                + capacity);
    }

    @Test
    @DisplayName("simulation never mutates, even across many fluid kinds")
    void simulationNeverMutatesFluid() {
        BigFluidHandler handler = StorageFixtures.largeFluidHandler(TANKS, 100_000D);
        DrawerFluidHandler forge = new DrawerFluidHandler(handler);
        Fluid[] fluids = StorageFixtures.fluids(FLUID_KINDS);
        TestFluidStack[] requests = reusableRequests(fluids);

        Random random = new Random(777L);
        long expected = 0L;

        for (int step = 0; step < 500_000; step++) {
            TestFluidStack request = requests[random.nextInt(FLUID_KINDS)];
            request.amount = 1 + random.nextInt(50_000);
            boolean doAction = step % 3 != 0;

            if (step % 2 == 0) {
                int accepted = forge.fill(SIDE, request, doAction);
                if (doAction) {
                    expected += accepted;
                }
            } else {
                FluidStack drained = forge.drain(SIDE, request, doAction);
                if (doAction) {
                    expected -= drained == null ? 0 : drained.amount;
                }
            }
        }

        assertEquals(expected, StorageFixtures.fluidGrandTotal(handler), "a simulated call changed stored fluid");
    }

    @Test
    @DisplayName("routed long requests are exact, not truncated to an int")
    void routedLongRequestKeepsItsFullAmount() {
        BigFluidHandler handler = StorageFixtures.largeFluidHandler(4, 500_000D);
        Fluid fluid = StorageFixtures.fluid("huge");

        long capacity = handler.getCapacity(0) * 4;
        TestFluidStack template = StorageFixtures.reusableFluidStack(fluid);

        TransferResult<BigFluidStack, ?> result = handler
            .fillRouted(new BigFluidStack(template, capacity), StorageAction.EXECUTE);
        assertEquals(capacity, result.getProcessedAmount(), "a long request must be served in full, not truncated");
        assertEquals(capacity, StorageFixtures.fluidGrandTotal(handler), "every tank must be filled to capacity");

        TransferResult<BigFluidStack, ?> drained = handler.drainRouted(capacity, StorageAction.EXECUTE);
        assertEquals(capacity, drained.getProcessedAmount(), "the whole long amount must drain back out");
        assertEquals(0L, StorageFixtures.fluidGrandTotal(handler), "draining everything must leave nothing");
    }

    @Test
    @DisplayName("draining one fluid never disturbs the other thousand")
    void drainingOneFluidLeavesOthersIntact() {
        BigFluidHandler handler = StorageFixtures.fluidHandler(FLUID_KINDS);
        DrawerFluidHandler forge = new DrawerFluidHandler(handler);
        Fluid[] fluids = StorageFixtures.fluids(FLUID_KINDS);
        TestFluidStack[] requests = reusableRequests(fluids);

        for (int index = 0; index < FLUID_KINDS; index++) {
            requests[index].amount = 500 + index;
            forge.fill(SIDE, requests[index], true);
        }

        int victim = 250;
        int victimStored = 500 + victim;
        requests[victim].amount = 400;
        FluidStack drained = forge.drain(SIDE, requests[victim], true);
        assertEquals(400, drained.amount, "the victim must drain what was asked");
        assertEquals(
            victimStored - 400,
            StorageFixtures.fluidTotal(handler, fluids[victim]),
            "the victim must hold the remainder");

        for (int index = 0; index < FLUID_KINDS; index++) {
            if (index == victim) {
                continue;
            }
            assertEquals(
                500 + index,
                StorageFixtures.fluidTotal(handler, fluids[index]),
                "draining fluid " + victim + " disturbed fluid " + index);
        }
    }

    private static TestFluidStack[] reusableRequests(Fluid[] fluids) {
        TestFluidStack[] requests = new TestFluidStack[fluids.length];
        for (int index = 0; index < fluids.length; index++) {
            requests[index] = StorageFixtures.reusableFluidStack(fluids[index]);
        }
        return requests;
    }
}
