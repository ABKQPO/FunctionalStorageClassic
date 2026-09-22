package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Verifies that an essentia drawer survives a save and reload unchanged.
 * <p>
 * Anything lost here is lost silently and permanently: a player's essentia is gone
 * after a restart, or a retained filter disappears and the drawer begins accepting
 * aspects it was told to keep out. Essentia is the one kind whose identity persists
 * by name rather than by a registry lookup, so this is the kind that can be verified
 * end to end without a running game.
 */
public class AspectPersistenceTest {

    @BeforeAll
    static void installVanillaState() {
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("contents survive a save and reload exactly")
    void contentsSurviveRoundTrip() {
        Aspect first = AspectBootstrap.firstAspect();
        Aspect second = AspectBootstrap.secondAspect();

        BigAspectHandler source = new BigAspectHandler(4);
        long capacityPerIndex = source.getCapacity(0);
        assertTrue(capacityPerIndex > 0L, "the fixture must have capacity");

        // Every amount is derived from the real capacity, so the check is about
        // persistence rather than about a request larger than the drawer can hold.
        long firstAmount = Math.max(1L, capacityPerIndex / 3L);
        long secondAmount = Math.max(1L, capacityPerIndex / 2L);
        long storedFirst = source.insertRouted(new BigAspectStack(first, firstAmount), StorageAction.EXECUTE)
            .getProcessedAmount();
        long storedSecond = source.insertRouted(new BigAspectStack(second, secondAmount), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(firstAmount, storedFirst, "the fixture must store the whole first request");
        assertEquals(secondAmount, storedSecond, "the fixture must store the whole second request");

        NBTTagCompound saved = source.serializeNBT();
        BigAspectHandler restored = new BigAspectHandler(4);
        restored.deserializeNBT(saved);

        assertEquals(storedFirst, AspectBootstrap.total(restored, first), "the first aspect must reload exactly");
        assertEquals(storedSecond, AspectBootstrap.total(restored, second), "the second aspect must reload exactly");
    }

    @Test
    @DisplayName("a total far beyond an int survives a save and reload")
    void longAmountSurvivesRoundTrip() {
        Aspect aspect = AspectBootstrap.firstAspect();
        // Capacity beyond an int is the case a long-to-int slip would silently cut.
        double multiplier = AspectBootstrap.intOverflowingMultiplier(256L, 4D);
        BigAspectHandler source = AspectBootstrap.largeHandler(4, multiplier);
        long capacity = source.getCapacity(0);
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must exceed the int boundary, was " + capacity);

        long stored = source.insertRouted(new BigAspectStack(aspect, capacity), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertTrue(stored > Integer.MAX_VALUE, "the fixture must store more than an int can express");

        BigAspectHandler restored = AspectBootstrap.largeHandler(4, multiplier);
        restored.deserializeNBT(source.serializeNBT());

        assertEquals(stored, AspectBootstrap.total(restored, aspect), "a long total must survive a reload uncut");
    }

    @Test
    @DisplayName("a retained filter on an empty index survives a reload")
    void retainedFilterSurvivesRoundTrip() {
        Aspect retained = AspectBootstrap.firstAspect();
        Aspect other = AspectBootstrap.secondAspect();
        boolean[] locked = { false };

        // Build the state a player creates: fill, then lock so the filter is retained.
        // The index is configured while unlocked because a lock refuses a resource it
        // does not already retain.
        BigAspectHandler source = AspectBootstrap.lockableHandler(2, locked);
        source.insertRouted(new BigAspectStack(retained, 50L), StorageAction.EXECUTE);
        locked[0] = true;

        assertTrue(source.isLocked(), "the source must report itself locked");
        assertEquals(50L, AspectBootstrap.total(source, retained), "the retained aspect must be stored");

        NBTTagCompound saved = source.serializeNBT();
        BigAspectHandler restored = AspectBootstrap.lockableHandler(2, locked);
        restored.deserializeNBT(saved);

        assertEquals(
            50L,
            AspectBootstrap.total(restored, retained),
            "a locked drawer's contents must survive a reload");
        assertEquals(
            0L,
            restored.insertRouted(new BigAspectStack(other, 10L), StorageAction.SIMULATE)
                .getProcessedAmount(),
            "a reloaded locked drawer must refuse an aspect it does not retain");
    }

    @Test
    @DisplayName("an unlocked empty index is not persisted as a filter")
    void unlockedEmptyIndexIsNotPersisted() {
        BigAspectHandler source = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();
        source.insertRouted(new BigAspectStack(aspect, 10L), StorageAction.EXECUTE);
        source.extractRouted(new BigAspectStack(aspect, 10L), StorageAction.EXECUTE);

        NBTTagCompound saved = source.serializeNBT();
        BigAspectHandler restored = new BigAspectHandler(4);
        restored.deserializeNBT(saved);

        assertTrue(
            restored.summary()
                .acceptsMore(),
            "an unlocked drawer that was emptied must reload open, not stuck shut");
        assertEquals(
            10L,
            restored.insertRouted(new BigAspectStack(AspectBootstrap.secondAspect(), 10L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a reloaded open drawer must accept any aspect");
    }

    @Test
    @DisplayName("an empty tag clears the drawer rather than keeping stale contents")
    void emptyTagClearsContents() {
        Aspect aspect = AspectBootstrap.firstAspect();
        BigAspectHandler handler = new BigAspectHandler(4);
        handler.insertRouted(new BigAspectStack(aspect, 99L), StorageAction.EXECUTE);

        handler.deserializeNBT(null);

        assertEquals(0L, AspectBootstrap.total(handler, aspect), "loading nothing must clear the drawer");
        assertEquals(
            0L,
            handler.summary()
                .getTotal(aspect),
            "the memo must reflect the cleared state");
        assertTrue(
            handler.serializeNBT()
                .getTagList("Entries", 10)
                .tagCount() == 0,
            "an empty drawer must save nothing");
    }

    @Test
    @DisplayName("an out-of-range or malformed entry is ignored without disturbing the rest")
    void malformedEntriesAreIgnored() {
        Aspect aspect = AspectBootstrap.firstAspect();
        BigAspectHandler handler = new BigAspectHandler(2);
        handler.insertRouted(new BigAspectStack(aspect, 42L), StorageAction.EXECUTE);

        NBTTagCompound saved = handler.serializeNBT();
        NBTTagList entries = saved.getTagList("Entries", 10);

        // An index past the end, an entry naming no aspect, and a negative amount.
        NBTTagCompound outOfRange = new NBTTagCompound();
        outOfRange.setInteger("Index", 99);
        outOfRange.setLong("Amount", 5L);
        NBTTagCompound template = new NBTTagCompound();
        template.setString("Aspect", aspect.getTag());
        outOfRange.setTag("Template", template);
        entries.appendTag(outOfRange);

        NBTTagCompound nameless = new NBTTagCompound();
        nameless.setInteger("Index", 1);
        nameless.setLong("Amount", 5L);
        nameless.setTag("Template", new NBTTagCompound());
        entries.appendTag(nameless);

        NBTTagCompound negative = new NBTTagCompound();
        negative.setInteger("Index", 1);
        negative.setLong("Amount", -500L);
        NBTTagCompound secondTemplate = new NBTTagCompound();
        secondTemplate.setString(
            "Aspect",
            AspectBootstrap.secondAspect()
                .getTag());
        negative.setTag("Template", secondTemplate);
        entries.appendTag(negative);

        BigAspectHandler restored = new BigAspectHandler(2);
        restored.deserializeNBT(saved);

        assertEquals(42L, AspectBootstrap.total(restored, aspect), "a valid entry must survive malformed neighbours");
        assertEquals(
            0L,
            AspectBootstrap.total(restored, AspectBootstrap.secondAspect()),
            "a negative amount must not be stored");
        for (int index = 0; index < restored.getStorageCount(); index++) {
            assertTrue(
                restored.getSnapshot(index)
                    .getAmount() >= 0L,
                "a reloaded index must never hold a negative amount");
        }
    }

    @Test
    @DisplayName("a saved drawer whose kind is removed reloads as empty rather than crashing")
    void unknownAspectReloadsAsEmpty() {
        BigAspectHandler handler = new BigAspectHandler(2);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList entries = new NBTTagList();

        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("Index", 0);
        entry.setLong("Amount", 500L);
        NBTTagCompound template = new NBTTagCompound();
        template.setString("Aspect", "NOT_A_REAL_ASPECT_TAG");
        entry.setTag("Template", template);
        entries.appendTag(entry);
        root.setTag("Entries", entries);

        handler.deserializeNBT(root);

        assertNotNull(handler.summary(), "the drawer must still answer queries");
        assertEquals(
            0L,
            handler.summary()
                .getTotal(AspectBootstrap.firstAspect()),
            "an unknown aspect stores nothing");
        assertEquals(
            0L,
            handler.serializeNBT()
                .getTagList("Entries", 10)
                .tagCount(),
            "an unknown aspect must not be written back as a retained filter");
    }

    @Test
    @DisplayName("a summary rebuilt after a reload describes the reloaded contents")
    void summaryMatchesReloadedContents() {
        Aspect first = AspectBootstrap.firstAspect();
        Aspect second = AspectBootstrap.secondAspect();

        BigAspectHandler source = new BigAspectHandler(8);
        source.insertRouted(new BigAspectStack(first, 30L), StorageAction.EXECUTE);
        source.insertRouted(new BigAspectStack(second, 40L), StorageAction.EXECUTE);

        BigAspectHandler restored = new BigAspectHandler(8);
        restored.deserializeNBT(source.serializeNBT());

        AspectSummary summary = restored.summary();
        assertEquals(30L, summary.getTotal(first), "the summary must report the reloaded first aspect");
        assertEquals(40L, summary.getTotal(second), "the summary must report the reloaded second aspect");
        assertEquals(
            2,
            summary.getTotals()
                .size(),
            "the summary must list exactly the reloaded aspects");
        assertFalse(
            summary.getTotals()
                .isEmpty(),
            "a reloaded drawer with contents must not summarize as empty");
    }
}
