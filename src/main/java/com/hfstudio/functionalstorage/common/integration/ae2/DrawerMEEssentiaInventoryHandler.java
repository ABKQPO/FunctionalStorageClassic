package com.hfstudio.functionalstorage.common.integration.ae2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.StorageFilter;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageBusMonitor;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

@Optional.Interface(iface = "appeng.api.storage.IStorageBusMonitor", modid = "appliedenergistics2", striprefs = true)
public class DrawerMEEssentiaInventoryHandler implements IStorageBusMonitor<AEEssentiaStack> {

    private final IBigAspectHandler handler;
    private final int priority;
    private final Map<IMEMonitorHandlerReceiver<AEEssentiaStack>, Object> listeners = new HashMap<>();
    private AspectSummary observed;
    private BaseActionSource actionSource;

    public DrawerMEEssentiaInventoryHandler(IBigAspectHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
        this.observed = handler.summary();
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public AEEssentiaStack injectItems(AEEssentiaStack input, Actionable type, BaseActionSource src) {
        if (input == null || input.getStackSize() <= 0L) {
            return null;
        }
        TransferResult<BigAspectStack, ?> result = handler.insertRouted(
            new BigAspectStack(input.getAspect(), input.getStackSize()),
            DrawerMEInventoryHandler.actionOf(type));
        long remaining = result.getRemainingAmount();
        if (type == Actionable.MODULATE) updateContents(src);
        if (remaining <= 0L) {
            return null;
        }
        AEEssentiaStack leftover = input.copy();
        leftover.setStackSize(remaining);
        return leftover;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public AEEssentiaStack extractItems(AEEssentiaStack request, Actionable mode, BaseActionSource src) {
        if (request == null || request.getStackSize() <= 0L) {
            return null;
        }
        TransferResult<BigAspectStack, ?> result = handler.extractRouted(
            new BigAspectStack(request.getAspect(), request.getStackSize()),
            DrawerMEInventoryHandler.actionOf(mode));
        long extracted = result.getProcessedAmount();
        if (mode == Actionable.MODULATE) updateContents(src);
        if (extracted <= 0L) {
            return null;
        }
        AEEssentiaStack output = request.copy();
        output.setStackSize(Math.min(extracted, request.getStackSize()));
        return output;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IItemList<AEEssentiaStack> getAvailableItems(IItemList<AEEssentiaStack> out, int iteration) {
        for (Map.Entry<Aspect, Long> entry : handler.summary()
            .getTotals()
            .entrySet()) {
            out.addStorage(new AEEssentiaStack(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IItemList<AEEssentiaStack> getStorageList() {
        return getAvailableItems(AEEssentiaStackType.ESSENTIA_STACK_TYPE.createList(), 0);
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    @SuppressWarnings("unchecked")
    public void addListener(IMEMonitorHandlerReceiver listener, Object verificationToken) {
        listeners.put(listener, verificationToken);
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public void removeListener(IMEMonitorHandlerReceiver listener) {
        listeners.remove(listener);
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public void setMode(StorageFilter mode) {
        // Every populated drawer slot is extractable, including locked slots.
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public void setActionSource(BaseActionSource source) {
        actionSource = source;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public TickRateModulation onTick() {
        return updateContents(actionSource);
    }

    @Optional.Method(modid = "thaumicenergistics")
    private TickRateModulation updateContents(BaseActionSource source) {
        AspectSummary current = handler.summary();
        if (observed == current) return TickRateModulation.SLOWER;
        List<AEEssentiaStack> changes = new ArrayList<>();
        for (Map.Entry<Aspect, Long> entry : observed.getTotals()
            .entrySet()) {
            long difference = current.getTotal(entry.getKey()) - entry.getValue();
            if (difference != 0L) changes.add(new AEEssentiaStack(entry.getKey(), difference));
        }
        for (Map.Entry<Aspect, Long> entry : current.getTotals()
            .entrySet()) {
            if (!observed.getTotals()
                .containsKey(entry.getKey())) {
                changes.add(new AEEssentiaStack(entry.getKey(), entry.getValue()));
            }
        }
        observed = current;
        if (changes.isEmpty()) return TickRateModulation.SLOWER;
        Iterator<Map.Entry<IMEMonitorHandlerReceiver<AEEssentiaStack>, Object>> iterator = listeners.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<IMEMonitorHandlerReceiver<AEEssentiaStack>, Object> entry = iterator.next();
            if (entry.getKey()
                .isValid(entry.getValue())) {
                entry.getKey()
                    .postChange(this, changes, source);
            } else {
                iterator.remove();
            }
        }
        return TickRateModulation.URGENT;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IAEStackType<?> getStackType() {
        return AEEssentiaStackType.ESSENTIA_STACK_TYPE;
    }

    public void close() {
        listeners.clear();
        observed = AspectSummary.empty();
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public AccessRestriction getAccess() {
        return AccessRestriction.READ_WRITE;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public boolean isPrioritized(AEEssentiaStack input) {
        return false;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public boolean canAccept(AEEssentiaStack input) {
        return input != null && input.getStackSize() > 0L
            && handler.insertRouted(new BigAspectStack(input.getAspect(), 1L), StorageAction.SIMULATE)
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

    @Optional.Method(modid = "thaumicenergistics")
    public IMEInventory<AEEssentiaStack> asInventory() {
        return this;
    }
}
