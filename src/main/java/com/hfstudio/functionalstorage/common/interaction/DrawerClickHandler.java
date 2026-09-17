package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.item.LinkingToolItem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class DrawerClickHandler {

    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held != null && held.getItem() instanceof LinkingToolItem tool
            && tool.captureFrequency(held, event.entityPlayer, event.world, event.x, event.y, event.z)) {
            event.setCanceled(true);
            return;
        }
        if (!event.entityPlayer.capabilities.isCreativeMode || event.entityPlayer.isSneaking()) {
            return;
        }
        Block block = event.world.getBlock(event.x, event.y, event.z);
        if (!(block instanceof DrawerBlock drawer)
            || event.face != DrawerBlock.getFrontFacing(event.world.getBlockMetadata(event.x, event.y, event.z))
                .ordinal()) {
            return;
        }
        event.setCanceled(true);
        if (!event.world.isRemote) {
            drawer.onBlockClicked(event.world, event.x, event.y, event.z, event.entityPlayer);
        }
    }
}
