package com.hfstudio.functionalstorage.common.integration.ae2;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;

import appeng.api.AEApi;
import appeng.api.storage.IExternalStorageRegistry;
import cpw.mods.fml.common.Optional;
import thaumicenergistics.api.ThEApi;

public class AE2Integration {

    @Optional.Method(modid = "appliedenergistics2")
    public static void register() {
        IExternalStorageRegistry registry = AEApi.instance()
            .registries()
            .externalStorage();
        registry.addExternalStorageInterface(new DrawerExternalStorageHandler());
        FunctionalStorage.LOG.info("Registered the Applied Energistics 2 drawer storage bridge");
    }

    @Optional.Method(modid = "thaumicenergistics")
    public static void registerEssentiaTransport() {
        ThEApi.instance()
            .transportPermissions()
            .addAspectStorageTileToBothPermissions(EssentiaDrawerTile.class);
    }
}
