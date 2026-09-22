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
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Conservation checks under sustained random logistics traffic.
 *
 * <p>
 * Every scenario drives the real GTNHLib sink and source, which are the paths
 * GT5U buses and vanilla hoppers use, and asserts that the amount of an item
 * leaving a source equals the amount arriving in a sink. A drawer reports its
 * stored amount through an int-counted inventory, so any mismatch between the
 * reported figure and the physically committed amount shows up here as created or
 * destroyed items.
 * </p>
 */
public class LogisticsConservationTest {

    private static final int SLOTS = 4;
    private static final ForgeDirection SIDE = ForgeDirection.UP;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("a drawn-down drawer never loses or duplicates items across many transfers")
    void repeatedTransfersConserveItems() {
        BigItemHandler drawer = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(drawer, "drawer", () -> {});
        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * SLOTS;

        long stored = 0L;
        for (int round = 0; round < 400; round++) {
            InventoryItemSink sink = new InventoryItemSink(view, SIDE);
            int request = 1 + round % 97;
            int rejected = sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, request)));
            stored += request - rejected;

            assertEquals(
                stored,
                StorageFixtures.total(drawer, item),
                "stored total diverged after insert round " + round);
        }

        assertEquals(capacity, StorageFixtures.total(drawer, item), "a full drawer must hold its whole capacity");
        assertEquals(capacity, stored, "the sink must never accept more than the drawer holds");
    }

    @Test
    @DisplayName("draining then refilling a drawer conserves the item count exactly")
    void drainAndRefillConservesItems() {
        BigItemHandler drawer = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(drawer, "drawer", () -> {});
        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * SLOTS;

        InventoryItemSink sink = new InventoryItemSink(view, SIDE);
        sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, (int) capacity)));
        assertEquals(capacity, StorageFixtures.total(drawer, item), "the drawer must fill to capacity");

        long extractedTotal = 0L;
        int rounds = 0;
        // Drain until the drawer is empty rather than for a fixed number of rounds,
        // so the check proves the source can actually empty it. A round budget would
        // only prove the budget ran out.
        while (rounds < 10_000) {
            InventoryItemSource source = new InventoryItemSource(view, SIDE);
            int chunk = 1 + rounds % 53;
            ItemStack pulled = source.pull(null, stack -> chunk);
            if (pulled == null) {
                break;
            }
            extractedTotal += pulled.stackSize;
            assertEquals(
                capacity - extractedTotal,
                StorageFixtures.total(drawer, item),
                "stored total diverged after extraction round " + rounds);
            rounds++;
        }

        assertTrue(extractedTotal > 0L, "the source must extract something from a filled drawer");
        assertEquals(capacity, extractedTotal, "every stored item must come back out exactly once");
        assertEquals(0L, StorageFixtures.total(drawer, item), "a fully drained drawer must report nothing stored");
    }

    @Test
    @DisplayName("random interleaved insert and extract keeps both drawer and caller consistent")
    void randomInterleavingConservesItems() {
        BigItemHandler drawer = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(drawer, "drawer", () -> {});
        Item item = StorageFixtures.newItem();
        long capacity = StorageFixtures.capacityOf(item) * SLOTS;

        Random random = new Random(20260922L);
        long expected = 0L;

        for (int step = 0; step < 3000; step++) {
            boolean inserting = random.nextBoolean();
            int amount = 1 + random.nextInt(80);

            if (inserting) {
                InventoryItemSink sink = new InventoryItemSink(view, SIDE);
                int rejected = sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, amount)));
                expected = Math.min(capacity, expected + (amount - rejected));
            } else {
                InventoryItemSource source = new InventoryItemSource(view, SIDE);
                ItemStack pulled = source.pull(null, stack -> amount);
                expected = Math.max(0L, expected - (pulled == null ? 0 : pulled.stackSize));
            }

            assertEquals(
                expected,
                StorageFixtures.total(drawer, item),
                "stored total diverged at step " + step + " (inserting=" + inserting + ')');
        }
    }

    @Test
    @DisplayName("distinct items never share a slot, so none is overwritten")
    void distinctItemsStayInTheirOwnSlots() {
        BigItemHandler drawer = StorageFixtures.handler(SLOTS);
        IInventory view = new DrawerItemInventory(drawer, "drawer", () -> {});

        for (int index = 0; index < SLOTS; index++) {
            Item item = StorageFixtures.newItem();
            InventoryItemSink sink = new InventoryItemSink(view, SIDE);
            sink.store(new FastImmutableItemStack(StorageFixtures.stack(item, 300 + index)));
        }

        int populated = 0;
        for (int index = 0; index < SLOTS; index++) {
            if (drawer.getSnapshot(index)
                .hasTemplate()) {
                populated++;
            }
        }
        assertEquals(SLOTS, populated, "each distinct item must occupy its own index");
    }
}
