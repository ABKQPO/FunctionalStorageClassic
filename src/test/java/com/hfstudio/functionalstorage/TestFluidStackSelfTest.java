package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.support.StorageFixtures;

public class TestFluidStackSelfTest {

    @Test
    void identityMatchesForgeSemantics() {
        Fluid water = StorageFixtures.fluid("self_water");
        Fluid lava = StorageFixtures.fluid("self_lava");

        FluidStack waterA = StorageFixtures.fluidStack(water, 1000);
        FluidStack waterB = StorageFixtures.fluidStack(water, 7);
        FluidStack lavaStack = StorageFixtures.fluidStack(lava, 1000);

        assertEquals(water, waterA.getFluid(), "the fluid must survive");
        assertTrue(waterA.isFluidEqual(waterB), "the same fluid must compare equal regardless of amount");
        assertTrue(!waterA.isFluidEqual(lavaStack), "a different fluid must not compare equal");
    }

    @Test
    void copyIsDetached() {
        Fluid water = StorageFixtures.fluid("copy_water");
        FluidStack original = StorageFixtures.fluidStack(water, 500);
        FluidStack copy = original.copy();

        assertNotSame(original, copy, "a copy must be a different instance");
        assertEquals(500, copy.amount, "a copy must carry the amount");
        copy.amount = 999;
        assertEquals(500, original.amount, "mutating a copy must not touch the original");
    }

    @Test
    void bigFluidStackAcceptsLongAmounts() {
        Fluid water = StorageFixtures.fluid("long_water");
        BigFluidStack big = new BigFluidStack(StorageFixtures.fluidStack(water, 1), 5_000_000_000L);

        assertEquals(5_000_000_000L, big.getAmount(), "a long amount must survive");
        assertTrue(big.hasTemplate(), "a template must be retained");
        assertEquals(Integer.MAX_VALUE, big.toFluidStack().amount, "int conversion must saturate");
        assertEquals(
            water,
            big.getKey()
                .getFluid(),
            "the fluid identity must survive");
    }

    @Test
    void costIsLowEnoughForMillionsOfOperations() {
        Fluid water = StorageFixtures.fluid("perf_water");
        int iterations = 200_000;

        long start = System.nanoTime();
        for (int index = 0; index < iterations; index++) {
            StorageFixtures.fluidStack(water, index + 1);
        }
        long allocNanos = System.nanoTime() - start;

        start = System.nanoTime();
        for (int index = 0; index < iterations; index++) {
            new BigFluidStack(StorageFixtures.fluidStack(water, 1), index + 1);
        }
        long bigNanos = System.nanoTime() - start;

        System.out.println("MEASURE fixture stack ns/op = " + (allocNanos / iterations));
        System.out.println("MEASURE BigFluidStack ns/op = " + (bigNanos / iterations));
        assertTrue(
            allocNanos / iterations < 5_000L,
            "fixture allocation must stay far below the code under test, was " + (allocNanos / iterations) + " ns");
    }
}
