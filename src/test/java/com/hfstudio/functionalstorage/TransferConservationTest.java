package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.TestInventory;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;
import com.hfstudio.functionalstorage.util.TransferUtil;

/**
 * Asserts that the inventory transfer helpers conserve items.
 * <p>
 * These helpers move stacks between a drawer and an ordinary inventory, and they do so
 * by inserting first and reporting what is left over. The count they return is what the
 * caller subtracts from its budget, so a helper that reported more than it placed, or
 * that placed a stack and then lost track of it, would either strand items in limbo or
 * duplicate them. Every check states the amounts on both sides rather than trusting the
 * reported figure.
 */
public class TransferConservationTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("inserting into empty slots fills them in order")
    void insertionFillsSlotsInOrder() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(4);

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(4), StorageFixtures.stack(item, 100));

        assertNull(leftover, "an inventory with room must accept the whole stack");
        assertEquals(100, target.totalOf(item), "every item must be accounted for");
        assertEquals(2, target.populatedSlots(), "100 items must occupy two slots of a sixty-four stack");
    }

    @Test
    @DisplayName("inserting reports the surplus it could not place")
    void insertionReportsSurplus() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(1);

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(1), StorageFixtures.stack(item, 100));

        assertNotNull(leftover, "a full inventory must report a surplus");
        assertEquals(36, leftover.stackSize, "a single slot of sixty-four must leave thirty-six");
        assertEquals(64, target.totalOf(item), "the inventory must hold exactly its capacity");
    }

    @Test
    @DisplayName("inserting merges into a slot that already holds the item")
    void insertionMergesIntoMatchingSlot() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(2);
        target.setInventorySlotContents(0, StorageFixtures.stack(item, 30));

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(2), StorageFixtures.stack(item, 20));

        assertNull(leftover, "the whole stack must fit into the matching slot");
        assertEquals(50, target.totalOf(item), "the matching slot must be topped up");
        assertEquals(1, target.populatedSlots(), "no further slot must be opened while the first has room");
    }

    @Test
    @DisplayName("inserting skips a slot holding a different item")
    void insertionSkipsForeignSlot() {
        Item stored = StorageFixtures.newItem();
        Item incoming = StorageFixtures.newItem();
        TestInventory target = new TestInventory(2);
        target.setInventorySlotContents(0, StorageFixtures.stack(stored, 64));

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(2), StorageFixtures.stack(incoming, 10));

        assertNull(leftover, "the incoming item must take the empty slot");
        assertEquals(64, target.totalOf(stored), "the foreign slot must be left alone");
        assertEquals(10, target.totalOf(incoming), "the incoming item must be stored in full");
    }

    @Test
    @DisplayName("an exhausted request reports a null surplus, not an empty one")
    void exhaustedRequestReportsNull() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(4);

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(4), StorageFixtures.stack(item, 64));

        assertNull(leftover, "a request that fits must report null rather than an empty stack");
    }

    @Test
    @DisplayName("inserting honours the inventory's own stack limit")
    void insertionHonoursStackLimit() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(4, 16);

        // Four slots of sixteen hold sixty-four, so forty placed across them must fill
        // three of the four lanes and leave nothing over.
        ItemStack fitted = TransferUtil.insertIntoInventory(target, slots(4), StorageFixtures.stack(item, 40));
        assertNull(fitted, "four lanes of sixteen must absorb forty");
        assertEquals(40, target.totalOf(item), "every item must be placed");
        assertEquals(3, target.populatedSlots(), "forty items at sixteen per lane must occupy three lanes");

        // A request beyond the whole capacity must report the surplus instead.
        TestInventory small = new TestInventory(1, 16);
        ItemStack surplus = TransferUtil.insertIntoInventory(small, slots(1), StorageFixtures.stack(item, 40));
        assertNotNull(surplus, "a request beyond capacity must report a surplus");
        assertEquals(24, surplus.stackSize, "a single lane of sixteen must leave twenty-four");
        assertEquals(16, small.totalOf(item), "the lane must hold exactly its limit");
    }

    @Test
    @DisplayName("a request for nothing moves nothing")
    void zeroRequestMovesNothing() {
        Item item = StorageFixtures.newItem();
        TestInventory target = new TestInventory(4);

        ItemStack leftover = TransferUtil.insertIntoInventory(target, slots(4), StorageFixtures.stack(item, 0));

        assertEquals(0, target.totalOf(item), "an empty request must place nothing");
        assertTrue(leftover == null || leftover.stackSize == 0, "an empty request leaves nothing over");
    }

    @Test
    @DisplayName("accessible slots cover the whole inventory when it is not sided")
    void accessibleSlotsCoverWholeInventory() {
        TestInventory inventory = new TestInventory(7);
        int[] slots = TransferUtil.accessibleSlots(inventory, ForgeDirection.UP);

        assertEquals(7, slots.length, "an unsided inventory must expose every slot");
        for (int index = 0; index < 7; index++) {
            assertEquals(index, slots[index], "slot order must be preserved");
        }
    }

    private static int[] slots(int count) {
        int[] indices = new int[count];
        for (int index = 0; index < count; index++) {
            indices[index] = index;
        }
        return indices;
    }
}
