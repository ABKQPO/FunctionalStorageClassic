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

    public static void install() {
        if (Aspect.aspects.isEmpty()) {
            throw new IllegalStateException("Thaumcraft registered no aspects");
        }
    }

    public static Aspect[] allAspects() {
        install();
        List<Aspect> aspects = new ArrayList<>(Aspect.aspects.values());
        return aspects.toArray(new Aspect[0]);
    }

    public static Aspect firstAspect() {
        return allAspects()[0];
    }

    public static Aspect secondAspect() {
        Aspect[] aspects = allAspects();
        return aspects[Math.min(1, aspects.length - 1)];
    }

    /**
     * @param locked holder whose single entry is the current lock state
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
     * Capacity is scaled through the multiplier, the same knob a capacity upgrade
     * turns, so this exercises the real long-capacity path rather than a shortcut.
     */
    public static BigAspectHandler largeHandler(int slots, double multiplier) {
        return new BigAspectHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    public static double intOverflowingMultiplier(long baseCapacity, double factor) {
        return StorageFixtures.intOverflowingMultiplier(baseCapacity, factor);
    }

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
