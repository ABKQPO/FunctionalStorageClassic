package com.hfstudio.functionalstorage.common.interaction;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.annotation.Nullable;

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
            prepareLockedSlot,
            null);
    }

    public static boolean activateCursor(EntityPlayer player, IBigFluidHandler handler, int slot,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isFluidContainer(cursor)) {
            return false;
        }
        return activateBatch(player, cursor, handler, slot, player.inventory::setItemStack, prepareLockedSlot, null);
    }

    public static boolean activate(EntityPlayer player, IBigFluidHandler handler, BigFluidStack selected) {
        ItemStack held = player.getHeldItem();
        if (!isFluidContainer(held)) return false;
        FluidStack contained = getContainedFluid(held);
        if (!selected.hasTemplate()) {
            if (contained == null || contained.amount <= 0) return true;
            selected = new BigFluidStack(contained, 0L);
        }
        return activateBatch(
            player,
            held,
            handler,
            ROUTED,
            stack -> player.inventory.setInventorySlotContents(player.inventory.currentItem, stack),
            null,
            selected);
    }

    public static boolean activateCursor(EntityPlayer player, IBigFluidHandler handler, BigFluidStack selected,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        ItemStack cursor = player.inventory.getItemStack();
        if (!isFluidContainer(cursor)) {
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

    private static boolean activateBatch(EntityPlayer player, ItemStack source, IBigFluidHandler handler, int slot,
        Consumer<ItemStack> replace, BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot,
        @Nullable BigFluidStack selected) {
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

    public static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange) {
        return activate(held, handler, slot, exchange, null);
    }

    public static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot) {
        return activate(held, handler, slot, exchange, prepareLockedSlot, null);
    }

    private static boolean activate(ItemStack held, IBigFluidHandler handler, int slot, Consumer<ItemStack> exchange,
        BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot, @Nullable BigFluidStack selected) {
        return activateContainer(held, handler, slot, exchange, prepareLockedSlot, false, selected);
    }

    private static boolean activateContainer(ItemStack held, IBigFluidHandler handler, int slot,
        Consumer<ItemStack> exchange, BiFunction<Integer, BigFluidStack, Runnable> prepareLockedSlot,
        boolean matchingOnly, @Nullable BigFluidStack selected) {
        if (held == null || handler == null || exchange == null || handler.getStorageCount() <= 0) {
            return false;
        }
        if (slot >= handler.getStorageCount()) {
            return false;
        }
        ItemStack single = held.copy();
        single.stackSize = 1;
        boolean restrictToSelected = matchingOnly || selected != null && handler.hasMatchingResource(selected);
        if (single.getItem() instanceof IFluidContainerItem container) {
            FluidStack contained = container.getFluid(single);
            if (contained != null && contained.amount > 0) {
                if (selected != null && !selected.isSameType(contained)) return false;
                return insertMutable(
                    handler,
                    slot,
                    single,
                    container,
                    contained,
                    exchange,
                    prepareLockedSlot,
                    restrictToSelected) || fillMutable(handler, slot, single, container, exchange, selected);
            }
            return fillMutable(handler, slot, single, container, exchange, selected);
        }
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(single);
        if (contained != null) {
            if (selected != null && !selected.isSameType(contained)) return false;
            return insertFilled(handler, slot, single, contained, exchange, prepareLockedSlot, restrictToSelected);
        }
        if (!FluidContainerRegistry.isEmptyContainer(single)) {
            return false;
        }
        return fillRegistered(handler, slot, single, exchange, selected);
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
        if (handler == null) return 0;
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            FluidContainerInteraction::isFilledFluidContainer,
            (single, exchange) -> activateContainer(single, handler, ROUTED, exchange, null, matchingOnly, null));
    }

    public static int depositInventorySlot(EntityPlayer player, IBigFluidHandler handler, int inventorySlot,
        @Nullable FluidStack selected, int limit) {
        if (handler == null) return 0;
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            source -> isFilledFluidContainer(source)
                && (selected == null || getContainedFluid(source).isFluidEqual(selected)),
            (single, exchange) -> activateContainer(single, handler, ROUTED, exchange, null, selected != null, null));
    }

    public static int fillInventory(EntityPlayer player, IBigFluidHandler handler, int slot) {
        return fillInventory(player, handler, slot, Integer.MAX_VALUE);
    }

    public static int fillInventory(EntityPlayer player, IBigFluidHandler handler, int slot, int limit) {
        if (player == null || handler == null || slot < 0 || slot >= handler.getStorageCount() || limit <= 0) return 0;
        int transferred = 0;
        for (int inventorySlot = 0; inventorySlot < player.inventory.mainInventory.length
            && transferred < limit; inventorySlot++) {
            transferred += ContainerBatchExchange.exchangeInventorySlot(
                player,
                inventorySlot,
                limit - transferred,
                source -> isFluidContainer(source) && !isFilledFluidContainer(source),
                (single, exchange) -> activateContainer(single, handler, slot, exchange, null, false, null));
        }
        return transferred;
    }

    public static int fillInventory(EntityPlayer player, IBigFluidHandler handler, BigFluidStack selected, int limit) {
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

    public static int fillInventorySlot(EntityPlayer player, IBigFluidHandler handler, int inventorySlot,
        @Nullable BigFluidStack selected, int limit) {
        if (handler == null) return 0;
        return ContainerBatchExchange.exchangeInventorySlot(
            player,
            inventorySlot,
            limit,
            source -> isFluidContainer(source) && !isFilledFluidContainer(source),
            (single, exchange) -> activateContainer(single, handler, ROUTED, exchange, null, false, selected));
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
        Consumer<ItemStack> exchange, @Nullable BigFluidStack selected) {
        int start = selected == null ? Math.max(slot, 0) : 0;
        for (int index = start; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            if (selected != null && !stored.isSameType(selected.getTemplate())) {
                continue;
            }
            template.amount = Integer.MAX_VALUE;
            ItemStack filled = FluidContainerRegistry.fillFluidContainer(template, single);
            FluidStack content = filled == null ? null : FluidContainerRegistry.getFluidForFilledItem(filled);
            int target = selected == null ? index : ROUTED;
            if (content == null
                || !extract(handler, target, new BigFluidStack(content, content.amount), StorageAction.SIMULATE)
                    .isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                target,
                new BigFluidStack(content, content.amount),
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, target, executed, selected != null);
                return false;
            }
            exchange.accept(filled);
            return true;
        }
        return false;
    }

    private static boolean fillMutable(IBigFluidHandler handler, int slot, ItemStack single,
        IFluidContainerItem container, Consumer<ItemStack> exchange, @Nullable BigFluidStack selected) {
        int start = selected == null ? Math.max(slot, 0) : 0;
        for (int index = start; index < handler.getStorageCount(); index++) {
            if (slot >= 0 && index != slot) {
                break;
            }
            BigFluidStack stored = handler.getSnapshot(index);
            FluidStack template = stored.getTemplate();
            if (template == null) {
                continue;
            }
            if (selected != null && !stored.isSameType(selected.getTemplate())) {
                continue;
            }
            int available = clampToInt(stored.getAmount());
            if (available <= 0) {
                continue;
            }
            ItemStack filledContainer = single.copy();
            FluidStack before = container.getFluid(filledContainer);
            if (before != null && before.amount > 0 && !before.isFluidEqual(template)) {
                continue;
            }
            int beforeAmount = before == null ? 0 : before.amount;
            FluidStack offered = template.copy();
            offered.amount = available;
            int filled = container.fill(filledContainer, offered, true);
            if (filled <= 0 || filled > available) {
                continue;
            }
            FluidStack after = container.getFluid(filledContainer);
            if (after == null || !after.isFluidEqual(template) || (long) after.amount - beforeAmount != filled) {
                continue;
            }
            FluidStack content = template.copy();
            content.amount = filled;
            BigFluidStack request = new BigFluidStack(content, filled);
            int target = selected == null ? index : ROUTED;
            if (!extract(handler, target, request, StorageAction.SIMULATE).isComplete()) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> executed = extract(
                handler,
                target,
                request,
                StorageAction.EXECUTE);
            if (!executed.isComplete()) {
                rollbackExtracted(handler, target, executed, selected != null);
                return false;
            }
            exchange.accept(filledContainer);
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
        FluidStack fluid = getContainedFluid(stack);
        return fluid != null && fluid.amount > 0;
    }

    @Nullable
    public static FluidStack getContainedFluid(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        FluidStack fluid;
        if (stack.getItem() instanceof IFluidContainerItem container) {
            fluid = container.getFluid(stack);
        } else {
            fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
        }
        return fluid == null ? null : fluid.copy();
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
        return handler.fillMatchingRouted(request, action);
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
        TransferResult<BigFluidStack, FluidStorageKey> extracted, boolean matchingOnly) {
        if (extracted.getProcessedAmount() > 0L) {
            insert(handler, slot, extracted.getProcessed(), StorageAction.EXECUTE, matchingOnly);
        }
    }

}
