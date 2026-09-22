package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.item.FastImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSink;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSource;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Item storage under high type variety and high volume.
 *
 * <p>
 * The interesting pressure on an item drawer is not one full slot but many
 * unrelated types at once, driven through the interfaces other mods actually use,
 * while the stored amount runs far past what a single vanilla stack can express.
 * Each check asserts that the amount leaving a caller equals the amount arriving in
 * the drawer, so any disagreement between the reported figure and the committed
 * amount shows up as created or destroyed items.
 * </p>
 */
public class ItemScaleTest {

    private static final int TYPES = 2000;
    private static final int SLOTS = 512;
    private static final ForgeDirection SIDE = ForgeDirection.UP;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("two thousand types each keep their own slot and amount")
    void thousandsOfTypesStayDistinct() {
        BigItemHandler handler = StorageFixtures.handler(TYPES);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        InventoryItemSink sink = new InventoryItemSink(view, SIDE);
        Item[] items = newItemArray(TYPES);

        for (int index = 0; index < TYPES; index++) {
            int amount = 1 + index % 500;
            int rejected = sink.store(new FastImmutableItemStack(StorageFixtures.stack(items[index], amount)));
            assertEquals(0, rejected, "type " + index + " must be fully accepted");
        }

        for (int index = 0; index < TYPES; index++) {
            assertEquals(
                1 + index % 500,
                StorageFixtures.total(handler, items[index]),
                "type " + index + " is not stored under its own identity");
        }
    }

    @Test
    @DisplayName("a full network accepts hundreds of thousands of items without loss")
    void highVolumeInsertionConservesItems() {
        BigItemHandler handler = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        InventoryItemSink sink = new InventoryItemSink(view, SIDE);
        Item item = StorageFixtures.newItem();

        long capacity = StorageFixtures.capacityOf(item) * SLOTS;
        long accepted = 0L;
        long offered = 0L;

        for (int round = 0; round < 200_000 && accepted < capacity; round++) {
            int request = 1 + round % 64;
            offered += request;
            int rejected = sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, request)));
            accepted += request - rejected;
        }

        assertEquals(capacity, accepted, "the drawer must fill to exactly its capacity");
        assertEquals(capacity, StorageFixtures.total(handler, item), "a full drawer must report its whole capacity");
        assertTrue(offered > accepted, "the run must have offered more than the drawer could hold");
    }

    @Test
    @DisplayName("one hundred thousand mixed interactions conserve every type's amount")
    void manyMixedInteractionsConserveItems() {
        int interactions = 100_000;
        BigItemHandler handler = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item[] items = newItemArray(TYPES);

        long[] expected = new long[TYPES];
        Random random = new Random(20260922L);

        // A source scan walks every slot per call, so each extraction is charged to
        // the drawer's whole slot count. The mix is therefore weighted towards
        // insertion, which is what an automated line does anyway.
        long start = System.nanoTime();
        for (int step = 0; step < interactions; step++) {
            int kind = random.nextInt(TYPES);
            int amount = 1 + random.nextInt(200);
            Item item = items[kind];

            if (random.nextInt(10) < 8) {
                InventoryItemSink sink = new InventoryItemSink(view, SIDE);
                int rejected = sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, amount)));
                expected[kind] += amount - rejected;
            } else {
                InventoryItemSource source = new InventoryItemSource(view, SIDE);
                ItemStack pulled = source.pull(stack -> stack.getItem() == item, stack -> amount);
                expected[kind] -= pulled == null ? 0 : pulled.stackSize;
            }
        }
        long elapsed = System.nanoTime() - start;

        assertTotals(handler, items, expected, "after " + interactions + " interactions");
        System.out.println(
            "MEASURE item interactions=" + interactions
                + " elapsedMs="
                + (elapsed / 1_000_000L)
                + " nsPerInteraction="
                + (elapsed / interactions)
                + " types="
                + TYPES
                + " slots="
                + SLOTS);
    }

    @Test
    @DisplayName("a value past the int boundary round-trips through the Forge bridge exactly")
    void intBoundaryRoundTripsExactly() {
        long base = StorageFixtures.capacityOf(StorageFixtures.newItem());
        BigItemHandler handler = StorageFixtures.largeHandler(1, StorageFixtures.intOverflowingMultiplier(base, 4D));
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        long capacity = handler.getCapacity(0);
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must exceed the int boundary, was " + capacity);

        int limit = view.getInventoryStackLimit();
        assertTrue(limit > 64, "the bridge must expose room beyond one stack, was " + limit);

        TransferResult<BigItemStack, ?> filled = handler
            .insertRouted(new BigItemStack(StorageFixtures.one(item), capacity), StorageAction.EXECUTE);
        assertEquals(capacity, filled.getProcessedAmount(), "a long request must be served in full");
        assertEquals(capacity, StorageFixtures.total(handler, item), "the whole long amount must be stored");

        ItemStack read = view.getStackInSlot(0);
        assertEquals(
            Integer.MAX_VALUE,
            read.stackSize,
            "one stack cannot express the whole amount, so the exposed count must saturate");
    }

    @Test
    @DisplayName("many types drained at once leave each other untouched")
    void drainingOneTypeLeavesOthersIntact() {
        BigItemHandler handler = StorageFixtures.handler(TYPES);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        InventoryItemSink sink = new InventoryItemSink(view, SIDE);
        Item[] items = newItemArray(TYPES);

        for (int index = 0; index < TYPES; index++) {
            sink.store(new FastImmutableItemStack(StorageFixtures.stack(items[index], 300 + index % 100)));
        }

        int victim = 500;
        InventoryItemSource source = new InventoryItemSource(view, SIDE);
        ItemStack pulled = source.pull(stack -> stack.getItem() == items[victim], stack -> 150);
        assertEquals(150, pulled.stackSize, "the victim must surrender the requested amount");

        for (int index = 0; index < TYPES; index++) {
            long expected = index == victim ? 300 + index % 100 - 150 : 300 + index % 100;
            assertEquals(
                expected,
                StorageFixtures.total(handler, items[index]),
                "draining type " + victim + " disturbed type " + index);
        }
    }

    private static void assertTotals(BigItemHandler handler, Item[] items, long[] expected, String when) {
        for (int index = 0; index < items.length; index++) {
            assertEquals(
                expected[index],
                StorageFixtures.total(handler, items[index]),
                "type " + index + " diverged " + when);
        }
    }

    private static Item[] newItemArray(int count) {
        Item[] items = new Item[count];
        for (int index = 0; index < count; index++) {
            items[index] = StorageFixtures.newItem();
        }
        return items;
    }
}
