package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.EnderItemHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.world.EnderSavedData;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts that an ender frequency names exactly one storage.
 * <p>
 * Every drawer bound to a frequency reads and writes the same handler, which is what
 * makes the contents shared rather than copied. If a lookup ever returned a different
 * handler for the same frequency, two drawers that appear linked would each hold their
 * own contents and a player would see items vanish from one drawer and appear in
 * another. The policy flags are shared for the same reason, so a lock or a void
 * upgrade applied at one drawer has to be visible at the other.
 */
public class EnderSharingTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("one frequency resolves to one handler every time")
    void oneFrequencyResolvesToOneHandler() {
        EnderSavedData data = new EnderSavedData();
        UUID frequency = UUID.randomUUID();

        BigItemHandler first = data.handlerFor(frequency, 1);
        BigItemHandler second = data.handlerFor(frequency, 1);

        assertSame(first, second, "the same frequency must resolve to the same handler");
        assertSame(first, data.peek(frequency), "peeking must report the same handler");
    }

    @Test
    @DisplayName("different frequencies resolve to different handlers")
    void differentFrequenciesAreSeparate() {
        EnderSavedData data = new EnderSavedData();
        BigItemHandler first = data.handlerFor(UUID.randomUUID(), 1);
        BigItemHandler second = data.handlerFor(UUID.randomUUID(), 1);

        assertNotSame(first, second, "two frequencies must not share storage");

        Item item = StorageFixtures.newItem();
        first.insert(0, new BigItemStack(StorageFixtures.one(item), 50L), StorageAction.EXECUTE);

        assertEquals(50L, StorageFixtures.total(first, item), "the written frequency must hold the items");
        assertEquals(0L, StorageFixtures.total(second, item), "an unrelated frequency must stay empty");
    }

    @Test
    @DisplayName("writing through one drawer is visible through the other")
    void contentsAreShared() {
        EnderSavedData data = new EnderSavedData();
        UUID frequency = UUID.randomUUID();
        Item item = StorageFixtures.newItem();

        BigItemHandler drawerA = data.handlerFor(frequency, 1);
        BigItemHandler drawerB = data.handlerFor(frequency, 1);

        drawerA.insert(0, new BigItemStack(StorageFixtures.one(item), 30L), StorageAction.EXECUTE);
        assertEquals(30L, StorageFixtures.total(drawerB, item), "a drawer must see what its partner stored");

        drawerB.extract(0, 10L, StorageAction.EXECUTE);
        assertEquals(20L, StorageFixtures.total(drawerA, item), "a drawer must see what its partner removed");
    }

    @Test
    @DisplayName("a lock applied at one drawer is visible at the other")
    void policyIsShared() {
        EnderSavedData data = new EnderSavedData();
        UUID frequency = UUID.randomUUID();
        Item item = StorageFixtures.newItem();

        EnderItemHandler drawerA = (EnderItemHandler) data.handlerFor(frequency, 1);
        EnderItemHandler drawerB = (EnderItemHandler) data.handlerFor(frequency, 1);

        drawerA.insert(0, new BigItemStack(StorageFixtures.one(item), 10L), StorageAction.EXECUTE);
        drawerA.setLocked(true);

        assertTrue(drawerB.isLocked(), "a lock applied at one drawer must be visible at the other");
    }

    @Test
    @DisplayName("the policy flags and slot count of a frequency survive a save and reload")
    void policySurvivesReload() {
        EnderSavedData data = new EnderSavedData();
        UUID frequency = UUID.randomUUID();

        EnderItemHandler drawer = (EnderItemHandler) data.handlerFor(frequency, 1);
        drawer.setLocked(true);

        NBTTagCompound saved = new NBTTagCompound();
        data.writeToNBT(saved);

        EnderSavedData reloaded = new EnderSavedData();
        reloaded.readFromNBT(saved);

        BigItemHandler restored = reloaded.peek(frequency);
        assertTrue(restored != null, "a saved frequency must reload");
        assertTrue(restored.isLocked(), "a shared lock must survive a reload");
        assertEquals(1, restored.getStorageCount(), "the slot count must survive a reload");
    }

    @Test
    @DisplayName("every bound frequency appears in the saved table")
    void everyFrequencyIsWritten() {
        EnderSavedData data = new EnderSavedData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        data.handlerFor(first, 1);
        data.handlerFor(second, 1);

        NBTTagCompound saved = new NBTTagCompound();
        data.writeToNBT(saved);

        NBTTagCompound frequencies = saved.getCompoundTag("Frequencies");
        assertTrue(frequencies.hasKey(first.toString()), "the first bound frequency must be saved");
        assertTrue(frequencies.hasKey(second.toString()), "the second bound frequency must be saved");
    }

    @Test
    @DisplayName("a corrupted frequency key is skipped without losing the valid ones")
    void corruptedKeyIsSkipped() {
        EnderSavedData data = new EnderSavedData();
        UUID valid = UUID.randomUUID();

        EnderItemHandler drawer = (EnderItemHandler) data.handlerFor(valid, 1);
        drawer.setLocked(true);

        NBTTagCompound saved = new NBTTagCompound();
        data.writeToNBT(saved);

        // Add a key that is not a UUID at all.
        NBTTagCompound frequencies = saved.getCompoundTag("Frequencies");
        NBTTagCompound bogus = new NBTTagCompound();
        bogus.setInteger("Slots", 1);
        frequencies.setTag("not-a-uuid", bogus);

        EnderSavedData reloaded = new EnderSavedData();
        reloaded.readFromNBT(saved);

        BigItemHandler restored = reloaded.peek(valid);
        assertTrue(restored != null, "the valid frequency must still reload");
        assertTrue(restored.isLocked(), "a broken key must not disturb the valid entries");
    }

    @Test
    @DisplayName("an unbound frequency is not created by peeking")
    void peekingDoesNotCreate() {
        EnderSavedData data = new EnderSavedData();
        UUID frequency = UUID.randomUUID();

        assertEquals(null, data.peek(frequency), "peeking an unused frequency must report nothing");
        assertTrue(data.handlerFor(frequency, 1) != null, "binding must create the storage");
        assertTrue(data.peek(frequency) != null, "a bound frequency must be reportable");
    }
}
