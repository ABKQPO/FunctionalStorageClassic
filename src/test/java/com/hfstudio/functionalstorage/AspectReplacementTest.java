package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Asserts how clearing and refilling an essentia storage behaves.
 * <p>
 * A container that replaces its contents wholesale clears first and then adds, which
 * is what Thaumcraft's container interface invites. On a storage that retains a filter,
 * the refill can be refused where the clear succeeded, so this checks what actually
 * happens rather than assuming the two halves agree. The aim is to know whether a
 * wholesale replacement can lose essentia, and under which lock state.
 */
public class AspectReplacementTest {

    @BeforeAll
    static void installVanillaState() {
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("an unlocked storage can be cleared and refilled with a different aspect")
    void unlockedStorageAcceptsReplacement() {
        BigAspectHandler handler = new BigAspectHandler(4);
        Aspect first = AspectBootstrap.firstAspect();
        Aspect second = AspectBootstrap.secondAspect();

        handler.insertRouted(new BigAspectStack(first, 100L), StorageAction.EXECUTE);
        clearAll(handler);
        assertEquals(0L, AspectBootstrap.total(handler, first), "the clear must empty the storage");

        handler.insertRouted(new BigAspectStack(second, 50L), StorageAction.EXECUTE);
        assertEquals(50L, AspectBootstrap.total(handler, second), "an unlocked storage must take the new aspect");
        assertEquals(0L, AspectBootstrap.total(handler, first), "the old aspect must be gone");
    }

    @Test
    @DisplayName("a locked storage keeps its retained aspect through a clear and refill")
    void lockedStorageKeepsItsOwnAspect() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(4, locked);
        Aspect retained = AspectBootstrap.firstAspect();

        handler.insertRouted(new BigAspectStack(retained, 100L), StorageAction.EXECUTE);
        locked[0] = true;

        clearAll(handler);
        assertEquals(0L, AspectBootstrap.total(handler, retained), "the clear must empty the storage");

        // The filter is retained, so the same aspect must go back in.
        long restored = handler.insertRouted(new BigAspectStack(retained, 60L), StorageAction.EXECUTE)
            .getProcessedAmount();
        assertEquals(60L, restored, "a locked storage must take back the aspect it retains");
    }

    @Test
    @DisplayName("a locked storage refuses a wholesale replacement with a foreign aspect")
    void lockedStorageRefusesForeignReplacement() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(4, locked);
        Aspect retained = AspectBootstrap.firstAspect();
        Aspect foreign = AspectBootstrap.secondAspect();

        handler.insertRouted(new BigAspectStack(retained, 100L), StorageAction.EXECUTE);
        locked[0] = true;

        // This is the sequence a wholesale replacement performs.
        clearAll(handler);
        long added = handler.insertRouted(new BigAspectStack(foreign, 100L), StorageAction.EXECUTE)
            .getProcessedAmount();

        // The lock is what makes this safe rather than lossy: the storage refuses the
        // foreign aspect, so it cannot silently discard one aspect in favour of another.
        assertEquals(0L, added, "a locked storage must refuse a foreign aspect");
        assertEquals(0L, AspectBootstrap.total(handler, foreign), "the foreign aspect must not be stored");
        assertEquals(
            0L,
            AspectBootstrap.total(handler, retained),
            "the retained aspect was cleared and cannot return because a different one was offered");
    }

    @Test
    @DisplayName("clearing never removes more than is stored")
    void clearingRemovesExactlyWhatIsStored() {
        BigAspectHandler handler = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();
        long stored = handler.insertRouted(new BigAspectStack(aspect, 200L), StorageAction.EXECUTE)
            .getProcessedAmount();

        long removed = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            removed += handler.extract(index, Long.MAX_VALUE, StorageAction.EXECUTE)
                .getProcessedAmount();
        }

        assertEquals(stored, removed, "clearing must remove exactly what was stored");
        assertEquals(0L, AspectBootstrap.total(handler, aspect), "the storage must be empty afterwards");
    }

    @Test
    @DisplayName("a simulation never clears anything")
    void simulationDoesNotClear() {
        BigAspectHandler handler = new BigAspectHandler(4);
        Aspect aspect = AspectBootstrap.firstAspect();
        handler.insertRouted(new BigAspectStack(aspect, 80L), StorageAction.EXECUTE);

        for (int index = 0; index < handler.getStorageCount(); index++) {
            handler.extract(index, Long.MAX_VALUE, StorageAction.SIMULATE);
        }

        assertEquals(80L, AspectBootstrap.total(handler, aspect), "a simulated clear must remove nothing");
    }

    @Test
    @DisplayName("an emptied locked storage still refuses, and reports that it is locked")
    void emptiedLockedStorageStillRefuses() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(2, locked);
        Aspect retained = AspectBootstrap.firstAspect();

        handler.insertRouted(new BigAspectStack(retained, 30L), StorageAction.EXECUTE);
        locked[0] = true;
        clearAll(handler);

        assertTrue(handler.isLocked(), "the storage must still report itself locked");
        assertFalse(
            handler.insertRouted(new BigAspectStack(AspectBootstrap.secondAspect(), 10L), StorageAction.SIMULATE)
                .getProcessedAmount() > 0L,
            "an emptied locked storage must still refuse a foreign aspect");

        // The retained aspect itself must still be accepted, or the filter would be
        // useless after a clear.
        assertTrue(
            handler.insertRouted(new BigAspectStack(retained, 10L), StorageAction.SIMULATE)
                .getProcessedAmount() > 0L,
            "an emptied locked storage must still accept the aspect it retains");
    }

    /**
     * Performs the clear half of a wholesale replacement.
     *
     * @param handler handler to empty
     */
    private static void clearAll(BigAspectHandler handler) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            handler.extract(index, Long.MAX_VALUE, StorageAction.EXECUTE);
        }
    }
}
