package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ContainerExchange {

    private ContainerExchange() {}

    public static void completeCursor(EntityPlayer player, ItemStack result) {
        complete(player, player.inventory.getItemStack(), player.inventory::setItemStack, result);
    }

    public static void complete(EntityPlayer player, ItemStack source, Consumer<ItemStack> replace, ItemStack result) {
        if (source == null) return;
        if (source.stackSize == 1) {
            replace.accept(result);
        } else {
            source.stackSize--;
            if (result != null && !player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
        }
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
