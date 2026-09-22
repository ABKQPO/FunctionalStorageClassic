package com.hfstudio.functionalstorage.mixins.late.bogosorter;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.bogosorter.common.dropoff.DropOffHandler;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

@Mixin(value = DropOffHandler.class, remap = false)
public class MixinDropOffHandler {

    @Shadow
    @Final
    private ItemStack[] playerStacks;

    @Inject(method = "movePlayerStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void functionalStorage$movePlayerStack(int playerStackIndex, IInventory toInventory,
        CallbackInfo callbackInfo) {
        if (!(toInventory instanceof ControllableDrawerTile drawer)) {
            return;
        }
        callbackInfo.cancel();
        IBigItemHandler handler = drawer.getItemHandler();
        if (handler == null || playerStackIndex < 0 || playerStackIndex >= playerStacks.length) {
            return;
        }
        ItemStack stack = playerStacks[playerStackIndex];
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return;
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
