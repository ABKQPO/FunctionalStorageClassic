package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts the rules of a single-capacity slot, the shape an armory cabinet uses.
 * <p>
 * That storage overrides two rules: it accepts only items that do not stack, and each
 * index holds exactly one. Both are expressed as overrides rather than as a separate
 * kind of storage, so they have to survive the ordinary insertion path, which checks
 * acceptance before it checks capacity. These tests pin both rules from the outside,
 * including the case where a caller asks for a quantity the slot cannot hold.
 */
public class SingleCapacityStorageTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("an item that stacks is refused even when the slot is empty")
    void stackingItemIsRefused() {
        BigItemHandler cabinet = cabinet(4);
        Item stackable = StorageFixtures.newItem();

        assertEquals(
            0L,
            cabinet.insert(0, new BigItemStack(StorageFixtures.one(stackable), 1L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a stackable item must be refused by a single-capacity slot");
        assertEquals(0L, StorageFixtures.total(cabinet, stackable), "a refused item must not be stored");
    }

    @Test
    @DisplayName("a non-stacking item fills the slot and a second one is refused")
    void nonStackingItemFillsTheSlot() {
        BigItemHandler cabinet = cabinet(4);
        Item tool = StorageFixtures.unstackableItem();

        assertEquals(
            1L,
            cabinet.insert(0, new BigItemStack(StorageFixtures.one(tool), 1L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "the first non-stacking item must be stored");
        assertEquals(
            1L,
            cabinet.getSnapshot(0)
                .getAmount(),
            "the slot must report exactly one item");

        assertEquals(
            0L,
            cabinet.insert(0, new BigItemStack(StorageFixtures.one(tool), 1L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a full single-capacity slot must refuse a second item");
        assertEquals(1L, StorageFixtures.total(cabinet, tool), "the slot must still hold exactly one");
    }

    @Test
    @DisplayName("a request for more than one is clamped to the slot's capacity")
    void oversizedRequestIsClamped() {
        BigItemHandler cabinet = cabinet(4);
        Item tool = StorageFixtures.unstackableItem();

        long processed = cabinet.insert(0, new BigItemStack(StorageFixtures.one(tool), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();

        assertEquals(1L, processed, "a slot holding one item must store exactly one from a larger request");
        assertEquals(1L, StorageFixtures.total(cabinet, tool), "the slot must not exceed its capacity of one");
    }

    @Test
    @DisplayName("each slot holds one item and the slot count is preserved")
    void eachSlotHoldsOne() {
        BigItemHandler cabinet = cabinet(3);
        Item[] tools = new Item[3];
        for (int index = 0; index < tools.length; index++) {
            tools[index] = StorageFixtures.unstackableItem();
            long stored = cabinet
                .insert(index, new BigItemStack(StorageFixtures.one(tools[index]), 8L), StorageAction.EXECUTE)
                .getProcessedAmount();
            assertEquals(1L, stored, "slot " + index + " must hold exactly one");
        }

        assertEquals(3, cabinet.getStorageCount(), "every slot must remain addressable");
        for (int index = 0; index < tools.length; index++) {
            assertEquals(
                1L,
                cabinet.getSnapshot(index)
                    .getAmount(),
                "slot " + index + " must report one item");
        }
    }

    @Test
    @DisplayName("a non-stacking item is refused by a plain drawer's rules when it is stackable")
    void plainDrawerStillAcceptsStackableItems() {
        // The control case: the ordinary storage must keep accepting stackable items,
        // so the restriction is a property of this storage and not of the core.
        BigItemHandler plain = new BigItemHandler(4);
        Item stackable = StorageFixtures.newItem();

        long stored = plain.insert(0, new BigItemStack(StorageFixtures.one(stackable), 64L), StorageAction.EXECUTE)
            .getProcessedAmount();

        assertEquals(64L, stored, "an ordinary drawer must accept a stackable item");
        assertTrue(
            plain.getSnapshot(0)
                .getAmount() > 1L,
            "an ordinary drawer must not be limited to one");
    }

    @Test
    @DisplayName("a simulated request obeys the same rules without storing anything")
    void simulationObeysTheSameRules() {
        BigItemHandler cabinet = cabinet(2);
        Item tool = StorageFixtures.unstackableItem();
        Item stackable = StorageFixtures.newItem();

        assertEquals(
            1L,
            cabinet.insert(0, new BigItemStack(StorageFixtures.one(tool), 5L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a simulation must predict the clamped amount");
        assertEquals(
            0L,
            cabinet.insert(1, new BigItemStack(StorageFixtures.one(stackable), 1L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a simulation must refuse a stackable item too");
        assertEquals(0L, StorageFixtures.total(cabinet, tool), "a simulation must store nothing");
        assertFalse(
            cabinet.getSnapshot(0)
                .hasTemplate(),
            "a simulated request must not configure the slot");
    }

    /**
     * Builds storage with the armory cabinet's rules: one item per slot, and only for
     * items that do not stack.
     *
     * @param slots slot count
     * @return storage with single-capacity slots
     */
    private static BigItemHandler cabinet(int slots) {
        return new BigItemHandler(slots) {

            @Override
            protected boolean acceptsResource(BigItemStack resource) {
                ItemStack item = resource.getTemplate();
                return item != null && item.getMaxStackSize() == 1;
            }

            @Override
            protected long capacityLimit() {
                return 1L;
            }
        };
    }
}
