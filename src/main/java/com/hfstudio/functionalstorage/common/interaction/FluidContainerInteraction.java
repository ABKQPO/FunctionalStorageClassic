package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

/**
 * Exchanges containers with one tank. A negative slot asks the storage to
 * choose the tank itself, which is how a controller terminal serves a whole
 * fluid network instead of a single face.
 */
public class FluidContainerInteraction {

    public static final int ROUTED = -1;

    private FluidContainerInteraction() {}

    public static boolean activate(EntityPlayer player, IBigFluidHandler handler, int slot) {
        return activate(player, handler, slot, null);
    }

    public static boolean activate(EntityPlayer player, IBigFluidHandler handler, int slot,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        ItemStack held = player.getHeldItem();
        if (!isFluidContainer(held)) {
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

    public static boolean activateCursor(EntityPlayer player, IBigFluidHandler handler, int slot,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isFluidContainer(cursor)) {
            return false;
        }
        return activateBatch(player, cursor, handler, slot, player.inventory::setItemStack, prepareLockedSlot);
    }

    private static boolean activateBatch(EntityPlayer player, ItemStack source, IBigFluidHandler handler, int slot,
        Consumer<ItemStack> replace, BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
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

    public static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange) {
        return activate(held, handler, slot, exchange, null);
    }

    public static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        return activateContainer(held, handler, slot, exchange, prepareLockedSlot, false);
    }

    private static boolean activateContainer(ItemStack held, IBigFluidHandler handler, int slot,
        Consumer<ItemStack> exchange, BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot,
        boolean matchingOnly) {
        if (held == null || handler == null || exchange == null || handler.getStorageCount() <= 0) {
            return false;
        }
        if (slot >= handler.getStorageCount()) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        if (single.getItem() instanceof IFluidContainerItem container) {
            FluidStack contained = container.getFluid(single);
            if (contained != null && contained.amount > 0) {
                return insertMutable(
                    handler,
                    slot,
                    single,
                    container,
                    contained,
                    exchange,
                    prepareLockedSlot,
                    matchingOnly) || fillMutable(handler, slot, single, container, exchange);
            }
            return fillMutable(handler, slot, single, container, exchange);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            return insertFilled(handler, slot, single, contained, exchange, prepareLockedSlot, matchingOnly);
        }
        if (!FluidContainerRegistry.isEmptyContainer(single)) {
            return false;
        }
        return fillRegistered(handler, slot, single, exchange);
    }

    public static boolean deposit(EntityPlayer player, IBigFluidHandler handler, ItemStack source,
        Consumer<ItemStack> replace) {
        if (player == null || !isFilledFluidContainer(source)) {
            return false;
        }
        return activate(source, handler, ROUTED, result -> ContainerExchange.complete(player, source, replace, result));
    }

    public static int depositInventory(EntityPlayer player, IBigFluidHandler handler) {
        return depositInventory(player, handler, false);
    }

    public static int depositMatchingInventory(EntityPlayer player, IBigFluidHandler handler) {
        return depositInventory(player, handler, true);
    }

    private static int depositInventory(EntityPlayer player, IBigFluidHandler handler, boolean matchingOnly) {
        if (player == null || handler == null) {
            return 0;
        }
        int transferred = 0;
        for (int index = 0; index < player.inventory.mainInventory.length; index++) {
            transferred += depositInventorySlot(player, handler, index, matchingOnly);
        }
        return transferred;
    }

    public static int depositInventorySlot(EntityPlayer player, IBigFluidHandler handler, int inventorySlot,
        boolean matchingOnly) {
        return depositInventorySlot(player, handler, inventorySlot, matchingOnly, Integer.MAX_VALUE);
    }

    public static int depositInventorySlot(EntityPlayer player, IBigFluidHandler handler, int inventorySlot,
        boolean matchingOnly, int limit) {
        if (player == null || handler == null
            || inventorySlot < 0
            || inventorySlot >= player.inventory.mainInventory.length
            || limit <= 0) {
            return 0;
        }
        int transferred = 0;
        ItemStack source = player.inventory.getStackInSlot(inventorySlot);
        while (source != null && transferred < limit && isFilledFluidContainer(source)) {
            ItemStack current = source;
            if (!activateContainer(
                current,
                handler,
                ROUTED,
                result -> ContainerExchange.complete(
                    player,
                    current,
                    stack -> player.inventory.setInventorySlotContents(inventorySlot, stack),
                    result),
                null,
                matchingOnly)) {
                break;
            }
            transferred++;
            source = player.inventory.getStackInSlot(inventorySlot);
        }
        return transferred;
    }

