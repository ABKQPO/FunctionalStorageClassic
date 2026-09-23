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
        int transfers = player.capabilities.isCreativeMode ? 1 : held.stackSize;
        for (int index = 0; index < transfers; index++) {
            if (!activate(
                player.getHeldItem(),
                handler,
                slot,
                result -> HeldContainerExchange.complete(player, result))) {
                break;
            }
        }
        return true;
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
                return insertMutable(handler, slot, single, container, contained, exchange);
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
                || !extract(handler, slot, new BigFluidStack(content, content.amount), StorageAction.SIMULATE)
                    .isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                slot,
                new BigFluidStack(content, content.amount),
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, slot, executed);
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
            if (!extract(handler, slot, request, StorageAction.SIMULATE).isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                slot,
                request,
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, slot, executed);
                return false;
            }
            FluidStack content = template.copy();
            content.amount = filled;
            if (container.fill(single, content, true) != filled) {
                rollbackExtracted(handler, slot, executed);
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
}
