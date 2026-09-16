package com.hfstudio.functionalstorage.common.integration.waila;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

/**
 * Waila registration. Waila 1.7.10 calls a static method named in the
 * WailaPlugin manifest attribute, so this class exposes that method and the
 * integration only ever loads when Waila is present.
 */
public class WailaIntegration {

    /**
     * Name of the static callback Waila looks for in the manifest.
     */
    public static final String CALLBACK = "wailaCallback";

    private WailaIntegration() {}

    /**
     * Registers the drawer tooltip provider with Waila.
     *
     * @param registrar Waila registrar
     */
    public static void wailaCallback(IWailaRegistrar registrar) {
        if (!FunctionalStorageConfig.COMPATIBILITY.enableWailaCompatibility) {
            FunctionalStorage.LOG.info("Waila integration disabled by configuration");
            return;
        }
        IWailaDataProvider provider = new DrawerWailaProvider();
        registrar.registerBodyProvider(provider, DrawerBlock.class);
        registrar.registerNBTProvider(provider, DrawerBlock.class);
        registrar.addConfig("Functional Storage", "functionalstorage.drawer", true);
    }
}
