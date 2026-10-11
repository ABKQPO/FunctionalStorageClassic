package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.common.interaction.ContainerBatchExchange;

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
            prepareLockedSlot,
            null);
    }

    public static boolean activateCursor(EntityPlayer player, IBigAspectHandler handler, int slot,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isEssentiaContainer(cursor)) {
            return false;
        }
        return activateBatch(player, cursor, handler, slot, player.inventory::setItemStack, prepareLockedSlot, null);
    }

    public static boolean activateCursor(EntityPlayer player, IBigAspectHandler handler, BigAspectStack selected,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isEssentiaContainer(cursor)) {
            return false;
        }
        return activateBatch(
            player,
            cursor,
            handler,
            ROUTED,
            player.inventory::setItemStack,
            prepareLockedSlot,
            selected);
    }

    private static boolean activateBatch(EntityPlayer player, ItemStack source, IBigAspectHandler handler, int slot,
        Consumer<ItemStack> replace, BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot,
        @Nullable BigAspectStack selected) {
        if (player.capabilities.isCreativeMode) {
            activate(source, handler, slot, result -> {}, prepareLockedSlot, selected);
            return true;
        }
        ItemStack remaining = source.copy();
        ContainerBatchExchange exchange = new ContainerBatchExchange(player, replace);
        int transfers = source.stackSize;
        for (int index = 0; index < transfers; index++) {
            if (!activate(singleContainer(remaining), handler, slot, exchange::accept, prepareLockedSlot, selected)) {
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
        if (handler == null) return 0;
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            EssentiaContainerRegistry::isFilled,
            (single, exchange) -> activate(single, handler, ROUTED, exchange, null));
    }

    public static int depositInventorySlot(EntityPlayer player, IBigAspectHandler handler, int inventorySlot,
        @Nullable Aspect selected, int limit) {
        if (handler == null) return 0;
        BigAspectStack selection = selected == null ? null : new BigAspectStack(selected, 1L);
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            source -> isFilled(source) && (selected == null || getContainedAspect(source) == selected),
            (single, exchange) -> activate(single, handler, ROUTED, exchange, null, selection));
    }

    public static int fillInventory(EntityPlayer player, IBigAspectHandler handler, int slot) {
        return fillInventory(player, handler, slot, Integer.MAX_VALUE);
    }

    public static int fillInventory(EntityPlayer player, IBigAspectHandler handler, int slot, int limit) {
        if (player == null || handler == null || slot < 0 || slot >= handler.getStorageCount() || limit <= 0) return 0;
        int transferred = 0;
        for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length
            && transferred < limit; inventorySlot++) {
            transferred += ContainerBatchExchange.exchangeInventorySlot(
                player,
                inventorySlot,
                limit - transferred,
                source -> isEssentiaContainer(source) && isEmpty(source),
                (single, exchange) -> activate(single, handler, slot, exchange, null));
        }
        return transferred;
    }

    public static int fillInventory(EntityPlayer player, IBigAspectHandler handler, BigAspectStack selected,
        int limit) {
        if (player == null || handler == null || selected == null || !selected.hasTemplate() || limit <= 0) {
            return 0;
        }
        int transferred = 0;
        for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length
            && transferred < limit; inventorySlot++) {
            transferred += fillInventorySlot(player, handler, inventorySlot, selected, limit - transferred);
        }
        return transferred;
    }

    public static int fillInventorySlot(EntityPlayer player, IBigAspectHandler handler, int inventorySlot,
        @Nullable BigAspectStack selected, int limit) {
        if (handler == null) return 0;
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            source -> isEssentiaContainer(source) && isEmpty(source),
            (single, exchange) -> activate(single, handler, ROUTED, exchange, null, selected));
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

    @Nullable
    public static Aspect getContainedAspect(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IEssentiaContainerItem container)) {
            return null;
        }
        AspectList content = container.getAspects(singleContainer(stack));
        return content == null || content.size() != 1 ? null : content.getAspects()[0];
    }

    public static boolean activate(ItemStack held, IBigAspectHandler handler, int slot, Consumer<ItemStack> exchange) {
        return activate(held, handler, slot, exchange, null);
    }

    public static boolean activate(ItemStack held, IBigAspectHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot) {
        return activate(held, handler, slot, exchange, prepareLockedSlot, null);
    }

    private static boolean activate(ItemStack held, IBigAspectHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigAspectStack, Runnable> prepareLockedSlot, @Nullable BigAspectStack selected) {
        if (held == null || handler.getStorageCount() <= 0) {
            return false;
        }
        int target = selected == null ? resolveContainer(handler, held, slot) : ROUTED;
        if (target < 0 && selected == null) {
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
            if (selected != null && selected.getAspect() != aspect) return false;
            if (!request.isEmpty()) {
                Runnable rollbackFilter = selected != null || prepareLockedSlot == null ? () -> {}
                    : prepareLockedSlot.apply(target, request);
                if (rollbackFilter != null) {
                    TransferResult<BigAspectStack, AspectStorageKey> simulated = selected == null
                        ? handler.insert(target, request, StorageAction.SIMULATE)
                        : handler.insertMatchingRouted(request, StorageAction.SIMULATE);
                    if (simulated.isComplete()) {
                        TransferResult<BigAspectStack, AspectStorageKey> inserted = selected == null
                            ? handler.insert(target, request, StorageAction.EXECUTE)
                            : handler.insertMatchingRouted(request, StorageAction.EXECUTE);
                        if (inserted.isComplete()) {
                            container.setAspects(result, new AspectList());
                            result.setItemDamage(definition.emptyMetadata());
                            exchange.accept(result);
                            transferred = true;
                        } else if (inserted.getProcessedAmount() > 0L) {
                            if (selected == null)
                                handler.extract(target, inserted.getProcessedAmount(), StorageAction.EXECUTE);
                            else handler.extractRouted(inserted.getProcessed(), StorageAction.EXECUTE);
                        }
                    }
                    if (!transferred) {
                        rollbackFilter.run();
                    }
                }
            }
        } else if (content == null || content.size() == 0) {
            BigAspectStack available = selected != null ? selected : handler.getSnapshot(target);
            TransferResult<BigAspectStack, AspectStorageKey> simulated = selected == null
                ? handler.extract(target, definition.capacity(), StorageAction.SIMULATE)
                : handler.extractRouted(
                    new BigAspectStack(available.getAspect(), definition.capacity()),
                    StorageAction.SIMULATE);
            if (available.getAspect() != null && simulated.isComplete()) {
                TransferResult<BigAspectStack, AspectStorageKey> extracted = selected == null
                    ? handler.extract(target, definition.capacity(), StorageAction.EXECUTE)
                    : handler.extractRouted(
                        new BigAspectStack(available.getAspect(), definition.capacity()),
                        StorageAction.EXECUTE);
                if (extracted.isComplete()) {
                    result.setItemDamage(definition.filledMetadata());
                    container.setAspects(result, new AspectList().add(available.getAspect(), definition.capacity()));
                    exchange.accept(result);
                    transferred = true;
                } else if (extracted.getProcessedAmount() > 0L) {
                    if (selected == null) handler.insert(target, extracted.getProcessed(), StorageAction.EXECUTE);
                    else handler.insertMatchingRouted(extracted.getProcessed(), StorageAction.EXECUTE);
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
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.getAspect() != null && snapshot.getAmount() > 0L) {
                return index;
            }
        }
        return -1;
    }

}
