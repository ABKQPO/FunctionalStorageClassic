package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;

/** Collects equivalent container results without losing stack counts. */
public class ContainerBatchExchange {

    private final EntityPlayer player;
    private final Consumer<ItemStack> replace;
    private ItemStack result;

    public ContainerBatchExchange(EntityPlayer player, Consumer<ItemStack> replace) {
        this.player = player;
        this.replace = replace;
    }

    public void accept(ItemStack exchanged) {
        if (exchanged == null || exchanged.stackSize <= 0) {
            return;
        }
        if (result == null) {
            result = exchanged.copy();
            return;
        }
        if (BigItemStack.matches(result, exchanged)
            && result.stackSize <= result.getMaxStackSize() - exchanged.stackSize) {
            result.stackSize += exchanged.stackSize;
            return;
        }
        store(result);
        result = exchanged.copy();
    }

    public void complete(ItemStack remaining) {
        if (remaining != null && remaining.stackSize > 0) {
            store(result);
            result = remaining;
        }
        replace.accept(result == null || result.stackSize <= 0 ? null : result);
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }

    private void store(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }
}
