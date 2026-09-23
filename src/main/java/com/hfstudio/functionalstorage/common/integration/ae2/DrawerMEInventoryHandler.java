package com.hfstudio.functionalstorage.common.integration.ae2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageChange;
import com.hfstudio.functionalstorage.api.storage.StorageSubscription;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.StorageFilter;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEMonitor;
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

    private final Map<IMEMonitorHandlerReceiver, Object> listeners = new HashMap<>();
    private final Map<IAEItemStack, IAEItemStack> pendingChanges = new HashMap<>();

    @Getter
    private final IBigItemHandler handler;
    private final int priority;
    private final StorageSubscription subscription;

    private BaseActionSource actionSource;

    public DrawerMEInventoryHandler(IBigItemHandler handler) {
        this(handler, 0);
    }

    public DrawerMEInventoryHandler(IBigItemHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
        this.subscription = handler.subscribe(this::onStorageChange);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static StorageAction actionOf(Actionable type) {
        return type == Actionable.SIMULATE ? StorageAction.SIMULATE : StorageAction.EXECUTE;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void onStorageChange(@Nonnull StorageChange<BigItemStack, ItemStorageKey> change) {
        if (!change.isDelta()) {
            // A reset states only that nothing about the previous contents may
            // be trusted and carries no amounts to diff against, so the drawer is
            // re-read and its whole inventory is republished instead.
            republishWholeInventory();
            flush();
            return;
        }
        for (StorageChange.Entry<BigItemStack, ItemStorageKey> entry : change.getEntries()) {
            accumulate(entry.getBefore(), entry.getAfter());
        }
        flush();
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void republishWholeInventory() {
        for (IAEItemStack current : availableItemsList()) {
            pendingChanges.put(current, current);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void accumulate(@Nonnull BigItemStack before, @Nonnull BigItemStack after) {
        if (before.hasTemplate() && after.hasTemplate() && before.isSameType(after.getTemplate())) {
            accumulateDelta(toAEStack(after.getTemplate()), after.getAmount() - before.getAmount());
            return;
        }
        if (before.hasTemplate()) {
            accumulateDelta(toAEStack(before.getTemplate()), -before.getAmount());
        }
        if (after.hasTemplate()) {
            accumulateDelta(toAEStack(after.getTemplate()), after.getAmount());
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void accumulateDelta(@Nullable IAEItemStack stack, long delta) {
        if (stack == null || delta == 0L) {
            return;
        }
        IAEItemStack previous = pendingChanges.get(stack);
        long total = delta + (previous == null ? 0L : previous.getStackSize());
        if (total == 0L) {
            pendingChanges.remove(stack);
            return;
        }
        stack.setStackSize(total);
        pendingChanges.put(stack, stack);
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    private IAEItemStack toAEStack(@Nullable ItemStack template) {
        return template == null ? null
            : AEApi.instance()
                .storage()
                .createItemStack(template);
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
            IAEItemStack stack = toAEStack(template);
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
    public IMEMonitor<IAEItemStack> asMonitor() {
        return this;
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
        flush();
        return TickRateModulation.SLEEP;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private void flush() {
        if (pendingChanges.isEmpty() || listeners.isEmpty()) {
            pendingChanges.clear();
            return;
        }
        List<IAEItemStack> changes = new ArrayList<>(pendingChanges.values());
        pendingChanges.clear();
        postChanges(changes);
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
        return availableItemsList();
    }

    public void close() {
        subscription.close();
        pendingChanges.clear();
        listeners.clear();
    }

    @Optional.Method(modid = "appliedenergistics2")
    private IItemList<IAEItemStack> availableItemsList() {
        return getAvailableItems(
            AEApi.instance()
                .storage()
                .createItemList(),
            0);
    }

    @Optional.Method(modid = "appliedenergistics2")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void postChanges(List<IAEItemStack> changes) {
        if (changes.isEmpty() || listeners.isEmpty()) {
            return;
        }
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
