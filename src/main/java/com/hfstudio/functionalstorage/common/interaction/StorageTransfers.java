package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.container.StorageTransferMenu;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.integration.bogosorter.BogoSorterIntegration;

public class StorageTransfers {

    public enum Action {
        PUSH,
        PULL,
        MOVE_ALL,
        MOVE_SAME,
        MOVE_ONE,
        MOVE_ONE_EMPTY
    }

    private final EntityPlayer player;
    private final Container container;
    private final StorageTransferMenu menu;
    private final IBigItemHandler storage;

    private StorageTransfers(EntityPlayer player, Container container, StorageTransferMenu menu) {
        this.player = player;
        this.container = container;
        this.menu = menu;
        this.storage = menu.getTransferStorage();
    }

    public static void execute(EntityPlayer player, int slot, Action action, int amount, boolean reverse) {
        Container container = player.openContainer;
        if (player.worldObj.isRemote || container == null
            || !container.canInteractWith(player)
            || !(container instanceof StorageTransferMenu menu)
            || menu.getTransferStorage() == null
            || slot < 0
            || slot >= container.inventorySlots.size()
            || action == null
            || amount < 1
            || amount > 64) return;
        StorageTransfers transfers = new StorageTransfers(player, container, menu);
        transfers.move(slot, action, amount, reverse);
        player.inventory.markDirty();
        container.detectAndSendChanges();
        if (player instanceof EntityPlayerMP serverPlayer && serverPlayer.playerNetServerHandler != null) {
            serverPlayer.updateHeldItem();
        }
    }

    private void move(int slotNumber, Action action, int amount, boolean reverse) {
        int count = menu.getTransferSlotCount();
        if (count == 0) return;
        Slot hovered = container.getSlot(slotNumber);
        int storedSlot = menu.getTransferSlot(slotNumber);
        boolean fromPlayer = hovered.inventory == player.inventory && hovered.getSlotIndex() >= 0
            && hovered.getSlotIndex() < 36;
        if (!fromPlayer && storedSlot < 0) return;
        boolean wheel = action == Action.PUSH || action == Action.PULL;
        if (wheel) {
            if (fromPlayer) {
                int inventorySlot = hovered.getSlotIndex();
                if (action == Action.PUSH) deposit(inventorySlot, -1, amount, false);
                else withdrawMatching(inventorySlot, amount, reverse);
            } else if (action == Action.PUSH) {
                withdraw(storedSlot, amount, false, false);
            } else {
                ItemStack type = storage.getSnapshot(storedSlot)
                    .getTemplate();
                for (int i = 0; i < 36 && amount > 0; i++) {
                    int index = playerIndex(reverse ? 35 - i : i);
                    if (BigItemStack.matches(type, player.inventory.getStackInSlot(index))) {
                        amount -= deposit(index, storedSlot, amount, false);
                    }
                }
            }
            return;
        }
        if (action == Action.MOVE_ONE || action == Action.MOVE_ONE_EMPTY) {
            boolean emptyOnly = action == Action.MOVE_ONE_EMPTY;
            if (fromPlayer) deposit(hovered.getSlotIndex(), -1, 1, emptyOnly);
            else withdraw(storedSlot, 1, emptyOnly, true);
            return;
        }
        if (!fromPlayer) {
            // Hovering a storage slot moves its contents out to the player. Slots
            // that hold nothing are skipped rather than aborting the whole action,
            // so an empty slot among matches no longer cancels the transfer.
            ItemStack stored = storage.getSnapshot(storedSlot)
                .getTemplate();
            if (stored == null) return;
            for (int index : transferredSlots(count)) {
                ItemStack stack = storage.getSnapshot(index)
                    .getTemplate();
                if (stack != null && (action != Action.MOVE_SAME || BigItemStack.matches(stored, stack))) {
                    withdraw(index, 27 * Math.min(64, stack.getMaxStackSize()), false, true);
                }
            }
            return;
        }
        ItemStack type = hovered.getStack();
        if (type == null) return;
        int first = hovered.getSlotIndex() < 9 ? 0 : 9;
        int end = first == 0 ? 9 : 36;
        for (int index = first; index < end; index++) {
            if (index != hovered.getSlotIndex() && pinned(index)) continue;
            ItemStack stack = player.inventory.getStackInSlot(index);
            if (stack != null && (action != Action.MOVE_SAME || BigItemStack.matches(type, stack))) {
                deposit(index, -1, stack.stackSize, false);
            }
        }
    }

