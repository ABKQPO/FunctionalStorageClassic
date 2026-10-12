package com.hfstudio.functionalstorage.common.presentation;

import java.util.List;

import javax.annotation.Nullable;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.inventory.base.BigAspectHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem.ConfigurationAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

public interface AdaptiveDisplayProvider {

    ControllableDrawerTile getDisplayOwner();

    StorageResource<?, ?> getDisplayResource();

    AdaptiveDisplaySettings getDisplaySettings();

    @Nullable
    default AdaptiveMatrixTile adaptiveMatrix() {
        return AdaptiveLinking.resolveMatrix(getDisplayOwner());
    }

    default boolean isAdaptiveDisplayPowered() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        return matrix != null && matrix.isPowered();
    }

    default int getAdaptiveModelKey() {
        return getAdaptiveDisplayLayout().getSlotCount() << 1 | (isAdaptiveDisplayPowered() ? 1 : 0);
    }

    default int getRequestedDisplayCells() {
        return switch (getDisplayOwner().getDrawerOptions()
            .getAdvancedValue(ConfigurationAction.ADAPTIVE_CELLS)) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> -1;
        };
    }

    default boolean isDisplayCellReserved(int cell) {
        if (getDisplaySettings().isReserved(cell)) return true;
        if (!getDisplayOwner().isLocked()) return false;
        return cell < getDisplaySettings().getLockedCellCount() || getDisplayReservation(cell) != null;
    }

    @Nullable
    default StorageSnapshot<?, ?> getDisplayReservation(int cell) {
        AdaptiveDisplaySettings settings = getDisplaySettings();
        if (settings.isReserved(cell)) return settings.getReservation(cell);
        if (!getDisplayOwner().isLocked()) return null;
        StorageSnapshot<?, ?> retained = settings.getLockedEntry(cell);
        if (retained != null) return retained;
        for (int index = 0; index < settings.getLockedCellCount(); index++) {
            if (settings.getLockedEntry(index) != null) return null;
        }
        IStorageHandler<?, ?> storage = getDisplayOwner().getActiveStorage();
        StorageSnapshot<?, ?> local = storage == null ? null : storage.getSnapshot(cell);
        return local != null && local.hasTemplate() ? local : null;
    }

    default void captureAdaptiveLock(boolean locked) {
        List<AdaptiveDisplayEntry> entries = locked ? getAdaptiveDisplayEntries() : List.of();
        getDisplaySettings().captureLock(entries, locked);
        if (!locked) return;
        IStorageHandler<?, ?> storage = getDisplayOwner().getActiveStorage();
        if (storage == null) return;
        for (AdaptiveDisplayEntry entry : entries) {
            StorageSnapshot<?, ?> template = entry.content();
            if (!template.hasTemplate()) continue;
            int empty = -1;
            boolean retained = false;
            for (int slot = 0; slot < storage.getStorageCount(); slot++) {
                StorageSnapshot<?, ?> current = storage.getSnapshot(slot);
                if (template.getKey()
                    .equals(current.getKey())) retained = true;
                if (!current.hasTemplate() && empty < 0) empty = slot;
            }
            if (retained || empty < 0) continue;
            if (storage instanceof BigItemHandler items && template instanceof BigItemStack item) {
                items.setSlotFilter(empty, item.withAmount(0L));
            } else if (storage instanceof BigFluidHandler fluids && template instanceof BigFluidStack fluid) {
                fluids.setSlotFilter(empty, fluid.withAmount(0L));
            } else if (storage instanceof BigAspectHandler aspects && template instanceof BigAspectStack aspect) {
                aspects.setSlotFilter(empty, aspect.withAmount(0L));
            }
        }
    }

    default boolean setDisplayReservation(int cell, StorageSnapshot<?, ?> template, boolean reserved) {
        ControllableDrawerTile owner = getDisplayOwner();
        if (owner.getWorldObj() != null && owner.getWorldObj().isRemote) return false;
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (reserved && template != null && matrix != null && !matrix.canReserveDisplayResource(owner, cell, template))
            return false;
        if (!getDisplaySettings().setReservation(cell, template, reserved)) return false;
        owner.markOptionsDirty();
        return true;
    }

    default String configureDisplayCell(int cell, boolean reserveEmpty) {
        if (adaptiveMatrix() == null || cell < 0 || cell >= 4) return "unlinked";
        if (!reserveEmpty && isDisplayCellReserved(cell)) {
            setDisplayReservation(cell, null, false);
            return "cleared";
        }
        List<AdaptiveDisplayEntry> entries = getAdaptiveDisplayEntries();
        StorageSnapshot<?, ?> template = reserveEmpty || cell >= entries.size() ? null
            : entries.get(cell)
                .content();
        return setDisplayReservation(cell, template, true)
            ? template == null || !template.hasTemplate() ? "empty" : "reserved"
            : "conflict";
    }

    default List<AdaptiveDisplayEntry> getAdaptiveDisplayEntries() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null || !matrix.isPowered()) return List.of();
        ControllableDrawerTile owner = getDisplayOwner();
        return matrix.getDisplayEntries(ConnectedDrawerScope.pack(owner.xCoord, owner.yCoord, owner.zCoord));
    }

    default DrawerFaceLayout getAdaptiveDisplayLayout() {
        AdaptiveMatrixTile matrix = adaptiveMatrix();
        if (matrix == null || !matrix.isPowered()) return DrawerFaceLayout.X_1;
        ControllableDrawerTile owner = getDisplayOwner();
        return matrix.getDisplayLayout(ConnectedDrawerScope.pack(owner.xCoord, owner.yCoord, owner.zCoord));
    }
}
