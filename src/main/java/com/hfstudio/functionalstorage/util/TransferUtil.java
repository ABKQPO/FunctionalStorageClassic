package com.hfstudio.functionalstorage.util;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

/**
 * Cross-block item and fluid transfer used by the pulling, pushing, and
 * collecting upgrades. Everything goes through vanilla {@code IInventory} and
 * Forge {@code IFluidHandler}, which are the only common 1.7.10 contracts, so
 * these upgrades work with any container mod without special casing.
 */
public class TransferUtil {

    private TransferUtil() {
    }

    /**
     * Moves items from an external block into a drawer.
     *
     * @param drawer      destination drawer
     * @param source      source tile entity, or {@code null}
     * @param sourceSide  side of the source being accessed
     * @param limit       maximum number of items to move
     * @return whether anything moved
     */
    public static boolean pullItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity source, @Nonnull ForgeDirection sourceSide, int limit) {
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
            ItemStack leftover = drawer.insertItem(0, extracted, false);
            moved = true;
            if (leftover != null && leftover.stackSize > 0) {
                inventory.setInventorySlotContents(slot, leftover);
                break;
            }
            limit -= extracted.stackSize;
        }
        return moved;
    }

    /**
     * Moves items from a drawer into an external block.
     *
     * @param drawer     source drawer
     * @param target     destination tile entity, or {@code null}
     * @param targetSide side of the target being accessed
     * @param limit      maximum number of items to move
     * @return whether anything moved
     */
    public static boolean pushItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity target, @Nonnull ForgeDirection targetSide, int limit) {
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
            int request = (int) Math.min(remaining, Math.min(Integer.MAX_VALUE, view.getSnapshot()
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

    /**
     * Moves fluid from an external block into a drawer.
     *
     * @param drawer     destination drawer
     * @param source     source tile entity, or {@code null}
     * @param sourceSide side of the source being accessed
     * @param limit      maximum millibuckets to move
     * @return whether anything moved
     */
    public static boolean pullFluid(@Nonnull IBigFluidHandler drawer, @Nullable TileEntity source, @Nonnull ForgeDirection sourceSide, int limit) {
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

    /**
     * Moves fluid from a drawer into an external block.
     *
     * @param drawer     source drawer
     * @param target     destination tile entity, or {@code null}
     * @param targetSide side of the target being accessed
     * @param limit      maximum millibuckets to move
     * @return whether anything moved
     */
    public static boolean pushFluid(@Nonnull IBigFluidHandler drawer, @Nullable TileEntity target, @Nonnull ForgeDirection targetSide, int limit) {
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

    /**
     * @param tile candidate tile entity
     * @return the tile as an inventory, or {@code null}
     */
    @Nullable
    public static IInventory asInventory(@Nullable TileEntity tile) {
        return tile instanceof IInventory ? (IInventory) tile : null;
    }

    /**
     * @param tile candidate tile entity
     * @return the tile as a fluid handler, or {@code null}
     */
    @Nullable
    public static IFluidHandler asFluidHandler(@Nullable TileEntity tile) {
        return tile instanceof IFluidHandler ? (IFluidHandler) tile : null;
    }

    /**
     * Resolves the slots of an inventory reachable from one side.
     *
     * @param inventory inventory to inspect
     * @param side      side being accessed
     * @return the reachable slot indices
     */
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
