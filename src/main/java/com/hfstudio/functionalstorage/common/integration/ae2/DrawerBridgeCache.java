package com.hfstudio.functionalstorage.common.integration.ae2;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class DrawerBridgeCache {

    private static final Map<ControllableDrawerTile, Entry> ENTRIES = new WeakHashMap<>();

    private DrawerBridgeCache() {}

    @Nonnull
    public static DrawerMEInventoryHandler itemMonitor(@Nonnull ControllableDrawerTile drawer) {
        Entry entry = entryFor(drawer);
        if (entry.items == null) {
            entry.items = new DrawerMEInventoryHandler(drawer.getItemHandler(), drawer.getStorageUpgradeSlots());
        }
        return entry.items;
    }

    @Nonnull
    public static DrawerMEEssentiaInventoryHandler aspectMonitor(@Nonnull ControllableDrawerTile drawer) {
        Entry entry = entryFor(drawer);
        if (entry.aspects == null) {
            entry.aspects = new DrawerMEEssentiaInventoryHandler(
                drawer.getAspectHandler(),
                drawer.getStorageUpgradeSlots());
        }
        return entry.aspects;
    }

    @Nullable
    public static DrawerMEInventoryHandler itemMonitorOf(@Nonnull ControllableDrawerTile drawer) {
        Entry entry = ENTRIES.get(drawer);
        return entry == null ? null : entry.items;
    }

    public static void invalidate(@Nonnull ControllableDrawerTile drawer) {
        Entry entry = ENTRIES.remove(drawer);
        if (entry == null) {
            return;
        }
        if (entry.items != null) {
            entry.items.close();
        }
        if (entry.aspects != null) {
            entry.aspects.close();
        }
    }

    private static Entry entryFor(@Nonnull ControllableDrawerTile drawer) {
        return ENTRIES.computeIfAbsent(drawer, key -> new Entry());
    }

    private static class Entry {

        private DrawerMEInventoryHandler items;
        private DrawerMEEssentiaInventoryHandler aspects;
    }
}