    private int[] transferredSlots(int count) {
        int[] indices = new int[count];
        for (int index = 0; index < count; index++) {
            indices[index] = menu.getTransferSlot(index);
        }
        return indices;
    }

    private int deposit(int playerSlot, int storageSlot, int limit, boolean emptyOnly) {
        ItemStack source = player.inventory.getStackInSlot(playerSlot);
        if (source == null || source.stackSize <= 0) return 0;
        BigItemStack request = new BigItemStack(source, Math.min(limit, source.stackSize));
        int moved = 0;
        if (storageSlot >= 0) {
            moved = (int) storage.insert(storageSlot, request, StorageAction.EXECUTE)
                .getProcessedAmount();
        } else if (!emptyOnly) {
            moved = (int) storage.insertRouted(request, StorageAction.EXECUTE)
                .getProcessedAmount();
        } else {
            for (int index = 0; index < storage.getStorageCount() && moved < request.getAmount(); index++) {
                if (!storage.getSnapshot(index)
                    .hasTemplate()) {
                    moved += (int) storage
                        .insert(index, request.withAmount(request.getAmount() - moved), StorageAction.EXECUTE)
                        .getProcessedAmount();
                }
            }
        }
        if (moved > 0) {
            source.stackSize -= moved;
            if (source.stackSize == 0) player.inventory.setInventorySlotContents(playerSlot, null);
        }
        return moved;
    }

    private void withdrawMatching(int playerSlot, int amount, boolean reverse) {
        ItemStack target = player.inventory.getStackInSlot(playerSlot);
        if (target == null) return;
        int room = Math.min(
            amount,
            Math.min(player.inventory.getInventoryStackLimit(), target.getMaxStackSize()) - target.stackSize);
        int count = menu.getTransferSlotCount();
        for (int i = 0; i < count && room > 0; i++) {
            int index = menu.getTransferSlot(reverse ? count - 1 - i : i);
            if (index < 0) continue;
            if (storage.getSnapshot(index)
                .isSameType(target)) {
                int moved = (int) storage.extract(index, room, StorageAction.EXECUTE)
                    .getProcessedAmount();
                target.stackSize += moved;
                room -= moved;
            }
        }
    }

    private void withdraw(int storageSlot, int amount, boolean emptyOnly, boolean mainOnly) {
        ItemStack type = storage.getSnapshot(storageSlot)
            .getTemplate();
        if (type == null) return;
        int slots = mainOnly ? 27 : 36;
        for (int pass = emptyOnly ? 1 : 0; pass < 2 && amount > 0; pass++) {
            for (int i = 0; i < slots && amount > 0; i++) {
                int index = playerIndex(i);
                if (mainOnly && pinned(index)) continue;
                ItemStack target = player.inventory.getStackInSlot(index);
                if (pass == 0 ? target == null || !BigItemStack.matches(type, target) : target != null) continue;
                int room = Math.min(player.inventory.getInventoryStackLimit(), type.getMaxStackSize())
                    - (target == null ? 0 : target.stackSize);
                if (room <= 0) continue;
                ItemStack extracted = storage.extract(storageSlot, Math.min(amount, room), StorageAction.EXECUTE)
                    .getProcessed()
                    .toItemStack();
                if (extracted == null) return;
                if (target == null) player.inventory.setInventorySlotContents(index, extracted);
                else target.stackSize += extracted.stackSize;
                amount -= extracted.stackSize;
            }
        }
    }

    private boolean pinned(int index) {
        return Mods.InventoryBogoSorter.isModLoaded() && BogoSorterIntegration.isPinned(player, index);
    }

    private static int playerIndex(int position) {
        return position < 27 ? position + 9 : position - 27;
    }
}
