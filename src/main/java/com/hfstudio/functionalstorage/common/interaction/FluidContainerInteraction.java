package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/**
 * Exchanges containers with one tank. A negative slot asks the storage to
 * choose the tank itself, which is how a controller terminal serves a whole
 * fluid network instead of a single face.
 */
public class FluidContainerInteraction {

    public static final int ROUTED = -1;

    private FluidContainerInteraction() {}

    public static boolean activate(EntityPlayer player, IBigFluidHandler handler, int slot) {
        ItemStack held = player.getHeldItem();
        if (!isFluidContainer(held)) {
            return false;
        }
        if (player.capabilities.isCreativeMode) {
            activate(held, handler, slot, result -> {});
            return true;
        }
        ItemStack remaining = held.copy();
        BatchContainerExchange exchange = new BatchContainerExchange(player);
        int transfers = held.stackSize;
        for (int index = 0; index < transfers; index++) {
            if (!activate(singleContainer(remaining), handler, slot, exchange::accept)) {
                break;
            }
            remaining.stackSize--;
        }
        exchange.complete(remaining);
        return true;
    }

    private static ItemStack singleContainer(ItemStack stack) {
        ItemStack single = stack.copy();
        single.stackSize = 1;
        return single;
    }

    public static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange) {
        if (held == null || handler == null || exchange == null || handler.getStorageCount() <= 0) {
            return false;
        }
        if (slot >= handler.getStorageCount()) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            FluidStack contained = container.getFluid(single);
            if (contained != null && contained.amount > 0) {
                return insertMutable(handler, slot, single, container, contained, exchange)
                    || fillMutable(handler, slot, single, container, exchange);
            }
            return fillMutable(handler, slot, single, container, exchange);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            return insertFilled(handler, slot, single, contained, exchange);
        }
        if (!FluidContainerRegistry.isEmptyContainer(single)) {
            return false;
        }
        return fillRegistered(handler, slot, single, exchange);
    }

    public static boolean deposit(EntityPlayer player, IBigFluidHandler handler, ItemStack source,
        Consumer<ItemStack> replace) {
        if (player == null || !isFilledFluidContainer(source)) {
            return false;
        }
        return activate(source, handler, ROUTED, result -> ContainerExchange.complete(player, source, replace, result));
    }

    public static int depositInventory(EntityPlayer player, IBigFluidHandler handler) {
        if (player == null || handler == null) {
            return 0;
        }
        int transferred = 0;
        for (int index = 0; index < player.inventory.mainInventory.length; index++) {
            int inventorySlot = index;
            ItemStack source = player.inventory.getStackInSlot(index);
            if (deposit(
                player,
                handler,
                source,
                result -> player.inventory.setInventorySlotContents(inventorySlot, result))) {
                transferred++;
            }
        }
        return transferred;
    }

    private static boolean insertFilled(IBigFluidHandler handler, int slot, ItemStack single, FluidStack fluid,
        Consumer<ItemStack> exchange) {
        BigFluidStack request = new BigFluidStack(fluid, fluid.amount);
        ItemStack drained = FluidContainerRegistry.drainFluidContainer(single);
        if (drained == null) {
            return false;
        }
        if (!insert(handler, slot, request, StorageAction.SIMULATE).isComplete()) {
            return false;
        }
        TransferResult<BigFluidStack, FluidStorageKey> executed = insert(handler, slot, request, StorageAction.EXECUTE);
        if (!executed.isComplete()) {
            rollbackInserted(handler, slot, executed);
            return false;
        }
        exchange.accept(drained);
        return true;
    }

    private static boolean insertMutable(IBigFluidHandler handler, int slot, ItemStack single,
        IFluidContainerItem container, FluidStack fluid, Consumer<ItemStack> exchange) {
        BigFluidStack request = new BigFluidStack(fluid, fluid.amount);
        if (!insert(handler, slot, request, StorageAction.SIMULATE).isComplete()) {
            return false;
        }
        FluidStack simulated = container.drain(single, fluid.amount, false);
        if (simulated == null || simulated.amount != fluid.amount || !simulated.isFluidEqual(fluid)) {
            return false;
        }
        TransferResult<BigFluidStack, FluidStorageKey> executed = insert(handler, slot, request, StorageAction.EXECUTE);
        if (!executed.isComplete()) {
            rollbackInserted(handler, slot, executed);
            return false;
        }
        FluidStack drained = container.drain(single, fluid.amount, true);
        if (drained == null || drained.amount != fluid.amount || !drained.isFluidEqual(fluid)) {
            rollbackInserted(handler, slot, executed);
            return false;
        }
        exchange.accept(single);
        return true;
    }

    private static boolean fillRegistered(IBigFluidHandler handler, int slot, ItemStack single,
        Consumer<ItemStack> exchange) {
        for (int index = slot < 0 ? 0 : slot; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            template.amount = Integer.MAX_VALUE;
            ItemStack filled = FluidContainerRegistry.fillFluidContainer(template, single);
            FluidStack content = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
            if (content == null
                || !extract(handler, index, new BigFluidStack(content, content.amount), StorageAction.SIMULATE)
                    .isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                index,
                new BigFluidStack(content, content.amount),
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, index, executed);
                return false;
            }
            exchange.accept(filled);
            return true;
        }
        return false;
    }

    private static boolean fillMutable(IBigFluidHandler handler, int slot, ItemStack single,
        IFluidContainerItem container, Consumer<ItemStack> exchange) {
        for (int index = slot < 0 ? 0 : slot; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            template.amount = Integer.MAX_VALUE;
            int filled = container.fill(single, template, false);
            if (filled <= 0) {
                continue;
            }
            BigFluidStack request = new BigFluidStack(template, filled);
            if (!extract(handler, index, request, StorageAction.SIMULATE).isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                index,
                request,
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, index, executed);
                return false;
            }
            FluidStack content = template.copy();
            content.amount = filled;
            if (container.fill(single, content, true) != filled) {
                rollbackExtracted(handler, index, executed);
                return false;
            }
            exchange.accept(single);
            return true;
        }
        return false;
    }

    public static boolean isFluidContainer(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof IFluidContainerItem
            || FluidContainerRegistry.getFluidForFilledItem(stack) != null
            || FluidContainerRegistry.isEmptyContainer(stack));
    }

    public static boolean isFilledFluidContainer(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if (stack.getItem() instanceof IFluidContainerItem container) {
            FluidStack fluid = container.getFluid(stack);
            return fluid != null && fluid.amount > 0;
        }
        return FluidContainerRegistry.getFluidForFilledItem(stack) != null;
    }

    private static TransferResult<BigFluidStack, FluidStorageKey> insert(IBigFluidHandler handler, int slot,
        BigFluidStack request, StorageAction action) {
        return slot < 0 ? handler.fillRouted(request, action) : handler.insert(slot, request, action);
    }

    private static TransferResult<BigFluidStack, FluidStorageKey> extract(IBigFluidHandler handler, int slot,
        BigFluidStack request, StorageAction action) {
        return slot < 0 ? handler.drainRouted(request, action) : handler.extract(slot, request.getAmount(), action);
    }

    private static void rollbackInserted(IBigFluidHandler handler, int slot,
        TransferResult<BigFluidStack, FluidStorageKey> inserted) {
        if (inserted.getProcessedAmount() > 0L) {
            extract(handler, slot, inserted.getProcessed(), StorageAction.EXECUTE);
        }
    }

    private static void rollbackExtracted(IBigFluidHandler handler, int slot,
        TransferResult<BigFluidStack, FluidStorageKey> extracted) {
        if (extracted.getProcessedAmount() > 0L) {
            insert(handler, slot, extracted.getProcessed(), StorageAction.EXECUTE);
        }
    }

    private static class BatchContainerExchange {

        private final EntityPlayer player;
        private ItemStack result;

        private BatchContainerExchange(EntityPlayer player) {
            this.player = player;
        }

        private void accept(ItemStack exchanged) {
            if (exchanged == null || exchanged.stackSize <= 0) {
                return;
            }
            if (result == null) {
                result = exchanged.copy();
                return;
            }
            if (ItemStack.areItemStacksEqual(result, exchanged)
                && result.stackSize <= result.getMaxStackSize() - exchanged.stackSize) {
                result.stackSize += exchanged.stackSize;
                return;
            }
            store(result);
            result = exchanged.copy();
        }

        private void complete(ItemStack remaining) {
            if (remaining.stackSize > 0) {
                store(result);
                result = remaining;
            }
            if (result != null && result.stackSize > 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
            } else {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }

        private void store(ItemStack stack) {
            if (stack == null || stack.stackSize <= 0) {
                return;
            }
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
    }
}
