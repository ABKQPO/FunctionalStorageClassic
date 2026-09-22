package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Pins the bridge between vanilla's mutable stacks and committed storage.
 * <p>
 * A caller may edit a stack it was handed, and it may hold that stack while it reads
 * another slot. Both are ordinary uses of the interface, and a bridge that commits
 * only the most recent read would silently discard the earlier edit, so each sequence
 * is stated here and the stored amount is checked afterwards.
 */
public class InventoryBridgeTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("an edit made after reading another slot is still committed")
    void delayedInPlaceEditIsCommitted() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(4);
        for (int index = 0; index < 4; index++) {
            handler.insert(index, new BigItemStack(StorageFixtures.one(item), 100L), StorageAction.EXECUTE);
        }

        DrawerItemInventory inventory = new DrawerItemInventory(handler, "test", () -> {});

        // Hand out slot 0, then slot 1, and only then edit the first stack. A caller
        // doing this is editing a stack it legitimately holds.
        ItemStack first = inventory.getStackInSlot(0);
        assertNotNull(first, "slot 0 must hand out a stack");
        inventory.getStackInSlot(1);

        long before = StorageFixtures.total(handler, item);
        first.stackSize += 25;

        inventory.markDirty();

        assertEquals(
            before + 25L,
            StorageFixtures.total(handler, item),
            "an edit to a stack handed out before another slot was read must still be committed");
    }

    @Test
    @DisplayName("an edit to the most recent read is committed")
    void immediateInPlaceEditIsCommitted() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(4);
        handler.insert(0, new BigItemStack(StorageFixtures.one(item), 100L), StorageAction.EXECUTE);

        DrawerItemInventory inventory = new DrawerItemInventory(handler, "test", () -> {});

        ItemStack stack = inventory.getStackInSlot(0);
        assertNotNull(stack, "slot 0 must hand out a stack");
        stack.stackSize += 25;
        inventory.markDirty();

        assertEquals(125L, StorageFixtures.total(handler, item), "an immediate edit must be committed");
    }

    @Test
    @DisplayName("a withdrawal through the bridge is committed in full")
    void withdrawalIsCommitted() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(4);
        handler.insert(0, new BigItemStack(StorageFixtures.one(item), 100L), StorageAction.EXECUTE);

        DrawerItemInventory inventory = new DrawerItemInventory(handler, "test", () -> {});

        ItemStack stack = inventory.getStackInSlot(0);
        assertNotNull(stack, "slot 0 must hand out a stack");
        stack.stackSize -= 40;
        inventory.markDirty();

        assertEquals(60L, StorageFixtures.total(handler, item), "a withdrawal must be committed in full");
    }

    @Test
    @DisplayName("reading a slot the storage does not hold hands out nothing")
    void emptySlotHandsOutNothing() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(4);
        handler.insert(0, new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);

        DrawerItemInventory inventory = new DrawerItemInventory(handler, "test", () -> {});

        assertEquals(null, inventory.getStackInSlot(1), "an empty index must hand out nothing");
        assertEquals(null, inventory.getStackInSlot(-1), "an index below zero must hand out nothing");
        assertEquals(null, inventory.getStackInSlot(99), "an index past the end must hand out nothing");
    }

    @Test
    @DisplayName("a repeated read of an unchanged slot loses nothing")
    void repeatedReadsAreStable() {
        Item item = StorageFixtures.newItem();
        BigItemHandler handler = new BigItemHandler(2);
        handler.insert(0, new BigItemStack(StorageFixtures.one(item), 64L), StorageAction.EXECUTE);

        DrawerItemInventory inventory = new DrawerItemInventory(handler, "test", () -> {});

        for (int round = 0; round < 20; round++) {
            ItemStack stack = inventory.getStackInSlot(0);
            assertNotNull(stack, "the populated slot must keep handing out a stack");
            assertEquals(64, stack.stackSize, "an unedited read must keep reporting the stored amount");
        }
        inventory.markDirty();

        assertEquals(64L, StorageFixtures.total(handler, item), "reads alone must never move items");
    }
}
