package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.common.integration.bogosorter.BogoSorterIntegration;

/** Collects equivalent container results without losing stack counts. */
public class ContainerBatchExchange {

    private final EntityPlayer player;
    private final Consumer<ItemStack> replace;
    private ItemStack result;

    public ContainerBatchExchange(EntityPlayer player, Consumer<ItemStack> replace) {
        this.player = player;
        this.replace = replace;
    }

    public static int exchangeInventorySlot(EntityPlayer player, int inventorySlot, int limit,
        Predicate<ItemStack> accepts, BiPredicate<ItemStack, Consumer<ItemStack>> transfer) {
        if (player == null || inventorySlot < 0
            || inventorySlot >= player.inventory.mainInventory.length
            || limit <= 0
            || BogoSorterIntegration.isProtectedSlot(player, inventorySlot)) return 0;
        ItemStack source = player.inventory.getStackInSlot(inventorySlot);
        if (source == null || source.stackSize <= 0 || !accepts.test(source)) return 0;
        ItemStack remaining = source.copy();
        ContainerBatchExchange exchange = new ContainerBatchExchange(
            player,
            stack -> player.inventory.setInventorySlotContents(inventorySlot, stack));
        int transferred = 0;
        while (remaining.stackSize > 0 && transferred < limit && accepts.test(remaining)) {
            ItemStack single = remaining.copy();
            single.stackSize = 1;
            if (!transfer.test(single, exchange::accept)) break;
            remaining.stackSize--;
            transferred++;
        }
        if (transferred > 0) exchange.complete(remaining);
        return transferred;
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
