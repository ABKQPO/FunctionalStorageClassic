package com.hfstudio.functionalstorage.common.integration.ae2;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import cpw.mods.fml.common.Optional;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Bridges a drawer's long-capacity essentia storage to Thaumic Energistics. */
@Optional.Interface(iface = "appeng.api.storage.IMEInventoryHandler", modid = "appliedenergistics2", striprefs = true)
public class DrawerMEEssentiaInventoryHandler implements IMEInventoryHandler<AEEssentiaStack> {

    private final IBigAspectHandler handler;
    private final int priority;

    public DrawerMEEssentiaInventoryHandler(IBigAspectHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
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
        if (extracted <= 0L) {
            return null;
        }
        AEEssentiaStack output = request.copy();
        output.setStackSize(Math.min(extracted, request.getStackSize()));
        return output;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IItemList<AEEssentiaStack> getAvailableItems(IItemList<AEEssentiaStack> out) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigAspectStack snapshot = handler.getSnapshot(index);
            if (snapshot.getAspect() != null && !snapshot.isEmpty()) {
                out.add(new AEEssentiaStack(snapshot.getAspect(), snapshot.getAmount()));
            }
        }
        return out;
    }

    @Override
    @Optional.Method(modid = "thaumicenergistics")
    public IAEStackType<?> getStackType() {
        return AEEssentiaStackType.ESSENTIA_STACK_TYPE;
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
