package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Replaces one held container and returns any result without discarding a full inventory's overflow. */
public class HeldContainerExchange {

    private HeldContainerExchange() {}

    public static void complete(EntityPlayer player, ItemStack result) {
        ContainerExchange.complete(
            player,
            player.getHeldItem(),
            stack -> player.inventory.setInventorySlotContents(player.inventory.currentItem, stack),
            result);
    }
}