    public static int fillInventory(EntityPlayer player, IBigFluidHandler handler, int slot) {
        return fillInventory(player, handler, slot, Integer.MAX_VALUE);
    }

    public static int fillInventory(EntityPlayer player, IBigFluidHandler handler, int slot, int limit) {
        if (player == null || handler == null || slot < 0 || slot >= handler.getStorageCount() || limit <= 0) {
            return 0;
        }
        int transferred = 0;
        for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length
            && transferred < limit; inventorySlot++) {
            ItemStack source = player.inventory.getStackInSlot(inventorySlot);
            int slotIndex = inventorySlot;
            while (source != null && transferred < limit
                && isFluidContainer(source)
                && !isFilledFluidContainer(source)) {
                ItemStack current = source;
                if (!activateContainer(
                    current,
                    handler,
                    slot,
                    result -> ContainerExchange.complete(
                        player,
                        current,
                        stack -> player.inventory.setInventorySlotContents(slotIndex, stack),
                        result),
                    null,
                    false)) {
                    break;
                }
                transferred++;
                source = player.inventory.getStackInSlot(slotIndex);
            }
        }
        return transferred;
    }

    private static boolean insertFilled(IBigFluidHandler handler, int slot, ItemStack single, FluidStack fluid,
        Consumer<ItemStack> exchange, BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot,
        boolean matchingOnly) {
        BigFluidStack request = new BigFluidStack(fluid, fluid.amount);
        ItemStack drained = FluidContainerRegistry.drainFluidContainer(single);
        if (drained == null) {
            return false;
        }
        Runnable rollbackFilter = prepareLockedSlot == null ? () -> {} : prepareLockedSlot.apply(slot, request);
        if (rollbackFilter == null) {
            return false;
        }
        if (!insert(handler, slot, request, StorageAction.SIMULATE, matchingOnly).isComplete()) {
            rollbackFilter.run();
            return false;
        }
        TransferResult<BigFluidStack, FluidStorageKey> executed = insert(
            handler,
            slot,
            request,
            StorageAction.EXECUTE,
            matchingOnly);
        if (!executed.isComplete()) {
            rollbackInserted(handler, slot, executed);
            rollbackFilter.run();
            return false;
        }
        exchange.accept(drained);
        return true;
    }

    private static boolean insertMutable(IBigFluidHandler handler, int slot, ItemStack single,
        IFluidContainerItem container, FluidStack fluid, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot, boolean matchingOnly) {
        BigFluidStack request = new BigFluidStack(fluid, fluid.amount);
        FluidStack simulated = container.drain(single, fluid.amount, false);
        if (simulated == null || simulated.amount != fluid.amount || !simulated.isFluidEqual(fluid)) {
            return false;
        }
        Runnable rollbackFilter = prepareLockedSlot == null ? () -> {} : prepareLockedSlot.apply(slot, request);
        if (rollbackFilter == null) {
            return false;
        }
        if (!insert(handler, slot, request, StorageAction.SIMULATE, matchingOnly).isComplete()) {
            rollbackFilter.run();
            return false;
        }
        TransferResult<BigFluidStack, FluidStorageKey> executed = insert(
            handler,
            slot,
            request,
            StorageAction.EXECUTE,
            matchingOnly);
        if (!executed.isComplete()) {
            rollbackInserted(handler, slot, executed);
            rollbackFilter.run();
            return false;
        }
        FluidStack drained = container.drain(single, fluid.amount, true);
        if (drained == null || drained.amount != fluid.amount || !drained.isFluidEqual(fluid)) {
            rollbackInserted(handler, slot, executed);
            rollbackFilter.run();
            return false;
        }
        exchange.accept(single);
        return true;
    }

    private static boolean fillRegistered(IBigFluidHandler handler, int slot, ItemStack single,
        Consumer<ItemStack> exchange) {
        for (int index = slot < 0 ? 0 : slot; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            template.amount = Integer.MAX_VALUE;
            ItemStack filled = FluidContainerRegistry.fillFluidContainer(template, single);
            FluidStack content = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
            if (content == null
                || !extract(handler, index, new BigFluidStack(content, content.amount), StorageAction.SIMULATE)
                    .isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                index,
                new BigFluidStack(content, content.amount),
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, index, executed);
                return false;
            }
            exchange.accept(filled);
            return true;
        }
        return false;
    }

    private static boolean fillMutable(IBigFluidHandler handler, int slot, ItemStack single,
        IFluidContainerItem container, Consumer<ItemStack> exchange) {
        for (int index = slot < 0 ? 0 : slot; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            template.amount = Integer.MAX_VALUE;
            int capacity = container.fill(single, template, false);
            int available = clampToInt(stored.getAmount());
            int requested = Math.min(capacity, available);
            if (requested <= 0) {
                continue;
            }
            FluidStack content = template.copy();
            content.amount = requested;
            int filled = container.fill(single, content, false);
            if (filled <= 0) {
                continue;
            }
            filled = Math.min(filled, available);
            content.amount = filled;
            if (container.fill(single, content, false) != filled) {
                continue;
            }
            BigFluidStack request = new BigFluidStack(content, filled);
            if (!extract(handler, index, request, StorageAction.SIMULATE).isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                index,
                request,
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, index, executed);
                return false;
            }
            int actualFilled = container.fill(single, content, true);
            if (actualFilled != filled) {
                if (actualFilled > 0) {
                    container.drain(single, actualFilled, true);
                }
                rollbackExtracted(handler, index, executed);
                return false;
            }
            exchange.accept(single);
            return true;
        }
        return false;
    }

    private static int clampToInt(long amount) {
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, amount);
    }

    public static boolean isFluidContainer(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof IFluidContainerItem
            || FluidContainerRegistry.getFluidForFilledItem(stack) != null
            || FluidContainerRegistry.isEmptyContainer(stack));
    }

    public static boolean isFilledFluidContainer(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if (stack.getItem() instanceof IFluidContainerItem container) {
            FluidStack fluid = container.getFluid(stack);
            return fluid != null && fluid.amount > 0;
        }
        return FluidContainerRegistry.getFluidForFilledItem(stack) != null;
    }

    private static TransferResult<BigFluidStack, FluidStorageKey> insert(IBigFluidHandler handler, int slot,
        BigFluidStack request, StorageAction action) {
        return slot < 0 ? handler.fillRouted(request, action) : handler.insert(slot, request, action);
    }

    private static TransferResult<BigFluidStack, FluidStorageKey> insert(IBigFluidHandler handler, int slot,
        BigFluidStack request, StorageAction action, boolean matchingOnly) {
        if (!matchingOnly || slot >= 0) {
            return insert(handler, slot, request, action);
        }
        long requested = request.getAmount();
        long processedTotal = 0L;
        FluidStack template = request.getTemplate();
        int count = handler.getStorageCount();
        for (int index = 0; index < count && processedTotal < requested; index++) {
            if (!handler.supportsFill(index) || !handler.supportsFluid(index, request)
                || !handler.getSnapshot(index)
                    .isSameType(template)) {
                continue;
            }
            long remaining = requested - processedTotal;
            long processed = handler.insert(index, request.withAmount(remaining), action)
                .getProcessedAmount();
            processedTotal += Math.min(remaining, Math.max(0L, processed));
        }
        return new TransferResult<>(requested, request.withAmount(processedTotal), action);
    }

    private static TransferResult<BigFluidStack, FluidStorageKey> extract(IBigFluidHandler handler, int slot,
        BigFluidStack request, StorageAction action) {
        return slot < 0 ? handler.drainRouted(request, action) : handler.extract(slot, request.getAmount(), action);
    }

    private static void rollbackInserted(IBigFluidHandler handler, int slot,
        TransferResult<BigFluidStack, FluidStorageKey> inserted) {
        if (inserted.getProcessedAmount() > 0L) {
            extract(handler, slot, inserted.getProcessed(), StorageAction.EXECUTE);
        }
    }

    private static void rollbackExtracted(IBigFluidHandler handler, int slot,
        TransferResult<BigFluidStack, FluidStorageKey> extracted) {
        if (extracted.getProcessedAmount() > 0L) {
            insert(handler, slot, extracted.getProcessed(), StorageAction.EXECUTE);
        }
    }

}
