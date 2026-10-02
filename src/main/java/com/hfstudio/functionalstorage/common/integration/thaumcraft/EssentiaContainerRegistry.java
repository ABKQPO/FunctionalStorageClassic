package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.interaction.ContainerBatchExchange;
import com.hfstudio.functionalstorage.common.interaction.ContainerExchange;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

/** Registers explicit container capacities because Thaumcraft's item interface does not expose them. */
public class EssentiaContainerRegistry {

    private static final int ROUTED = -1;

    private static final Map<Item, ContainerDefinition> CONTAINERS = new ConcurrentHashMap<>();

    public record ContainerDefinition(int capacity, int emptyMetadata, int filledMetadata) {

        public ContainerDefinition {
            if (capacity <= 0 || emptyMetadata < 0 || filledMetadata < 0) {
                throw new IllegalArgumentException("Invalid essentia container definition");
            }
        }
    }

    private EssentiaContainerRegistry() {}

    public static void register(Item item, ContainerDefinition definition) {
        if (!(item instanceof IEssentiaContainerItem)) {
            throw new IllegalArgumentException("An essentia container must implement IEssentiaContainerItem");
        }
        CONTAINERS.put(item, definition);
    }

    public static boolean isEssentiaContainer(ItemStack stack) {
        return stack != null && CONTAINERS.containsKey(stack.getItem());
    }

    public static boolean activate(EntityPlayer player, IBigAspectHandler handler, int slot) {
        return activate(player, handler, slot, null);
    }

    public static boolean activate(EntityPlayer player, IBigAspectHandler handler, int slot,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        ItemStack held = player.getHeldItem();
        if (!isEssentiaContainer(held)) {
            return false;
        }
        return activateBatch(
            player,
            held,
            handler,
            slot,
            stack -> player.inventory.setInventorySlotContents(player.inventory.currentItem, stack),
            prepareLockedSlot);
    }

