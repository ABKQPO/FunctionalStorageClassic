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

/** Exchanges containers with exactly the tank clicked by the player. */
public class FluidContainerInteraction {

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
        if (held == null || slot < 0 || slot >= handler.getStorageCount()) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            return exchangeMutable(handler, slot, single, container, exchange);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            BigFluidStack request = new BigFluidStack(contained, contained.amount);
            if (handler.insert(slot, request, StorageAction.SIMULATE)
                .isComplete()) {
                handler.insert(slot, request, StorageAction.EXECUTE);
                exchange.accept(FluidContainerRegistry.drainFluidContainer(single));
                return true;
            }
            return false;
        }
        if (!FluidContainerRegistry.isEmptyContainer(single)) {
            return false;
        }
        FluidStack available = handler.getSnapshot(slot)
            .toFluidStack();
        ItemStack filled = available == null ? null : FluidContainerRegistry.fillFluidContainer(available, single);
        if (filled != null) {
            FluidStack content = FluidContainerRegistry.getFluidForFilledItem(filled);
            if (content != null && handler.extract(slot, content.amount, StorageAction.SIMULATE)
                .isComplete()) {
                handler.extract(slot, content.amount, StorageAction.EXECUTE);
                exchange.accept(filled);
                return true;
            }
        }
        return false;
    }

    private static boolean isFluidContainer(ItemStack stack) {
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
