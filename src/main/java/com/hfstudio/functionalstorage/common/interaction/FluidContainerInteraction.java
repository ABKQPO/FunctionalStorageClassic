package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

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
        if (held == null || handler.getStorageCount() <= 0) {
            return false;
        }
        int tank = resolveTank(handler, held, slot);
        if (tank < 0) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            return exchangeMutable(handler, tank, single, container, exchange);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            BigFluidStack request = new BigFluidStack(contained, contained.amount);
            if (handler.insert(tank, request, StorageAction.SIMULATE)
                .isComplete()) {
                handler.insert(tank, request, StorageAction.EXECUTE);
                exchange.accept(FluidContainerRegistry.drainFluidContainer(single));
                return true;
            }
            return false;
        }
        if (!FluidContainerRegistry.isEmptyContainer(single)) {
            return false;
        }
        FluidStack available = handler.getSnapshot(tank)
            .toFluidStack();
        ItemStack filled = available == null ? null : FluidContainerRegistry.fillFluidContainer(available, single);
        if (filled != null) {
            FluidStack content = FluidContainerRegistry.getFluidForFilledItem(filled);
            if (content != null && handler.extract(tank, content.amount, StorageAction.SIMULATE)
                .isComplete()) {
                handler.extract(tank, content.amount, StorageAction.EXECUTE);
                exchange.accept(filled);
                return true;
            }
        }
        return false;
    }

    private static int resolveTank(IBigFluidHandler handler, ItemStack held, int slot) {
        if (slot >= 0) {
            return slot < handler.getStorageCount() ? slot : ROUTED;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            FluidStack contained = container.getFluid(single);
            return contained != null && contained.amount > 0 ? firstAccepting(handler, contained)
                : firstContaining(handler);
        }
        FluidStack filled = FluidContainerRegistry.getFluidForFilledItem(single);
        if (filled != null) {
            return firstAccepting(handler, filled);
        }
        return firstContaining(handler);
    }

    private static int firstAccepting(IBigFluidHandler handler, FluidStack fluid) {
        BigFluidStack request = new BigFluidStack(fluid, Math.max(1, fluid.amount));
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (handler.insert(index, request, StorageAction.SIMULATE)
                .getProcessedAmount() > 0L) {
                return index;
            }
        }
        return -1;
    }

    private static int firstContaining(IBigFluidHandler handler) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (!handler.getSnapshot(index)
                .isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    public static boolean isFluidContainer(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof IFluidContainerItem
            || FluidContainerRegistry.getFluidForFilledItem(stack) != null
            || FluidContainerRegistry.isEmptyContainer(stack));
    }

    private static boolean exchangeMutable(IBigFluidHandler handler, int slot, ItemStack containerStack,
        IFluidContainerItem container, Consumer<ItemStack> exchange) {
        FluidStack contained = container.getFluid(containerStack);
        if (contained != null && contained.amount > 0) {
            int accepted = (int) handler
                .insert(slot, new BigFluidStack(contained, contained.amount), StorageAction.SIMULATE)
                .getProcessedAmount();
            if (accepted <= 0) {
                return false;
            }
            FluidStack drained = container.drain(containerStack, accepted, true);
            if (drained != null && drained.amount > 0
                && drained.amount <= accepted
                && drained.isFluidEqual(contained)) {
                handler.insert(slot, new BigFluidStack(drained, drained.amount), StorageAction.EXECUTE);
                exchange.accept(containerStack);
                return true;
            }
            return false;
        }
        FluidStack available = handler.getSnapshot(slot)
            .toFluidStack();
        if (available == null) {
            return false;
        }
        int filled = container.fill(containerStack, available, true);
        if (filled > 0 && handler.extract(slot, filled, StorageAction.SIMULATE)
            .isComplete()) {
            handler.extract(slot, filled, StorageAction.EXECUTE);
            exchange.accept(containerStack);
            return true;
        }
        return false;
    }
}
