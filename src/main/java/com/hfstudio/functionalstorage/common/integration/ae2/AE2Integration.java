package com.hfstudio.functionalstorage.common.integration.ae2;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import appeng.api.AEApi;
import appeng.api.storage.IExternalStorageRegistry;

/** Optional AE2 external storage registration. */
public class AE2Integration {

    private AE2Integration() {}

    public static void register() {
        if (!FunctionalStorageConfig.COMPATIBILITY.enableAE2Compatibility) {
            FunctionalStorage.LOG.info("Applied Energistics 2 integration disabled by configuration");
            return;
        }
        if (!Mods.AE2.isModLoaded()) {
            FunctionalStorage.LOG.info("Applied Energistics 2 is not installed; skipping the storage bridge");
            return;
        }
        IExternalStorageRegistry registry = AEApi.instance()
            .registries()
            .externalStorage();
        registry.addExternalStorageInterface(new DrawerExternalStorageHandler());
        FunctionalStorage.LOG.info("Registered the Applied Energistics 2 drawer storage bridge");
    }
}
