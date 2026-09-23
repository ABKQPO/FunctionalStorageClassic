package com.hfstudio.functionalstorage.mixins.late.bogosorter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.bogosorter.common.PinnedSlots;
import com.cleanroommc.bogosorter.common.dropoff.DropOffHandler;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.interaction.FluidContainerInteraction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

@Mixin(value = DropOffHandler.class, remap = false)
public class MixinDropOffHandler {

    @Shadow
    @Final
    private ItemStack[] playerStacks;

    @Shadow
    @Final
    private InventoryPlayer playerInventory;

    @Inject(method = "movePlayerStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void functionalStorage$movePlayerStack(int playerStackIndex, IInventory toInventory,
        CallbackInfo callbackInfo) {
        if (!(toInventory instanceof ControllableDrawerTile drawer)) {
            return;
        }
        callbackInfo.cancel();
        if (PinnedSlots.isPinned(playerInventory.player, playerStackIndex)) {
            return;
        }
        IBigItemHandler handler = drawer.getItemHandler();
        if (handler == null || playerStackIndex < 0 || playerStackIndex >= playerStacks.length) {
            return;
        }
        ItemStack stack = playerStacks[playerStackIndex];
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return;
        }

        if (FunctionalStorageConfig.CLIENT.prioritizeFluidContainerDeposit
            && FluidContainerInteraction.isFilledFluidContainer(stack)) {
            IBigFluidHandler fluidHandler = drawer.getFluidHandler();
            if (fluidHandler != null && fluidHandler.getStorageCount() > 0) {
                EntityPlayer player = playerInventory.player;
                FluidContainerInteraction
                    .deposit(player, fluidHandler, stack, result -> playerStacks[playerStackIndex] = result);
                return;
            }
        }

        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        if (!handler.hasMatchingResource(request)) {
            return;
        }
        // Routing, rather than a single slot, because upstream also opens a fresh
        // slot once a matching pile is full and the container has room to spare.
        long processed = Math.min(
            Math.max(
                0L,
                handler.insertRouted(request, StorageAction.EXECUTE)
                    .getProcessedAmount()),
            stack.stackSize);
        if (processed <= 0L) {
            return;
        }

        stack.stackSize -= (int) processed;
        if (stack.stackSize <= 0) {
            playerStacks[playerStackIndex] = null;
        }
    }
}
