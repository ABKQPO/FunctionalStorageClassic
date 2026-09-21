package com.hfstudio.functionalstorage.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

/** Transfers through native IInventory and IFluidHandler contracts for cross-mod compatibility. */
public class TransferUtil {

    private TransferUtil() {}

    public static boolean pullItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity source,
        @Nonnull ForgeDirection sourceSide, int limit) {
        IInventory inventory = asInventory(source);
        if (inventory == null || limit <= 0) {
            return false;
        }
        int[] slots = accessibleSlots(inventory, sourceSide);
        boolean moved = false;
        for (int slot : slots) {
            if (limit <= 0) {
                break;
            }
            ItemStack available = inventory.getStackInSlot(slot);
            if (available == null || available.getItem() == null) {
                continue;
            }
            int request = Math.min(limit, available.stackSize);
            ItemStack extracted = inventory.decrStackSize(slot, request);
            if (extracted == null || extracted.getItem() == null) {
                continue;
            }
            // Pulled items merge into the slot already holding that item; a full
            // matching slot ends the pass instead of opening an empty neighbour.
            BigItemStack probe = new BigItemStack(extracted, extracted.stackSize);
            int target = drawer.pickInsertionIndex(probe);
            if (target < 0) {
                inventory.setInventorySlotContents(slot, extracted);
                break;
            }
            int stored = (int) drawer.insert(target, probe, StorageAction.EXECUTE)
                .getProcessedAmount();
            if (stored < extracted.stackSize) {
                ItemStack leftover = extracted.copy();
                leftover.stackSize = extracted.stackSize - stored;
                inventory.setInventorySlotContents(slot, leftover);
                if (stored > 0) {
                    moved = true;
                }
                limit -= stored;
                break;
            }
            moved = true;
            limit -= stored;
        }
        return moved;
    }

    public static boolean pushItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity target,
        @Nonnull ForgeDirection targetSide, int limit) {
        IInventory inventory = asInventory(target);
        if (inventory == null || limit <= 0) {
            return false;
        }
        int[] slots = accessibleSlots(inventory, targetSide);
        int remaining = limit;
        boolean moved = false;
        for (ItemStorageView view : ItemStorageView.storages(drawer)) {
            if (remaining <= 0) {
                break;
            }
            ItemStack template = view.getSnapshot()
                .getTemplate();
            if (template == null) {
                continue;
            }
            int request = (int) Math.min(
                remaining,
                Math.min(
                    Integer.MAX_VALUE,
                    view.getSnapshot()
                        .getAmount()));
            if (request <= 0) {
                continue;
            }
            ItemStack probe = template.copy();
            probe.stackSize = request;
            ItemStack leftover = insertIntoInventory(inventory, slots, probe);
            int inserted = request - (leftover == null ? 0 : leftover.stackSize);
            if (inserted <= 0) {
                continue;
            }
            drawer.extractRouted(new BigItemStack(template, inserted), StorageAction.EXECUTE);
            remaining -= inserted;
            moved = true;
        }
        return moved;
    }

    public static boolean pullFluid(@Nonnull IBigFluidHandler drawer, @Nullable TileEntity source,
        @Nonnull ForgeDirection sourceSide, int limit) {
        IFluidHandler handler = asFluidHandler(source);
        if (handler == null || limit <= 0) {
            return false;
        }
        FluidStack drained = handler.drain(sourceSide, limit, false);
        if (drained == null || drained.amount <= 0) {
            return false;
        }
        int accepted = drawer.fill(drained, false);
        if (accepted <= 0) {
            return false;
        }
        FluidStack taken = handler.drain(sourceSide, accepted, true);
        if (taken == null || taken.amount <= 0) {
            return false;
        }
        drawer.fill(taken, true);
        return true;
    }

    public static boolean pushFluid(@Nonnull IBigFluidHandler drawer, @Nullable TileEntity target,
        @Nonnull ForgeDirection targetSide, int limit) {
        IFluidHandler handler = asFluidHandler(target);
        if (handler == null || limit <= 0) {
            return false;
        }
        for (int index = 0; index < drawer.getStorageCount(); index++) {
            BigFluidStack snapshot = drawer.getSnapshot(index);
            FluidStack template = snapshot.getTemplate();
            if (template == null || snapshot.isEmpty()) {
                continue;
            }
            FluidStack request = template.copy();
            request.amount = (int) Math.min(limit, Math.min(Integer.MAX_VALUE, snapshot.getAmount()));
            int accepted = handler.fill(targetSide, request, false);
            if (accepted <= 0) {
                continue;
            }
            FluidStack taken = drawer.drainRouted(new BigFluidStack(template, accepted), StorageAction.EXECUTE)
                .getProcessed()
                .toFluidStack();
            if (taken == null || taken.amount <= 0) {
                continue;
            }
            handler.fill(targetSide, taken, true);
            return true;
        }
        return false;
    }

    /**
     * Inserts a stack into the first inventory slot that accepts it.
     *
     * @param inventory destination inventory
     * @param slots     candidate slot indices
     * @param stack     stack to insert
     * @return the leftover stack, or {@code null} when everything fit
     */
    @Nullable
    public static ItemStack insertIntoInventory(@Nonnull IInventory inventory, int[] slots, @Nonnull ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot : slots) {
            if (remaining.stackSize <= 0) {
                return null;
            }
            if (!inventory.isItemValidForSlot(slot, remaining)) {
                continue;
            }
            ItemStack existing = inventory.getStackInSlot(slot);
            int limit = Math.min(inventory.getInventoryStackLimit(), remaining.getMaxStackSize());
            if (existing == null || existing.getItem() == null) {
                int moved = Math.min(limit, remaining.stackSize);
                ItemStack placed = remaining.copy();
                placed.stackSize = moved;
                inventory.setInventorySlotContents(slot, placed);
                remaining.stackSize -= moved;
            } else if (ItemUtil.areItemStacksEqual(existing, remaining)) {
                int space = limit - existing.stackSize;
                if (space <= 0) {
                    continue;
                }
                int moved = Math.min(space, remaining.stackSize);
                existing.stackSize += moved;
                inventory.setInventorySlotContents(slot, existing);
                remaining.stackSize -= moved;
            }
        }
        return remaining.stackSize <= 0 ? null : remaining;
    }

    @Nullable
    public static IInventory asInventory(@Nullable TileEntity tile) {
        return tile instanceof IInventory ? (IInventory) tile : null;
    }

    @Nullable
    public static IFluidHandler asFluidHandler(@Nullable TileEntity tile) {
        return tile instanceof IFluidHandler ? (IFluidHandler) tile : null;
    }

    @Nonnull
    public static int[] accessibleSlots(@Nonnull IInventory inventory, @Nonnull ForgeDirection side) {
        if (inventory instanceof ISidedInventory) {
            int[] slots = ((ISidedInventory) inventory).getAccessibleSlotsFromSide(side.ordinal());
            if (slots != null) {
                return slots;
            }
        }
        int size = inventory.getSizeInventory();
        int[] slots = new int[size];
        for (int index = 0; index < size; index++) {
            slots[index] = index;
        }
        return slots;
    }
}
