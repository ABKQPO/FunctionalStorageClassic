package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Replaces one held container and returns any result without discarding a full inventory's overflow. */
public class HeldContainerExchange {

    private HeldContainerExchange() {}

    public static void complete(EntityPlayer player, ItemStack result) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held.stackSize == 1) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
        } else {
            held.stackSize--;
            if (result != null && !player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
