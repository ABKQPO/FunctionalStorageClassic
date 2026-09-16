package com.hfstudio.functionalstorage.common.interaction;

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
        if (held == null || slot < 0 || slot >= handler.getStorageCount()) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            exchangeMutable(player, handler, slot, single, container);
            return true;
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            BigFluidStack request = new BigFluidStack(contained, contained.amount);
            if (handler.insert(slot, request, StorageAction.SIMULATE)
                .isComplete()) {
                handler.insert(slot, request, StorageAction.EXECUTE);
                HeldContainerExchange.complete(player, FluidContainerRegistry.drainFluidContainer(single));
            }
            return true;
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
                HeldContainerExchange.complete(player, filled);
            }
        }
        return true;
    }

    private static void exchangeMutable(EntityPlayer player, IBigFluidHandler handler, int slot,
        ItemStack containerStack, IFluidContainerItem container) {
        FluidStack contained = container.getFluid(containerStack);
        if (contained != null && contained.amount > 0) {
            int accepted = (int) handler
                .insert(slot, new BigFluidStack(contained, contained.amount), StorageAction.SIMULATE)
                .getProcessedAmount();
            if (accepted <= 0) {
                return;
            }
            FluidStack drained = container.drain(containerStack, accepted, true);
            if (drained != null && drained.amount > 0
                && drained.amount <= accepted
                && drained.isFluidEqual(contained)) {
                handler.insert(slot, new BigFluidStack(drained, drained.amount), StorageAction.EXECUTE);
                HeldContainerExchange.complete(player, containerStack);
            }
            return;
        }
        FluidStack available = handler.getSnapshot(slot)
            .toFluidStack();
        if (available == null) {
            return;
        }
        int filled = container.fill(containerStack, available, true);
        if (filled > 0 && handler.extract(slot, filled, StorageAction.SIMULATE)
            .isComplete()) {
            handler.extract(slot, filled, StorageAction.EXECUTE);
            HeldContainerExchange.complete(player, containerStack);
        }
    }
}
