package com.hfstudio.functionalstorage.mixins.late.findit;

import java.util.List;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.world.ChunkPosition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnh.findit.service.blockfinder.BlockFoundResponse;
import com.gtnh.findit.service.blockfinder.ClientBlockFindService;
import com.hfstudio.functionalstorage.client.integration.findit.DrawerNetworkHighlightOverlay;

@Mixin(value = ClientBlockFindService.class, remap = false)
public class MixinClientBlockFindService {

    @Inject(method = "handleResponse", at = @At("TAIL"), remap = false)
    private void functionalStorage$extendToLinkedDrawers(EntityClientPlayerMP player, BlockFoundResponse response,
        CallbackInfo callbackInfo) {
        if (player == null || player.worldObj == null) {
            return;
        }
        List<ChunkPosition> positions = response.getPositions();
        if (positions == null || positions.isEmpty()) {
            return;
        }
        DrawerNetworkHighlightOverlay overlay = DrawerNetworkHighlightOverlay.active();
        if (overlay == null) {
            return;
        }
        for (ChunkPosition position : positions) {
            overlay.extend(player.worldObj, position);
        }
    }
}
