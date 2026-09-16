package com.hfstudio.functionalstorage.common.integration.ae2;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

/**
 * Bridges a drawer's long-capacity item storage to AE2's ME inventory contract.
 * AE2 addresses storage by exact stack identity, so an aggregated view over the
 * physical slots is presented and injection and extraction are routed back
 * through the generic handler.
 */
public class DrawerMEInventoryHandler implements IMEInventoryHandler<IAEItemStack> {

    private final IBigItemHandler handler;
    private final int priority;

    public DrawerMEInventoryHandler(IBigItemHandler handler) {
        this(handler, 0);
    }

    public DrawerMEInventoryHandler(IBigItemHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
    }

    /**
     * @param type AE2 actionable mode
     * @return the matching storage action
     */
    public static StorageAction actionOf(Actionable type) {
        return type == Actionable.SIMULATE ? StorageAction.SIMULATE : StorageAction.EXECUTE;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable type, BaseActionSource src) {
        if (input == null || input.getStackSize() <= 0 || input.getItemStack() == null) {
            return null;
        }
        BigItemStack request = new BigItemStack(input.getItemStack(), input.getStackSize());
        TransferResult<BigItemStack, ItemStorageKey> result = handler.insertRouted(request, actionOf(type));
        long remaining = result.getRemainingAmount();
        if (remaining <= 0L) {
            return null;
        }
        IAEItemStack leftover = input.copy();
        leftover.setStackSize(remaining);
        return leftover;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null || request.getStackSize() <= 0 || request.getItemStack() == null) {
            return null;
        }
        BigItemStack typed = new BigItemStack(request.getItemStack(), request.getStackSize());
        TransferResult<BigItemStack, ItemStorageKey> result = handler.extractRouted(typed, actionOf(mode));
        long extracted = result.getProcessedAmount();
        if (extracted <= 0L) {
            return null;
        }
        IAEItemStack out = request.copy();
        out.setStackSize(Math.min(extracted, request.getStackSize()));
        return out;
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
        for (ItemStorageView view : ItemStorageView.storages(handler)) {
            ItemStack template = view.getSnapshot()
                .getTemplate();
            if (template == null) {
                continue;
            }
            IAEItemStack stack = AEApi.instance()
                .storage()
                .createItemStack(template);
            if (stack == null) {
                continue;
            }
            stack.setStackSize(
                view.getSnapshot()
                    .getAmount());
            out.add(stack);
        }
        return out;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    public boolean isPrioritized(IAEItemStack input) {
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack input) {
        if (input == null || input.getItemStack() == null) {
            return false;
        }
        return handler.insertRouted(new BigItemStack(input.getItemStack(), 1L), StorageAction.SIMULATE)
            .getProcessedAmount() > 0L;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int pass) {
        return true;
    }

    /**
     * @return the wrapped drawer handler
     */
    public IBigItemHandler getHandler() {
        return handler;
    }

    /**
     * @return an inventory view of this handler, used by the AE2 storage bus
     */
    public IMEInventory<IAEItemStack> asInventory() {
        return this;
    }

    /**
     * @return the aggregated views this handler currently reports to AE2
     */
    public List<ItemStorageView> getViews() {
        return ItemStorageView.storages(handler);
    }
}
