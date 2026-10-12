package com.hfstudio.functionalstorage.util;

import static com.gtnewhorizon.gtnhlib.util.ItemUtil.getItemSink;
import static com.gtnewhorizon.gtnhlib.util.ItemUtil.getItemSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InsertionItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSink;
import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

public class TransferUtil {

    public static boolean pullItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity source,
                                    @Nonnull ForgeDirection sourceSide, long limit) {
        if (source == null || sourceSide == ForgeDirection.UNKNOWN || limit <= 0) {
            return false;
        }
        ItemSource itemSource = getItemSource(source, sourceSide);
        if (itemSource == null) {
            return false;
        }
        itemSource.resetSource();
        InventoryIterator slots = itemSource.sourceIterator();
        if (slots == null) {
            return false;
        }
        boolean moved = false;
        while (limit > 0 && slots.hasNext()) {
            ImmutableItemStack available = slots.next();
            if (available == null || available.isEmpty()) {
                continue;
            }
            BigItemStack probe = new BigItemStack(available.toStackFast(), Math.min(limit, available.getStackSize()));
            int target = drawer.pickInsertionIndex(probe);
            if (target < 0) {
                continue;
            }
            long accepted = drawer.insert(target, probe, StorageAction.SIMULATE)
                    .getProcessedAmount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack extracted = slots.extract(nativeAmount(accepted), false);
            if (extracted == null || extracted.getItem() == null || extracted.stackSize <= 0) {
                continue;
            }
            long stored = drawer.insert(target, new BigItemStack(extracted, extracted.stackSize), StorageAction.EXECUTE)
                    .getProcessedAmount();
            if (stored < extracted.stackSize) {
                // A forced iterator insertion merges with the source remainder, including output-only slots.
                slots.insert(new InsertionItemStack(extracted, extracted.stackSize - (int) stored), true);
            }
            moved |= stored > 0;
            limit -= stored;
        }
        return moved;
    }

    public static boolean pushItems(@Nonnull IBigItemHandler drawer, @Nullable TileEntity target,
                                    @Nonnull ForgeDirection targetSide, long limit) {
        if (target == null || targetSide == ForgeDirection.UNKNOWN || limit <= 0) {
            return false;
        }
        ItemSink sink = getItemSink(target, targetSide);
        if (sink == null) {
            return false;
        }
        sink.resetSink();
        long remaining = limit;
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
            int request = nativeAmount(drawer.extractRouted(
                    new BigItemStack(template, Math.min(remaining, view.getSnapshot().getAmount())),
                    StorageAction.SIMULATE).getProcessedAmount());
            if (request <= 0) {
                continue;
            }
            ItemStack probe = template.copy();
            probe.stackSize = request;
            int inserted = request - sink.store(new InsertionItemStack(probe));
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
                                    @Nonnull ForgeDirection sourceSide, long limit) {
        IFluidHandler handler = asFluidHandler(source);
        if (handler == null || limit <= 0) {
            return false;
        }
        FluidStack drained = handler.drain(sourceSide, nativeAmount(limit), false);
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
                                    @Nonnull ForgeDirection targetSide, long limit) {
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
            request.amount = nativeAmount(Math.min(limit, snapshot.getAmount()));
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

    private static int nativeAmount(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, amount));
    }

    @Nullable
    public static ItemStack insertIntoInventory(@Nonnull IInventory inventory, int[] slots, @Nonnull ItemStack stack) {
        return insertIntoInventory(inventory, slots, stack, ForgeDirection.UNKNOWN);
    }

    @Nullable
    public static ItemStack insertIntoInventory(@Nonnull IInventory inventory, int[] slots, @Nonnull ItemStack stack,
                                                @Nonnull ForgeDirection side) {
        if (stack.stackSize <= 0) {
            return null;
        }
        InventoryItemSink sink = new InventoryItemSink(inventory, side);
        sink.setAllowedSinkSlots(slots);
        int rejected = sink.store(new InsertionItemStack(stack));
        if (rejected <= 0) {
            return null;
        }
        ItemStack remaining = stack.copy();
        remaining.stackSize = rejected;
        return remaining;
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
            return slots == null ? new int[0] : slots;
        }
        int size = inventory.getSizeInventory();
        int[] slots = new int[size];
        for (int index = 0; index < size; index++) {
            slots[index] = index;
        }
        return slots;
    }
}
