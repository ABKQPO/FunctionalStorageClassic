package com.hfstudio.functionalstorage.support;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;

import thaumcraft.api.aspects.Aspect;

/**
 * Shared fixtures for essentia storage.
 *
 * <p>
 * Thaumcraft's {@link Aspect} class builds its registry from a plain map of its own
 * constants, so unlike items and fluids it needs no game bootstrap and every
 * registered aspect is available here exactly as in game.
 * </p>
 */
public class AspectBootstrap {

    private AspectBootstrap() {}

    /**
     * Installs whatever state the essentia path needs. Present so every test in the
     * suite starts the same way, even though this path currently needs nothing.
     */
    public static void install() {
        // Touching the class is what matters: its own initializer registers aspects.
        if (Aspect.aspects.isEmpty()) {
            throw new IllegalStateException("Thaumcraft registered no aspects");
        }
    }

    /**
     * @return every registered aspect, in registry order
     */
    public static Aspect[] allAspects() {
        install();
        List<Aspect> aspects = new ArrayList<>(Aspect.aspects.values());
        return aspects.toArray(new Aspect[0]);
    }

    /**
     * @return the first registered aspect
     */
    public static Aspect firstAspect() {
        return allAspects()[0];
    }

    /**
     * @return a registered aspect distinct from {@link #firstAspect()}
     */
    public static Aspect secondAspect() {
        Aspect[] aspects = allAspects();
        return aspects[Math.min(1, aspects.length - 1)];
    }

    /**
     * Builds an essentia handler with a lock flag the caller can toggle.
     *
     * @param slots  slot count
     * @param locked holder whose single entry is the current lock state
     * @return a handler reading its lock state from the holder
     */
    public static BigAspectHandler lockableHandler(int slots, boolean[] locked) {
        return new BigAspectHandler(slots) {

            @Override
            public boolean isLocked() {
                return locked[0];
            }
        };
    }

    /**
     * Builds an essentia handler whose slots hold far more than an int can express.
     *
     * <p>
     * Capacity is scaled through the multiplier, which is the same knob a capacity
     * upgrade turns, so this exercises the real long-capacity path rather than a
     * test-only shortcut.
     * </p>
     *
     * @param slots      slot count
     * @param multiplier capacity multiplier
     * @return a handler with scaled capacity
     */
    public static BigAspectHandler largeHandler(int slots, double multiplier) {
        return new BigAspectHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    /**
     * @param baseCapacity capacity one slot grants before scaling
     * @param factor       multiple of the int boundary the result must exceed
     * @return a multiplier exceeding the int boundary
     */
    public static double intOverflowingMultiplier(long baseCapacity, double factor) {
        double boundary = (double) Integer.MAX_VALUE + 1D;
        return boundary / Math.max(1L, baseCapacity) * Math.max(1D, factor);
    }

    /**
     * @param handler handler to read
     * @param aspect  aspect to total
     * @return the summed stored amount of that aspect
     */
    public static long total(BigAspectHandler handler, Aspect aspect) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.isSameType(aspect)) {
                total += snapshot.getAmount();
            }
        }
        return total;
    }
}
