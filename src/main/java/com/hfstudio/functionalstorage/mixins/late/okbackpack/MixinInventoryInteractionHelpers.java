package com.hfstudio.functionalstorage.mixins.late.okbackpack;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import ruiseki.okbackpack.common.helpers.InventoryInteractionHelpers;

/**
 * Routes a backpack deposit upgrade through drawer storage instead of the
 * vanilla inventory contract.
 *
 * <p>
 * The upgrade computes the room left in a slot as
 * {@code min(writableLimit, stackSize) - stored}, where the writable limit is
 * the container's own limit clamped to one stack. A drawer legitimately reports
 * more than a stack, so that difference goes negative the moment a slot holds
 * sixty-four or more and the upgrade then skips the slot entirely; a filled
 * drawer can never receive anything. Routing the deposit through
 * {@link IBigItemHandler#insertRouted} removes the arithmetic from the
 * equation, lets a whole network absorb the stack, and keeps the writable limit
 * meaningful for callers that do respect it.
 * </p>
 */
@Mixin(value = InventoryInteractionHelpers.class, remap = false)
public class MixinInventoryInteractionHelpers {

    @Inject(method = "insertIntoInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void functionalStorage$insertIntoInventory(IInventory inventory, ItemStack stack,
        int[] accessibleSlots, boolean simulate, CallbackInfoReturnable<Integer> callbackInfo) {
        if (!(inventory instanceof ControllableDrawerTile drawer)) {
            return;
        }
        IBigItemHandler handler = drawer.getItemHandler();
        if (handler == null || stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            callbackInfo.setReturnValue(0);
            return;
        }
        // The caller hands over a copy of its whole stack and deducts whatever is
        // reported back, so the amount is clamped to what was actually offered.
        BigItemStack request = new BigItemStack(stack, stack.stackSize);
        long inserted = Math.min(
            Math.max(
                0L,
                handler.insertRouted(request, StorageAction.fromSimulation(simulate))
                    .getProcessedAmount()),
            stack.stackSize);
        callbackInfo.setReturnValue((int) inserted);
    }
}
