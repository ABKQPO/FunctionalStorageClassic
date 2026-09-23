package com.hfstudio.functionalstorage.mixins.late.thaumicenergistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.common.integration.ae2.DrawerMEInventoryHandler;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import appeng.api.config.Actionable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.common.integration.tc.EssentiaTileContainerHelper;

/** Uses exact drawer transfers where a shared int capacity cannot describe the available space. */
@Mixin(value = EssentiaTileContainerHelper.class, remap = false)
public class MixinEssentiaTileContainerHelper {

    @Inject(method = "injectEssentiaIntoContainer", at = @At("HEAD"), cancellable = true, remap = false)
    private void functionalStorage$insert(IAspectContainer container, int amount, Aspect aspect, Actionable mode,
        CallbackInfoReturnable<Long> callback) {
        if (!FunctionalStorageConfig.COMPATIBILITY.enableAE2Compatibility
            || !FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility
            || !(container instanceof ControllableDrawerTile drawer)) return;
        IBigAspectHandler handler = drawer.getAspectHandler();
        if (handler == null) return;
        callback.setReturnValue(
            aspect == null || amount <= 0 ? 0L
                : handler.insertRouted(new BigAspectStack(aspect, amount), DrawerMEInventoryHandler.actionOf(mode))
                    .getProcessedAmount());
    }

    @Inject(method = "extractFromContainer", at = @At("HEAD"), cancellable = true, remap = false)
    private void functionalStorage$extract(IAspectContainer container, int amount, Aspect aspect, Actionable mode,
        CallbackInfoReturnable<Long> callback) {
        if (!FunctionalStorageConfig.COMPATIBILITY.enableAE2Compatibility
            || !FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility
            || !(container instanceof ControllableDrawerTile drawer)) return;
        IBigAspectHandler handler = drawer.getAspectHandler();
        if (handler == null) return;
        callback.setReturnValue(
            aspect == null || amount <= 0 ? 0L
                : handler.extractRouted(new BigAspectStack(aspect, amount), DrawerMEInventoryHandler.actionOf(mode))
                    .getProcessedAmount());
    }
}
