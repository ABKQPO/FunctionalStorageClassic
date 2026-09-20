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
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
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
        IBigItemHandler handler = drawer.getItemHandler();
        ItemStack stack = playerStackIndex < 0 || playerStackIndex >= playerStacks.length ? null
            : playerStacks[playerStackIndex];
        if (handler == null || stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return;
        }

        TransferResult<BigItemStack, ItemStorageKey> result = handler
            .insertRouted(new BigItemStack(stack, stack.stackSize), StorageAction.EXECUTE);
        long processed = result.getProcessedAmount();
        if (processed <= 0L) {
            return;
        }

        int moved = (int) Math.min(processed, stack.stackSize);
        stack.stackSize -= moved;
        if (stack.stackSize <= 0) {
            playerStacks[playerStackIndex] = null;
        }
        callbackInfo.cancel();
    }
}
