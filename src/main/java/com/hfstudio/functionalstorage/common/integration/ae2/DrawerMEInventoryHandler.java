package com.hfstudio.functionalstorage.common.integration.ae2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import appeng.api.config.StorageFilter;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageBusMonitor;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import cpw.mods.fml.common.Optional;
import lombok.Getter;

/**
 * Bridges a drawer's long-capacity item storage to AE2's ME inventory contract.
 * AE2 addresses storage by exact stack identity, so an aggregated view over the
 * physical slots is presented and injection and extraction are routed back
 * through the generic handler.
 */
@Optional.Interface(iface = "appeng.api.storage.IStorageBusMonitor", modid = "appliedenergistics2", striprefs = true)
public class DrawerMEInventoryHandler implements IStorageBusMonitor<IAEItemStack> {

    @Getter
    private final IBigItemHandler handler;
    private final int priority;
    private final Map<IMEMonitorHandlerReceiver, Object> listeners = new HashMap<>();
    private IItemList<IAEItemStack> snapshot;
    private BaseActionSource actionSource;

    public DrawerMEInventoryHandler(IBigItemHandler handler) {
        this(handler, 0);
    }

    public DrawerMEInventoryHandler(IBigItemHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static StorageAction actionOf(Actionable type) {
        return type == Actionable.SIMULATE ? StorageAction.SIMULATE : StorageAction.EXECUTE;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
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
    @Optional.Method(modid = "appliedenergistics2")
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
    @Optional.Method(modid = "appliedenergistics2")
    @SuppressWarnings("unchecked")
    public IItemList<IAEItemStack> getAvailableItems(IItemList out, int iteration) {
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
    @Optional.Method(modid = "appliedenergistics2")
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public boolean isPrioritized(IAEItemStack input) {
        return false;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
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

    @Optional.Method(modid = "appliedenergistics2")
    public IMEInventory<IAEItemStack> asInventory() {
        return this;
    }

    public List<ItemStorageView> getViews() {
        return ItemStorageView.storages(handler);
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void addListener(IMEMonitorHandlerReceiver listener, Object verificationToken) {
        listeners.put(listener, verificationToken);
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void removeListener(IMEMonitorHandlerReceiver listener) {
        listeners.remove(listener);
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public TickRateModulation onTick() {
        IItemList<IAEItemStack> current = availableItems();
        if (snapshot == null) {
            snapshot = current;
            return TickRateModulation.SLOWER;
        }
        List<IAEItemStack> changes = changesBetween(snapshot, current);
        snapshot = current;
        if (changes.isEmpty()) {
            return TickRateModulation.SLOWER;
        }
        postChanges(changes);
        return TickRateModulation.URGENT;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void setMode(StorageFilter mode) {}

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public void setActionSource(BaseActionSource source) {
        actionSource = source;
    }

    @Override
    @Optional.Method(modid = "appliedenergistics2")
    public IItemList<IAEItemStack> getStorageList() {
        if (snapshot == null) {
            snapshot = availableItems();
        }
        return snapshot;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private IItemList<IAEItemStack> availableItems() {
        return getAvailableItems(
            AEApi.instance()
                .storage()
                .createItemList());
    }

    @Optional.Method(modid = "appliedenergistics2")
    private List<IAEItemStack> changesBetween(IItemList<IAEItemStack> previous, IItemList<IAEItemStack> current) {
        IItemList<IAEItemStack> difference = AEApi.instance()
            .storage()
            .createItemList();
        for (IAEItemStack stack : previous) {
            IAEItemStack removed = stack.copy();
            removed.setStackSize(-removed.getStackSize());
            difference.add(removed);
        }
        for (IAEItemStack stack : current) {
            difference.add(stack.copy());
        }
        List<IAEItemStack> changes = new ArrayList<>();
        for (IAEItemStack stack : difference) {
            if (stack.getStackSize() != 0L) {
                changes.add(stack);
            }
        }
        return changes;
    }

    @Optional.Method(modid = "appliedenergistics2")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void postChanges(List<IAEItemStack> changes) {
        Iterator<Map.Entry<IMEMonitorHandlerReceiver, Object>> iterator = listeners.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver, Object> entry = iterator.next();
            if (entry.getKey()
                .isValid(entry.getValue())) {
                entry.getKey()
                    .postChange(this, changes, actionSource);
            } else {
                iterator.remove();
            }
        }
    }
}
