package com.hfstudio.functionalstorage.common.presentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class AdaptivePresentationPlan {

    private static final AdaptivePresentationPlan EMPTY = new AdaptivePresentationPlan(Map.of(), 0);

    private final Map<Long, List<AdaptiveDisplayEntry>> entriesByDrawer;
    private final int unassignedResourceCount;

    private AdaptivePresentationPlan(Map<Long, List<AdaptiveDisplayEntry>> entriesByDrawer,
        int unassignedResourceCount) {
        this.entriesByDrawer = entriesByDrawer;
        this.unassignedResourceCount = unassignedResourceCount;
    }

    public int getUnassignedResourceCount() {
        return unassignedResourceCount;
    }

    @Nonnull
    public static AdaptivePresentationPlan empty() {
        return EMPTY;
    }

    @Nonnull
    public static AdaptivePresentationPlan build(@Nonnull List<ControllableDrawerTile> drawers, @Nonnull String sortId,
        @Nonnull String layoutId, @Nonnull String orderId) {
        List<ControllableDrawerTile> ordered = new ArrayList<>(drawers);
        AdaptivePresentationRegistry.order(orderId)
            .sort(ordered);
        Map<String, List<ControllableDrawerTile>> groups = new LinkedHashMap<>();
        for (ControllableDrawerTile drawer : ordered) {
            if (!(drawer instanceof AdaptiveDisplayProvider provider)) continue;
            groups.computeIfAbsent(
                provider.getDisplayResource()
                    .getId(),
                ignored -> new ArrayList<>())
                .add(drawer);
        }
        Map<Long, List<AdaptiveDisplayEntry>> assignments = new LinkedHashMap<>();
        int unassigned = 0;
        for (List<ControllableDrawerTile> group : groups.values()) {
            unassigned += assign(group, sortId, layoutId, assignments);
        }
        return new AdaptivePresentationPlan(Map.copyOf(assignments), unassigned);
    }

    private static int assign(List<ControllableDrawerTile> ordered, String sortId, String layoutId,
        Map<Long, List<AdaptiveDisplayEntry>> assignments) {
        List<Integer> capacities = new ArrayList<>(ordered.size());
        List<Integer> reservations = new ArrayList<>(ordered.size());
        List<Integer> requested = new ArrayList<>(ordered.size());
        Map<StorageKey, AdaptiveDisplayEntry> merged = new LinkedHashMap<>();
        for (ControllableDrawerTile drawer : ordered) {
            int capacity = 4;
            AdaptiveDisplayProvider provider = (AdaptiveDisplayProvider) drawer;
            int reserved = 0;
            int minimumCells = 0;
            for (int cell = 0; cell < capacity; cell++) {
                if (provider.isDisplayCellReserved(cell)) {
                    reserved++;
                    minimumCells = cell + 1;
                }
            }
            reservations.add(reserved);
            capacities.add(drawer.isLocked() ? 0 : capacity - reserved);
            int cells = provider.getRequestedDisplayCells();
            int wanted = cells < 0 ? capacity : Math.min(capacity, Math.max(minimumCells, cells));
            if (wanted == 3) wanted = 4;
            requested.add(Math.max(0, wanted - reserved));
            IStorageHandler<?, ?> handler = drawer.getActiveStorage();
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getStorageCount(); slot++) {
                StorageSnapshot<?, ?> snapshot = handler.getSnapshot(slot);
                StorageKey key = snapshot.getKey();
                if (!snapshot.hasTemplate() || key == null) continue;
                AdaptiveDisplayEntry previous = merged.get(key);
                long amount = previous == null ? snapshot.getAmount()
                    : saturatedAdd(
                        previous.content()
                            .getAmount(),
                        snapshot.getAmount());
                long aggregatedCapacity = previous == null ? handler.getCapacity(slot)
                    : saturatedAdd(previous.capacity(), handler.getCapacity(slot));
                merged.put(key, new AdaptiveDisplayEntry(snapshot.withAmount(amount), aggregatedCapacity));
            }
        }
        List<AdaptiveDisplayEntry> entries = new ArrayList<>(merged.values());
        AdaptivePresentationRegistry.sort(sortId)
            .sort(entries);
        Set<StorageKey> reservedKeys = new HashSet<>();
        for (ControllableDrawerTile drawer : ordered) {
            if (!(drawer instanceof AdaptiveDisplayProvider provider)) continue;
            for (int cell = 0; cell < 4; cell++) {
                StorageSnapshot<?, ?> template = provider.getDisplayReservation(cell);
                if (provider.isDisplayCellReserved(cell) && template != null && template.hasTemplate())
                    reservedKeys.add(template.getKey());
            }
        }
        entries.removeIf(
            entry -> reservedKeys.contains(
                entry.content()
                    .getKey()));
        List<Integer> cells = AdaptivePresentationRegistry.layout(layoutId)
            .allocate(entries.size(), capacities, requested);
        Set<StorageKey> assigned = new HashSet<>();
        int entryIndex = 0;
        for (int index = 0; index < ordered.size(); index++) {
            ControllableDrawerTile drawer = ordered.get(index);
            int freeCells = Math.min(Math.max(0, cells.get(index)), capacities.get(index));
            int cellCount = freeCells + reservations.get(index);
            AdaptiveDisplayProvider provider = (AdaptiveDisplayProvider) drawer;
            for (int cell = 0; cell < 4; cell++) {
                if (provider.isDisplayCellReserved(cell)) cellCount = Math.max(cellCount, cell + 1);
            }
            if (cellCount == 3) cellCount = 4;
            AdaptiveDisplayEntry empty = new AdaptiveDisplayEntry(
                provider.getDisplayResource()
                    .empty(),
                0L);
            List<AdaptiveDisplayEntry> face = new ArrayList<>(cellCount);
            for (int cell = 0; cell < cellCount; cell++) {
                AdaptiveDisplayEntry entry;
                if (provider.isDisplayCellReserved(cell)) {
                    StorageSnapshot<?, ?> template = provider.getDisplayReservation(cell);
                    StorageKey key = template == null ? null : template.getKey();
                    entry = key == null ? empty
                        : merged.getOrDefault(key, new AdaptiveDisplayEntry(template.withAmount(0L), 0L));
                } else {
                    entry = freeCells > 0 && entryIndex < entries.size() ? entries.get(entryIndex++) : empty;
                    if (freeCells > 0) freeCells--;
                }
                if (entry.content()
                    .hasTemplate())
                    assigned.add(
                        entry.content()
                            .getKey());
                face.add(entry);
            }
            assignments.put(positionOf(drawer), List.copyOf(face));
        }
        int unassigned = 0;
        for (StorageKey key : merged.keySet()) if (!assigned.contains(key)) unassigned++;
        return unassigned;
    }

    @Nonnull
    public List<AdaptiveDisplayEntry> entries(long position) {
        return entriesByDrawer.getOrDefault(position, List.of());
    }

    @Nonnull
    public DrawerFaceLayout layout(long position) {
        int count = Math.min(DrawerLayout.X_4.getSlotCount(), entries(position).size());
        if (count <= DrawerLayout.X_1.getSlotCount()) {
            return DrawerFaceLayout.X_1;
        }
        if (count == DrawerLayout.X_2.getSlotCount()) {
            return DrawerFaceLayout.X_2;
        }
        return DrawerFaceLayout.X_4;
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Unassigned", unassignedResourceCount);
        NBTTagList drawers = new NBTTagList();
        for (Map.Entry<Long, List<AdaptiveDisplayEntry>> assignment : entriesByDrawer.entrySet()) {
            NBTTagCompound drawer = new NBTTagCompound();
            drawer.setLong("Position", assignment.getKey());
            NBTTagList entries = new NBTTagList();
            for (AdaptiveDisplayEntry entry : assignment.getValue()) {
                entries.appendTag(entry.serializeNBT());
            }
            drawer.setTag("Entries", entries);
            drawers.appendTag(drawer);
        }
        tag.setTag("Drawers", drawers);
        return tag;
    }

    public static AdaptivePresentationPlan fromNBT(NBTTagCompound tag) {
        Map<Long, List<AdaptiveDisplayEntry>> assignments = new LinkedHashMap<>();
        NBTTagList drawers = tag.getTagList("Drawers", 10);
        for (int index = 0; index < drawers.tagCount(); index++) {
            NBTTagCompound drawer = drawers.getCompoundTagAt(index);
            NBTTagList entries = drawer.getTagList("Entries", 10);
            List<AdaptiveDisplayEntry> face = new ArrayList<>();
            for (int cell = 0; cell < Math.min(4, entries.tagCount()); cell++) {
                face.add(AdaptiveDisplayEntry.fromNBT(entries.getCompoundTagAt(cell)));
            }
            assignments.put(drawer.getLong("Position"), List.copyOf(face));
        }
        return new AdaptivePresentationPlan(Map.copyOf(assignments), Math.max(0, tag.getInteger("Unassigned")));
    }

    private static long positionOf(ControllableDrawerTile drawer) {
        return ConnectedDrawerScope.pack(drawer.xCoord, drawer.yCoord, drawer.zCoord);
    }

    private static long saturatedAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - Math.max(0L, right)) return Long.MAX_VALUE;
        return left + Math.max(0L, right);
    }
}
