package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.support.AspectBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Essentia storage under high type variety and long amounts.
 *
 * <p>
 * Thaumcraft speaks essentia in ints, while a drawer holds far more, so the same
 * boundary that applies to items and fluids applies here. These checks hold every
 * registered aspect at once and drive long requests through the routed operations
 * the tubes, the container API, and the AE bridge all use.
 * </p>
 */
public class AspectScaleTest {

    private static final int SLOTS = 256;

    @BeforeAll
    static void installVanillaState() {
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("every registered aspect can be stored at once without identity bleed")
    void everyAspectStaysDistinct() {
        Aspect[] aspects = AspectBootstrap.allAspects();
        BigAspectHandler handler = new BigAspectHandler(aspects.length);

        for (int index = 0; index < aspects.length; index++) {
            TransferResult<BigAspectStack, ?> result = handler
                .insertRouted(new BigAspectStack(aspects[index], 100 + index), StorageAction.EXECUTE);
            assertEquals(100 + index, result.getProcessedAmount(), "aspect " + index + " must be accepted");
        }

        for (int index = 0; index < aspects.length; index++) {
            assertEquals(
                100 + index,
                aspectTotal(handler, aspects[index]),
                "aspect " + index + " is not stored under its own identity");
        }
    }

    @Test
    @DisplayName("a long amount past the int boundary saturates on read but is stored in full")
    void longAmountSaturatesWithoutLoss() {
        long baseCapacity = new BigAspectHandler(1).getCapacity(0);
        BigAspectHandler handler = AspectBootstrap
            .largeHandler(4, AspectBootstrap.intOverflowingMultiplier(baseCapacity, 4D));
        Aspect aspect = AspectBootstrap.firstAspect();
        long capacity = handler.getCapacity(0) * 4;
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must exceed the int boundary, was " + capacity);

        TransferResult<BigAspectStack, ?> filled = handler
            .insertRouted(new BigAspectStack(aspect, capacity), StorageAction.EXECUTE);
        assertEquals(capacity, filled.getProcessedAmount(), "a long request must be served in full");
        assertEquals(capacity, aspectTotal(handler, aspect), "the whole long amount must be stored");

        assertEquals(
            Integer.MAX_VALUE,
            handler.getSnapshot(0)
                .toAmount(),
            "the int view of a long amount must saturate, not wrap");

        TransferResult<BigAspectStack, ?> drained = handler
            .extractRouted(new BigAspectStack(aspect, capacity), StorageAction.EXECUTE);
        assertEquals(capacity, drained.getProcessedAmount(), "the whole long amount must drain back out");
        assertEquals(0L, aspectGrandTotal(handler), "draining everything must leave nothing");
    }

    @Test
    @DisplayName("two million routed essentia interactions conserve every aspect's amount")
    void millionsOfInteractionsConserveEssentia() {
        int interactions = 2_000_000;
        Aspect[] aspects = AspectBootstrap.allAspects();
        long baseCapacity = new BigAspectHandler(1).getCapacity(0);
        BigAspectHandler handler = AspectBootstrap
            .largeHandler(SLOTS, AspectBootstrap.intOverflowingMultiplier(baseCapacity, SLOTS * 2D));
        long capacity = handler.getCapacity(0) * SLOTS;
        assertTrue(capacity > Integer.MAX_VALUE, "the fixture must hold more than an int can express");

        BigAspectStack[] requests = new BigAspectStack[aspects.length];
        for (int index = 0; index < aspects.length; index++) {
            requests[index] = new BigAspectStack(aspects[index], 1L);
        }

        long[] expected = new long[aspects.length];
        Random random = new Random(20260922L);
        long accepted = 0L;
        long rejectedTotal = 0L;

        long start = System.nanoTime();
        for (int step = 0; step < interactions; step++) {
            int kind = random.nextInt(aspects.length);
            int amount = 1 + random.nextInt(100_000);
            BigAspectStack request = requests[kind].withAmount(amount);

            if (random.nextInt(10) < 6) {
                long processed = handler.insertRouted(request, StorageAction.EXECUTE)
                    .getProcessedAmount();
                expected[kind] += processed;
                accepted += processed;
            } else {
                long processed = handler.extractRouted(request, StorageAction.EXECUTE)
                    .getProcessedAmount();
                expected[kind] -= processed;
                rejectedTotal += processed;
            }

            if (step % 250_000 == 0) {
                assertTotals(handler, aspects, expected, "at step " + step);
            }
        }
        long elapsed = System.nanoTime() - start;

        assertTotals(handler, aspects, expected, "after " + interactions + " interactions");
        long stored = aspectGrandTotal(handler);
        assertEquals(accepted - rejectedTotal, stored, "the grand total must match every operation");

        System.out.println(
            "MEASURE aspect interactions=" + interactions
                + " elapsedMs="
                + (elapsed / 1_000_000L)
                + " nsPerInteraction="
                + (elapsed / interactions)
                + " kinds="
                + aspects.length
                + " slots="
                + SLOTS
                + " stored="
                + stored
                + " capacity="
                + capacity);
    }

    @Test
    @DisplayName("simulation never mutates essentia, across every aspect")
    void simulationNeverMutatesEssentia() {
        Aspect[] aspects = AspectBootstrap.allAspects();
        BigAspectHandler handler = new BigAspectHandler(SLOTS);
        Random random = new Random(555L);
        long expected = 0L;

        for (int step = 0; step < 300_000; step++) {
            Aspect aspect = aspects[random.nextInt(aspects.length)];
            int amount = 1 + random.nextInt(50_000);
            boolean execute = step % 3 != 0;
            StorageAction action = StorageAction.fromSimulation(!execute);

            if (step % 2 == 0) {
                long processed = handler.insertRouted(new BigAspectStack(aspect, amount), action)
                    .getProcessedAmount();
                if (execute) {
                    expected += processed;
                }
            } else {
                long processed = handler.extractRouted(new BigAspectStack(aspect, amount), action)
                    .getProcessedAmount();
                if (execute) {
                    expected -= processed;
                }
            }
        }

        assertEquals(expected, aspectGrandTotal(handler), "a simulated call changed stored essentia");
    }

    @Test
    @DisplayName("the lock retains a filter and refuses a different aspect")
    void lockedFilterRefusesOtherAspects() {
        boolean[] locked = { false };
        BigAspectHandler handler = AspectBootstrap.lockableHandler(4, locked);
        Aspect air = AspectBootstrap.firstAspect();
        Aspect other = AspectBootstrap.secondAspect();

        handler.insertRouted(new BigAspectStack(air, 50L), StorageAction.EXECUTE);
        handler.extractRouted(new BigAspectStack(air, 50L), StorageAction.EXECUTE);
        locked[0] = true;

        assertEquals(
            0L,
            handler.insertRouted(new BigAspectStack(other, 10L), StorageAction.EXECUTE)
                .getProcessedAmount(),
            "a locked drawer must refuse an unrelated aspect");
        assertEquals(0L, aspectTotal(handler, other), "nothing of the refused aspect may be stored");
    }

    private static void assertTotals(IBigAspectHandler handler, Aspect[] aspects, long[] expected, String when) {
        for (int index = 0; index < aspects.length; index++) {
            assertEquals(
                expected[index],
                aspectTotal(handler, aspects[index]),
                "aspect " + index + " diverged " + when);
        }
    }

    private static long aspectTotal(IBigAspectHandler handler, Aspect aspect) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.isSameType(aspect)) {
                total += snapshot.getAmount();
            }
        }
        return total;
    }

    private static long aspectGrandTotal(IBigAspectHandler handler) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
        }
        return total;
    }
}
