package com.hfstudio.functionalstorage.common.presentation;

import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;

public class AdaptiveDisplaySettings {

    private int reservationMask;
    private final StorageSnapshot<?, ?>[] reservations = new StorageSnapshot<?, ?>[4];
    private final StorageSnapshot<?, ?>[] lockedEntries = new StorageSnapshot<?, ?>[4];
    private int lockedCellCount;

    public int getLockedCellCount() {
        return lockedCellCount;
    }

    public StorageSnapshot<?, ?> getLockedEntry(int cell) {
        return cell >= 0 && cell < lockedEntries.length ? lockedEntries[cell] : null;
    }

    public void captureLock(List<AdaptiveDisplayEntry> entries, boolean locked) {
        Arrays.fill(lockedEntries, null);
        lockedCellCount = locked ? Math.max(1, Math.min(4, entries.size())) : 0;
        for (int cell = 0; locked && cell < Math.min(4, entries.size()); cell++) {
            StorageSnapshot<?, ?> content = entries.get(cell)
                .content();
            if (content.hasTemplate()) lockedEntries[cell] = content.withAmount(0L);
        }
    }

    public boolean isReserved(int cell) {
        return cell >= 0 && cell < reservations.length && (reservationMask & 1 << cell) != 0;
    }

    public StorageSnapshot<?, ?> getReservation(int cell) {
        return cell >= 0 && cell < reservations.length ? reservations[cell] : null;
    }

    public boolean isEmpty() {
        return reservationMask == 0;
    }

    public boolean setReservation(int cell, StorageSnapshot<?, ?> template, boolean reserved) {
        if (cell < 0 || cell >= reservations.length) return false;
        reservationMask = reserved ? reservationMask | 1 << cell : reservationMask & ~(1 << cell);
        reservations[cell] = reserved && template != null && template.hasTemplate() ? template.withAmount(0L) : null;
        return true;
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList locked = new NBTTagList();
        for (int cell = 0; cell < lockedCellCount; cell++) {
            NBTTagCompound entry = lockedEntries[cell] == null ? new NBTTagCompound()
                : new AdaptiveDisplayEntry(lockedEntries[cell], 0L).serializeNBT();
            locked.appendTag(entry);
        }
        tag.setTag("AdaptiveLockedDisplay", locked);
        NBTTagList entries = new NBTTagList();
        for (int cell = 0; cell < reservations.length; cell++) {
            if (!isReserved(cell)) continue;
            NBTTagCompound entry = reservations[cell] == null ? new NBTTagCompound()
                : new AdaptiveDisplayEntry(reservations[cell], 0L).serializeNBT();
            entry.setInteger("Cell", cell);
            entries.appendTag(entry);
        }
        tag.setTag("AdaptiveReservations", entries);
    }

    public void readFromNBT(NBTTagCompound tag) {
        Arrays.fill(lockedEntries, null);
        NBTTagList locked = tag.getTagList("AdaptiveLockedDisplay", 10);
        lockedCellCount = Math.min(4, locked.tagCount());
        for (int cell = 0; cell < lockedCellCount; cell++) {
            StorageSnapshot<?, ?> content = AdaptiveDisplayEntry.fromNBT(locked.getCompoundTagAt(cell))
                .content();
            if (content.hasTemplate()) lockedEntries[cell] = content.withAmount(0L);
        }
        reservationMask = 0;
        Arrays.fill(reservations, null);
        NBTTagList entries = tag.getTagList("AdaptiveReservations", 10);
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            setReservation(
                entry.getInteger("Cell"),
                AdaptiveDisplayEntry.fromNBT(entry)
                    .content(),
                true);
        }
    }
}
