package com.hfstudio.functionalstorage.mixins.late.jabba;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import mcp.mobius.betterbarrels.common.items.dolly.ItemBarrelMover;

@Mixin(value = ItemBarrelMover.class, remap = false)
public class MixinItemBarrelMover {

    @Inject(method = "isTEMovable", at = @At("HEAD"), cancellable = true, remap = false)
    private void functionalStorage$isTEMovable(TileEntity tileEntity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (tileEntity instanceof ControllableDrawerTile) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(
        method = "pickupContainer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeTileEntity(III)V"),
        remap = false)
    private void functionalStorage$detach(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
        CallbackInfoReturnable<Boolean> callbackInfo) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof ControllableDrawerTile drawer) {
            drawer.detachFromController(world);
        }
    }
}
