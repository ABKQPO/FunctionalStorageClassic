package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.CompactingItemHandler;
import com.hfstudio.functionalstorage.common.storage.CompactingTier;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts that compression tiers are lossless views of one shared amount.
 * <p>
 * A compacting drawer shows a block, the ingots it equals, and the nuggets those
 * equal, all backed by a single count of the lowest tier. That design is only sound
 * while exchanging one view for another changes nothing: storing a block and then
 * reading nuggets must report exactly the nuggets that block is worth, and taking
 * them back out must leave the drawer exactly where it started. A rounding mistake in
 * either direction silently creates or destroys items, so each exchange is measured
 * against the ratio rather than against a remembered number.
 */
public class CompactingInvariantTest {

    /** Nuggets per ingot, and ingots per block, so a block is nine ingots. */
    private static final long LOWER_PER_MIDDLE = 9L;

    private static final long MIDDLE_PER_HIGHER = 9L;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("storing in a high tier reports the exact lower-tier equivalent")
    void storingHighTierReportsLowerEquivalent() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);
        long blockUnits = unitsOf(handler, 0);
        assertEquals(
            LOWER_PER_MIDDLE * MIDDLE_PER_HIGHER,
            blockUnits,
            "a block must be worth the full product of the tiers below it");

        long stored = handler.insert(0, new BigItemStack(StorageFixtures.one(block), 1L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(1L, stored, "one block must be stored");

        assertEquals(
            1L,
            handler.getSnapshot(0)
                .getAmount(),
            "the block view must report one block");
        assertEquals(
            LOWER_PER_MIDDLE,
            handler.getSnapshot(1)
                .getAmount(),
            "the ingot view must report exactly the ingots the block is worth");
        assertEquals(
            blockUnits,
            handler.getSnapshot(2)
                .getAmount(),
            "the nugget view must report exactly the nuggets the block is worth");
    }

    @Test
    @DisplayName("a fractional view truncates and never rounds up")
    void fractionalViewTruncates() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);

        // Five nuggets cannot make an ingot, so the middle view must read zero while
        // the lowest view reports all five.
        handler.insert(2, new BigItemStack(StorageFixtures.one(nugget), 5L), StorageAction.EXECUTE);

        assertEquals(
            5L,
            handler.getSnapshot(2)
                .getAmount(),
            "the nugget view must report all five");
        assertEquals(
            0L,
            handler.getSnapshot(1)
                .getAmount(),
            "five nuggets are not one ingot and must read zero");
        assertEquals(
            5L,
            handler.getStoredBaseAmount(),
            "the shared amount must still count all five nuggets, so nothing is lost to truncation");
    }

    @Test
    @DisplayName("exchanging one view for another conserves the shared amount")
    void exchangingViewsConserves() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);

        long stored = handler.insert(0, new BigItemStack(StorageFixtures.one(block), 2L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(2L, stored, "the fixture must store two blocks");
        long afterInsert = handler.getStoredBaseAmount();

        // Take the ingots back out, which must consume exactly the same shared amount.
        long extracted = handler.extract(1, LOWER_PER_MIDDLE * 2L, StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(LOWER_PER_MIDDLE * 2L, extracted, "the ingot view must surrender both blocks' worth");
        assertEquals(
            afterInsert - extracted * LOWER_PER_MIDDLE,
            handler.getStoredBaseAmount(),
            "removing through a view must consume the shared amount by the view's own ratio");
    }

    @Test
    @DisplayName("taking everything out through the lowest view empties the drawer exactly")
    void drainingThroughLowestViewEmptiesExactly() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);
        handler.insert(0, new BigItemStack(StorageFixtures.one(block), 1L), StorageAction.EXECUTE);

        long total = unitsOf(handler, 0);
        long drained = 0L;
        for (int round = 0; round < 1_000; round++) {
            long processed = handler.extract(2, total, StorageAction.EXECUTE)
                .getProcessedAmount();
            drained += processed;
            if (processed == 0L) {
                break;
            }
        }

        assertEquals(total, drained, "draining the lowest view must yield exactly the stored amount");
        assertEquals(0L, handler.getStoredBaseAmount(), "an exhausted drawer must hold nothing");
        assertEquals(
            0L,
            handler.getSnapshot(0)
                .getAmount(),
            "every view must read zero once emptied");
    }

    @Test
    @DisplayName("the shared amount never exceeds the shared capacity")
    void sharedAmountNeverExceedsCapacity() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);
        long capacity = handler.getTotalBaseCapacity();
        assertTrue(capacity > 0L, "the fixture must have capacity");

        // Ask for far more than fits in every tier at once.
        handler.insert(0, new BigItemStack(StorageFixtures.one(block), capacity), StorageAction.EXECUTE);
        assertTrue(
            handler.getStoredBaseAmount() <= capacity,
            "the shared amount must never exceed the shared capacity, was " + handler.getStoredBaseAmount()
                + " of "
                + capacity);
    }

    @Test
    @DisplayName("a tier refuses an item that is not its own")
    void tierRefusesForeignItem() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();
        Item foreign = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);

        assertEquals(
            0L,
            handler.insert(0, new BigItemStack(StorageFixtures.one(foreign), 10L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a tier must refuse an item that is not the one it shows");
        assertEquals(0L, handler.getStoredBaseAmount(), "a refused item must not change the shared amount");
    }

    @Test
    @DisplayName("a simulation never changes the shared amount or the tiers")
    void simulationIsPure() {
        Item nugget = StorageFixtures.newItem();
        Item ingot = StorageFixtures.newItem();
        Item block = StorageFixtures.newItem();

        CompactingItemHandler handler = configuredHandler(block, ingot, nugget);
        handler.insert(2, new BigItemStack(StorageFixtures.one(nugget), 20L), StorageAction.EXECUTE);
        long before = handler.getStoredBaseAmount();
        List<CompactingTier> tiersBefore = handler.getTiers();

        for (int round = 0; round < 50; round++) {
            handler.insert(1, new BigItemStack(StorageFixtures.one(ingot), 500L), StorageAction.SIMULATE);
            handler.extract(1, 500L, StorageAction.SIMULATE);
        }

        assertEquals(before, handler.getStoredBaseAmount(), "a simulation must not change the shared amount");
        assertEquals(
            tiersBefore.size(),
            handler.getTiers()
                .size(),
            "a simulation must not change the tier definitions");
    }

    /**
     * Builds a configured handler whose tiers are block, ingot, nugget from highest
     * to lowest, with the ratios this test measures against.
     *
     * @param block  highest tier
     * @param ingot  middle tier
     * @param nugget lowest tier
     * @return a handler with three configured tiers
     */
    private static CompactingItemHandler configuredHandler(Item block, Item ingot, Item nugget) {
        CompactingItemHandler handler = new CompactingItemHandler(3) {

            @Override
            public double getMultiplier() {
                return 1D;
            }
        };

        long nuggetUnits = 1L;
        long ingotUnits = nuggetUnits * LOWER_PER_MIDDLE;
        long blockUnits = ingotUnits * MIDDLE_PER_HIGHER;

        List<CompactingTier> tiers = new ArrayList<>();
        tiers.add(new CompactingTier(StorageFixtures.one(block), blockUnits));
        tiers.add(new CompactingTier(StorageFixtures.one(ingot), ingotUnits));
        tiers.add(new CompactingTier(StorageFixtures.one(nugget), nuggetUnits));
        handler.configureTiers(tiers);
        return handler;
    }

    private static long unitsOf(CompactingItemHandler handler, int index) {
        return handler.getTiers()
            .get(index)
            .getBaseUnits();
    }
}
