package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Pins how a caller's edits to a handed-out stack reach storage. Both in-game styles
 * must work: a hopper edits the returned object in place, a generic iterator writes a
 * replacement through {@code setInventorySlotContents}. Touching one slot deliberately
 * does not commit the others, because a caller may hold a stack while it reads a further
 * slot and edit that first stack afterwards; committing everything on each read would
 * discard the mark on a stack the caller still holds.
 */
public class InPlaceEditTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("an in-place edit is committed once the inventory is flushed")
    void inPlaceEditReachesStorageOnMarkDirty() {
        BigItemHandler handler = StorageFixtures.handler(4);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 100));
        assertEquals(100, StorageFixtures.total(handler, item), "the initial write must be committed");

        ItemStack held = view.getStackInSlot(0);
        assertNotNull(held, "a populated slot must hand out a stack");
        held.stackSize = 40;

        view.markDirty();
        assertEquals(40, StorageFixtures.total(handler, item), "the in-place edit must have been committed");
    }

    @Test
    @DisplayName("an edit survives reading a different slot in between")
    void inPlaceEditSurvivesAnotherRead() {
        BigItemHandler handler = StorageFixtures.handler(4);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 100));

        // The caller keeps the stack it holds while it reads a further slot, which is
        // ordinary use of the interface, and edits it only afterwards.
        ItemStack held = view.getStackInSlot(0);
        assertNotNull(held, "a populated slot must hand out a stack");
        view.getStackInSlot(1);
        held.stackSize = 40;

        view.markDirty();
        assertEquals(
            40,
            StorageFixtures.total(handler, item),
            "an edit made after reading another slot must still reach storage");
    }

    @Test
    @DisplayName("touching the edited slot again commits it immediately")
    void reReadingTheSlotCommits() {
        BigItemHandler handler = StorageFixtures.handler(4);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 100));

        ItemStack held = view.getStackInSlot(0);
        assertNotNull(held, "a populated slot must hand out a stack");
        held.stackSize = 40;

        ItemStack reread = view.getStackInSlot(0);
        assertNotNull(reread, "the slot must still hold a stack");
        assertEquals(40, reread.stackSize, "re-reading the slot must report the committed edit");
        assertEquals(40, StorageFixtures.total(handler, item), "re-reading the slot must commit the edit");
    }

    @Test
    @DisplayName("clearing a handed-out stack through the slot removes exactly the stored amount")
    void clearingViaSlotRemovesEverything() {
        BigItemHandler handler = StorageFixtures.handler(4);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 250));
        assertEquals(250, StorageFixtures.total(handler, item), "the initial write must be committed");

        view.setInventorySlotContents(0, null);
        assertEquals(0L, StorageFixtures.total(handler, item), "clearing the slot must empty storage");
    }

    @Test
    @DisplayName("writing back the identical stack does not add or remove anything")
    void rewritingTheSameStackIsANoOp() {
        BigItemHandler handler = StorageFixtures.handler(4);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 300));
        for (int round = 0; round < 50; round++) {
            ItemStack held = view.getStackInSlot(0);
            view.setInventorySlotContents(0, held);
        }

        assertEquals(300, StorageFixtures.total(handler, item), "rewriting the same stack must change nothing");
    }

    @Test
    @DisplayName("an in-place edit is committed even when it is the only thing that happens")
    void inPlaceEditCommitsOnFlush() {
        BigItemHandler handler = StorageFixtures.handler(4);
        DrawerItemInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        Item item = StorageFixtures.newItem();

        view.setInventorySlotContents(0, StorageFixtures.stack(item, 80));

        ItemStack held = view.getStackInSlot(0);
        held.stackSize = 75;
        view.flushChanges();

        assertEquals(75, StorageFixtures.total(handler, item), "an explicit flush must commit the edit");
    }

    @Test
    @DisplayName("a sided sweep sees every stored type without altering it")
    void sidedSweepIsReadOnly() {
        BigItemHandler handler = StorageFixtures.handler(64);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        ISidedInventory sided = (ISidedInventory) view;

        Item[] items = new Item[64];
        for (int index = 0; index < items.length; index++) {
            items[index] = StorageFixtures.newItem();
            view.setInventorySlotContents(index, StorageFixtures.stack(items[index], 100 + index));
        }

        int[] slots = sided.getAccessibleSlotsFromSide(0);
        assertEquals(64, slots.length, "every slot must be reachable from a side");

        long seen = 0L;
        for (int slot : slots) {
            ItemStack stack = view.getStackInSlot(slot);
            assertNotNull(stack, "slot " + slot + " must report its stack");
            seen += stack.stackSize;
        }

        long expected = 0L;
        for (int index = 0; index < items.length; index++) {
            expected += 100 + index;
            assertEquals(
                100 + index,
                StorageFixtures.total(handler, items[index]),
                "reading slot " + index + " must not change storage");
        }
        assertEquals(expected, seen, "the sweep must see every stored item");
    }
}