    public static boolean activateCursor(EntityPlayer player, IBigAspectHandler handler, int slot,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isEssentiaContainer(cursor)) {
            return false;
        }
        return activateBatch(player, cursor, handler, slot, player.inventory::setItemStack, prepareLockedSlot);
    }

    private static boolean activateBatch(EntityPlayer player, ItemStack source, IBigAspectHandler handler, int slot,
        Consumer<ItemStack> replace, BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        if (player.capabilities.isCreativeMode) {
            activate(source, handler, slot, result -> {}, prepareLockedSlot);
            return true;
        }
        ItemStack remaining = source.copy();
        ContainerBatchExchange exchange = new ContainerBatchExchange(player, replace);
        int transfers = source.stackSize;
        for (int index = 0; index < transfers; index++) {
            if (!activate(singleContainer(remaining), handler, slot, exchange::accept, prepareLockedSlot)) {
                break;
            }
            remaining.stackSize--;
        }
        exchange.complete(remaining);
        return true;
    }

    private static ItemStack singleContainer(ItemStack stack) {
        ItemStack single = stack.copy();
        single.stackSize = 1;
        return single;
    }

    public static int depositInventory(EntityPlayer player, IBigAspectHandler handler) {
        if (player == null || handler == null) {
            return 0;
        }
        int transferred = 0;
        for (int index = 0; index < player.inventory.mainInventory.length; index++) {
            transferred += depositInventorySlot(player, handler, index);
        }
        return transferred;
    }

    public static int depositInventorySlot(EntityPlayer player, IBigAspectHandler handler, int inventorySlot) {
        return depositInventorySlot(player, handler, inventorySlot, Integer.MAX_VALUE);
    }

    public static int depositInventorySlot(EntityPlayer player, IBigAspectHandler handler, int inventorySlot,
        int limit) {
        if (player == null || handler == null
            || inventorySlot < 0
            || inventorySlot >= player.inventory.mainInventory.length
            || limit <= 0) {
            return 0;
        }
        int transferred = 0;
        ItemStack source = player.inventory.getStackInSlot(inventorySlot);
        while (source != null && transferred < limit && isFilled(source)) {
            ItemStack current = source;
            if (!activate(
                current,
                handler,
                ROUTED,
                result -> ContainerExchange.complete(
                    player,
                    current,
                    stack -> player.inventory.setInventorySlotContents(inventorySlot, stack),
                    result),
                null)) {
                break;
            }
            transferred++;
            source = player.inventory.getStackInSlot(inventorySlot);
        }
        return transferred;
    }

    public static int fillInventory(EntityPlayer player, IBigAspectHandler handler, int slot) {
        return fillInventory(player, handler, slot, Integer.MAX_VALUE);
    }

    public static int fillInventory(EntityPlayer player, IBigAspectHandler handler, int slot, int limit) {
        if (player == null || handler == null || slot < 0 || slot >= handler.getStorageCount() || limit <= 0) {
            return 0;
        }
        int transferred = 0;
        for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length
            && transferred < limit; inventorySlot++) {
            ItemStack source = player.inventory.getStackInSlot(inventorySlot);
            int slotIndex = inventorySlot;
            while (source != null && transferred < limit && isEssentiaContainer(source)) {
                ItemStack current = source;
                if (!isEmpty(current) || !activate(
                    current,
                    handler,
                    slot,
                    result -> ContainerExchange.complete(
                        player,
                        current,
                        stack -> player.inventory.setInventorySlotContents(slotIndex, stack),
                        result),
                    null)) {
                    break;
                }
                transferred++;
                source = player.inventory.getStackInSlot(slotIndex);
            }
        }
        return transferred;
    }

    private static boolean isEmpty(ItemStack stack) {
        if (!(stack.getItem() instanceof IEssentiaContainerItem container)) {
            return false;
        }
        AspectList content = container.getAspects(singleContainer(stack));
        return content == null || content.size() == 0;
    }

    private static boolean isFilled(ItemStack stack) {
        return isEssentiaContainer(stack) && !isEmpty(stack);
    }

    public static boolean activate(ItemStack held, IBigAspectHandler handler, int slot, Consumer<ItemStack> exchange) {
        return activate(held, handler, slot, exchange, null);
    }

    public static boolean activate(ItemStack held, IBigAspectHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        if (held == null || handler.getStorageCount() <= 0) {
            return false;
        }
        int target = resolveContainer(handler, held, slot);
        if (target < 0) {
            return false;
        }
        ContainerDefinition definition = CONTAINERS.get(held.getItem());
        if (definition == null || !(held.getItem() instanceof IEssentiaContainerItem container)) {
            return false;
        }
        boolean transferred = false;
        ItemStack result = held.copy();
        result.stackSize = 1;
        AspectList content = container.getAspects(result);
        if (content != null && content.size() == 1) {
            Aspect aspect = content.getAspects()[0];
            BigAspectStack request = new BigAspectStack(aspect, content.getAmount(aspect));
            if (!request.isEmpty()) {
                Runnable rollbackFilter = prepareLockedSlot == null ? () -> {}
                    : prepareLockedSlot.apply(target, request);
                if (rollbackFilter != null) {
                    if (handler.insert(target, request, StorageAction.SIMULATE)
                        .isComplete()) {
                        TransferResult<BigAspectStack, AspectStorageKey> inserted = handler
                            .insert(target, request, StorageAction.EXECUTE);
                        if (inserted.isComplete()) {
                            container.setAspects(result, new AspectList());
                            result.setItemDamage(definition.emptyMetadata());
                            exchange.accept(result);
                            transferred = true;
                        } else if (inserted.getProcessedAmount() > 0L) {
                            handler.extract(target, inserted.getProcessedAmount(), StorageAction.EXECUTE);
                        }
                    }
                    if (!transferred) {
                        rollbackFilter.run();
                    }
                }
            }
        } else if (content == null || content.size() == 0) {
            BigAspectStack available = handler.getSnapshot(target);
            if (available.getAspect() != null && handler.extract(target, definition.capacity(), StorageAction.SIMULATE)
                .isComplete()) {
                TransferResult<BigAspectStack, AspectStorageKey> extracted = handler
                    .extract(target, definition.capacity(), StorageAction.EXECUTE);
                if (extracted.isComplete()) {
                    result.setItemDamage(definition.filledMetadata());
                    container.setAspects(result, new AspectList().add(available.getAspect(), definition.capacity()));
                    exchange.accept(result);
                    transferred = true;
                } else if (extracted.getProcessedAmount() > 0L) {
                    handler.insert(target, extracted.getProcessed(), StorageAction.EXECUTE);
                }
            }
        }
        return transferred;
    }

    public static int resolveContainer(IBigAspectHandler handler, ItemStack held, int slot) {
        if (slot >= 0) {
            return slot < handler.getStorageCount() ? slot : -1;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (!(single.getItem() instanceof IEssentiaContainerItem container)) {
            return -1;
        }
        AspectList content = container.getAspects(single);
        if (content != null && content.size() == 1) {
            Aspect aspect = content.getAspects()[0];
            BigAspectStack request = new BigAspectStack(aspect, content.getAmount(aspect));
            for (int index = 0; index < handler.getStorageCount(); index++) {
                if (handler.insert(index, request, StorageAction.SIMULATE)
                    .getProcessedAmount() > 0L) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (handler.getSnapshot(index)
                .getAspect() != null) {
                return index;
            }
        }
        return -1;
    }

}
