package com.hfstudio.functionalstorage.mixins.late.findit;

import net.minecraft.client.entity.EntityClientPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnh.findit.service.itemfinder.ClientItemFindService;
import com.gtnh.findit.service.itemfinder.ItemFoundResponse;
import com.hfstudio.functionalstorage.client.integration.findit.DrawerNetworkHighlightOverlay;

@Mixin(value = ClientItemFindService.class, remap = false)
public class MixinClientItemFindService {

    @Inject(method = "handleResponse", at = @At("TAIL"), remap = false)
    private void functionalStorage$highlightMatchingDrawers(EntityClientPlayerMP player, ItemFoundResponse response,
        CallbackInfo callbackInfo) {
        DrawerNetworkHighlightOverlay overlay = DrawerNetworkHighlightOverlay.active();
        if (overlay != null && player != null && player.worldObj != null) {
            overlay.highlightMatches(player.worldObj, response.getFoundStack());
        }
    }
}
