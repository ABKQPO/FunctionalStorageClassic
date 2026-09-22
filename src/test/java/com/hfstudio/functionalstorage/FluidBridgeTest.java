package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts that the fluid bridge never reports more than it moved.
 * <p>
 * Forge's fluid interface speaks in ints while a tank here holds a long, so every
 * answer is a conversion between the two. A conversion that saturated the wrong way
 * would tell a pipe that thousands of buckets had moved when none had, and the pipe
 * would then treat its own tank as full. Each check compares the reported figure
 * against what the tank actually holds afterwards.
 */
public class FluidBridgeTest {

    private static final ForgeDirection SIDE = ForgeDirection.UP;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("a fill reports exactly what the tank gained")
    void fillReportsWhatTheTankGained() {
        Fluid fluid = StorageFixtures.fluid("bridge_fill");
        BigFluidHandler handler = new BigFluidHandler(4);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        long before = StorageFixtures.fluidTotal(handler, fluid);
        int reported = bridge.fill(SIDE, StorageFixtures.fluidStack(fluid, 1000), true);
        long after = StorageFixtures.fluidTotal(handler, fluid);

        assertEquals(1000, reported, "a fill that succeeds must report the whole request");
        assertEquals(1000L, after - before, "the tank must have gained exactly what was reported");
    }

    @Test
    @DisplayName("a simulated fill reports what would move and moves nothing")
    void simulatedFillMovesNothing() {
        Fluid fluid = StorageFixtures.fluid("bridge_sim");
        BigFluidHandler handler = new BigFluidHandler(4);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        int reported = bridge.fill(SIDE, StorageFixtures.fluidStack(fluid, 500), false);

        assertEquals(500, reported, "a simulation must predict the same amount it would store");
        assertEquals(0L, StorageFixtures.fluidTotal(handler, fluid), "a simulation must store nothing");
    }

    @Test
    @DisplayName("a fill reported as an int is clamped, never wrapped negative")
    void fillIsNeverNegative() {
        Fluid fluid = StorageFixtures.fluid("bridge_clamp");
        // A tank far beyond the int boundary, so a naive cast would wrap.
        BigFluidHandler handler = StorageFixtures
            .largeFluidHandler(4, StorageFixtures.intOverflowingMultiplier(StorageFixtures.fluidCapacity(), 4D));
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        assertTrue(
            handler.getCapacity(0) > Integer.MAX_VALUE,
            "the fixture must exceed the int boundary, was " + handler.getCapacity(0));

        int reported = bridge.fill(SIDE, StorageFixtures.fluidStack(fluid, Integer.MAX_VALUE), false);

        assertTrue(reported >= 0, "a reported amount must never wrap to a negative value, was " + reported);
        assertEquals(Integer.MAX_VALUE, reported, "a tank with room for everything must report the whole request");
    }

    @Test
    @DisplayName("a drain reports exactly what the tank lost")
    void drainReportsWhatTheTankLost() {
        Fluid fluid = StorageFixtures.fluid("bridge_drain");
        BigFluidHandler handler = new BigFluidHandler(4);
        handler.fillRouted(new BigFluidStack(StorageFixtures.fluidStack(fluid, 1), 3000L), StorageAction.EXECUTE);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        long before = StorageFixtures.fluidTotal(handler, fluid);
        FluidStack drained = bridge.drain(SIDE, 1000, true);
        long after = StorageFixtures.fluidTotal(handler, fluid);

        assertNotNull(drained, "a populated tank must yield fluid");
        assertEquals(1000, drained.amount, "the drain must report the requested amount");
        assertEquals(1000L, before - after, "the tank must have lost exactly what was reported");
    }

    @Test
    @DisplayName("draining more than is stored reports only what was there")
    void drainBeyondContentsReportsWhatWasThere() {
        Fluid fluid = StorageFixtures.fluid("bridge_short");
        BigFluidHandler handler = new BigFluidHandler(4);
        handler.fillRouted(new BigFluidStack(StorageFixtures.fluidStack(fluid, 1), 250L), StorageAction.EXECUTE);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        FluidStack drained = bridge.drain(SIDE, 5_000, true);

        assertNotNull(drained, "a partially filled tank must still yield what it holds");
        assertEquals(250, drained.amount, "the drain must be limited to the stored amount");
        assertEquals(0L, StorageFixtures.fluidTotal(handler, fluid), "the tank must be empty afterwards");
        assertNull(bridge.drain(SIDE, 1, true), "an empty tank must yield nothing");
    }

    @Test
    @DisplayName("a typed drain of a fluid the tank does not hold yields nothing")
    void typedDrainOfForeignFluidYieldsNothing() {
        Fluid stored = StorageFixtures.fluid("bridge_stored");
        Fluid foreign = StorageFixtures.fluid("bridge_foreign");
        BigFluidHandler handler = new BigFluidHandler(4);
        handler.fillRouted(new BigFluidStack(StorageFixtures.fluidStack(stored, 1), 500L), StorageAction.EXECUTE);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        assertNull(
            bridge.drain(SIDE, StorageFixtures.fluidStack(foreign, 100), true),
            "a foreign fluid must not be drained from this tank");
        assertEquals(500L, StorageFixtures.fluidTotal(handler, stored), "the stored fluid must be untouched");
    }

    @Test
    @DisplayName("a fill of a second fluid does not disturb the first")
    void secondFluidDoesNotDisturbTheFirst() {
        Fluid first = StorageFixtures.fluid("bridge_first");
        Fluid second = StorageFixtures.fluid("bridge_second");
        BigFluidHandler handler = new BigFluidHandler(4);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        bridge.fill(SIDE, StorageFixtures.fluidStack(first, 400), true);
        bridge.fill(SIDE, StorageFixtures.fluidStack(second, 600), true);

        assertEquals(400L, StorageFixtures.fluidTotal(handler, first), "the first fluid must be intact");
        assertEquals(600L, StorageFixtures.fluidTotal(handler, second), "the second fluid must be stored");
    }

    @Test
    @DisplayName("tank info reports every tank with its own contents and capacity")
    void tankInfoReportsEveryTank() {
        Fluid fluid = StorageFixtures.fluid("bridge_info");
        BigFluidHandler handler = new BigFluidHandler(4);
        handler.fillRouted(new BigFluidStack(StorageFixtures.fluidStack(fluid, 1), 700L), StorageAction.EXECUTE);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        var info = bridge.getTankInfo(SIDE);
        assertEquals(4, info.length, "every tank must be reported");

        long totalReported = 0L;
        for (var tank : info) {
            assertNotNull(tank, "a reported tank must not be null");
            assertTrue(tank.capacity >= 0, "a reported capacity must never be negative");
            if (tank.fluid != null) {
                totalReported += tank.fluid.amount;
            }
        }
        assertEquals(
            700L,
            totalReported,
            "the reported contents must sum to what is stored, so a caller sees the whole tank set");
    }

    @Test
    @DisplayName("a request for nothing is refused cleanly")
    void emptyRequestIsRefused() {
        Fluid fluid = StorageFixtures.fluid("bridge_empty");
        BigFluidHandler handler = new BigFluidHandler(4);
        DrawerFluidHandler bridge = new DrawerFluidHandler(handler);

        assertEquals(0, bridge.fill(SIDE, null, true), "a null request must fill nothing");
        assertNull(bridge.drain(SIDE, null, true), "a null request must drain nothing");
        assertNull(bridge.drain(SIDE, 0, true), "a zero drain must yield nothing");
    }
}
