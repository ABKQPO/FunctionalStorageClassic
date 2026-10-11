package com.hfstudio.functionalstorage.common.presentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
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
        List<Integer> capacities = new ArrayList<>(ordered.size());
        List<Integer> reservations = new ArrayList<>(ordered.size());
        List<Integer> requested = new ArrayList<>(ordered.size());
        Map<ItemStorageKey, AdaptiveDisplayEntry> merged = new LinkedHashMap<>();
        for (ControllableDrawerTile drawer : ordered) {
            int capacity = isPresentableItemDrawer(drawer) ? drawerCapacity(drawer) : 0;
            AdaptiveDisplayProvider provider = drawer instanceof AdaptiveDisplayProvider display ? display : null;
            int reserved = 0;
            int minimumCells = 0;
            for (int cell = 0; cell < capacity; cell++) {
                if (provider != null && provider.isDisplayCellReserved(cell)) {
                    reserved++;
                    minimumCells = cell + 1;
                }
            }
            reservations.add(reserved);
            capacities.add(capacity - reserved);
            int cells = provider == null ? -1 : provider.getRequestedDisplayCells();
            int wanted = cells < 0 ? capacity : Math.min(capacity, Math.max(minimumCells, cells));
            if (wanted == 3) wanted = 4;
            requested.add(Math.max(0, wanted - reserved));
            IBigItemHandler handler = drawer.getItemHandler();
            if (!isPresentableItemDrawer(drawer) || handler == null) continue;
            for (int slot = 0; slot < handler.getStorageCount(); slot++) {
                BigItemStack snapshot = handler.getSnapshot(slot);
                ItemStack template = snapshot.getTemplate();
                ItemStorageKey key = snapshot.getKey();
                if (template == null || key == null) continue;
                AdaptiveDisplayEntry previous = merged.get(key);
                long amount = previous == null ? snapshot.getAmount()
                    : saturatedAdd(
                        previous.content()
                            .getAmount(),
                        snapshot.getAmount());
                long aggregatedCapacity = previous == null ? handler.getCapacity(slot)
                    : saturatedAdd(previous.capacity(), handler.getCapacity(slot));
                merged.put(key, new AdaptiveDisplayEntry(new BigItemStack(template, amount), aggregatedCapacity));
            }
        }
        List<AdaptiveDisplayEntry> entries = new ArrayList<>(merged.values());
        AdaptivePresentationRegistry.sort(sortId)
            .sort(entries);
        Set<ItemStorageKey> reservedKeys = new HashSet<>();
        for (ControllableDrawerTile drawer : ordered) {
            if (!(drawer instanceof AdaptiveDisplayProvider provider)) continue;
            for (int cell = 0; cell < 4; cell++) {
                ItemStack template = provider.getDisplayReservation(cell);
                if (provider.isDisplayCellReserved(cell) && template != null)
                    reservedKeys.add(new ItemStorageKey(template));
            }
        }
        entries.removeIf(
            entry -> reservedKeys.contains(
                entry.content()
                    .getKey()));
        List<Integer> cells = AdaptivePresentationRegistry.layout(layoutId)
            .allocate(entries.size(), capacities, requested);
        Map<Long, List<AdaptiveDisplayEntry>> assignments = new LinkedHashMap<>();
        Set<ItemStorageKey> assigned = new HashSet<>();
        int entryIndex = 0;
        for (int index = 0; index < ordered.size(); index++) {
            ControllableDrawerTile drawer = ordered.get(index);
            int freeCells = Math.min(Math.max(0, cells.get(index)), capacities.get(index));
            int cellCount = freeCells + reservations.get(index);
            AdaptiveDisplayProvider provider = drawer instanceof AdaptiveDisplayProvider display ? display : null;
            if (provider != null) {
                for (int cell = 0; cell < 4; cell++) {
                    if (provider.isDisplayCellReserved(cell)) cellCount = Math.max(cellCount, cell + 1);
                }
            }
            if (cellCount == 3) cellCount = 4;
            List<AdaptiveDisplayEntry> face = new ArrayList<>(cellCount);
            for (int cell = 0; cell < cellCount; cell++) {
                AdaptiveDisplayEntry entry;
                if (provider != null && provider.isDisplayCellReserved(cell)) {
                    ItemStack template = provider.getDisplayReservation(cell);
                    ItemStorageKey key = template == null ? null : new ItemStorageKey(template);
                    entry = key == null ? new AdaptiveDisplayEntry(BigItemStack.empty(), 0L)
                        : merged.getOrDefault(key, new AdaptiveDisplayEntry(new BigItemStack(template, 0L), 0L));
                } else {
                    entry = freeCells > 0 && entryIndex < entries.size() ? entries.get(entryIndex++)
                        : new AdaptiveDisplayEntry(BigItemStack.empty(), 0L);
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
        for (ItemStorageKey key : merged.keySet()) if (!assigned.contains(key)) unassigned++;
        return new AdaptivePresentationPlan(Map.copyOf(assignments), unassigned);
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
                NBTTagCompound stored = new NBTTagCompound();
                ItemStack template = entry.content()
                    .getTemplate();
                if (template != null) stored.setTag("Template", template.writeToNBT(new NBTTagCompound()));
                stored.setLong(
                    "Amount",
                    entry.content()
                        .getAmount());
                stored.setLong("Capacity", entry.capacity());
                entries.appendTag(stored);
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
                NBTTagCompound entry = entries.getCompoundTagAt(cell);
                ItemStack template = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Template"));
                face.add(
                    new AdaptiveDisplayEntry(
                        new BigItemStack(template, Math.max(0L, entry.getLong("Amount"))),
                        Math.max(0L, entry.getLong("Capacity"))));
            }
            assignments.put(drawer.getLong("Position"), List.copyOf(face));
        }
        return new AdaptivePresentationPlan(Map.copyOf(assignments), Math.max(0, tag.getInteger("Unassigned")));
    }

    private static int drawerCapacity(ControllableDrawerTile drawer) {
        return drawer instanceof AdaptiveDisplayProvider ? 4 : 0;
    }

    private static boolean isPresentableItemDrawer(ControllableDrawerTile drawer) {
        return drawer instanceof AdaptiveDisplayProvider && drawer.getItemHandler() != null;
    }

    private static long positionOf(ControllableDrawerTile drawer) {
        return ConnectedDrawerScope.pack(drawer.xCoord, drawer.yCoord, drawer.zCoord);
    }

    private static long saturatedAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - Math.max(0L, right)) return Long.MAX_VALUE;
        return left + Math.max(0L, right);
    }
}
