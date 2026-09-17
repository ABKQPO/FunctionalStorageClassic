package com.hfstudio.functionalstorage.common.integration.waila;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

public class WailaIntegration {

    public static void callbackRegister(IWailaRegistrar registrar) {
        if (!FunctionalStorageConfig.COMPATIBILITY.enableWailaCompatibility) {
            FunctionalStorage.LOG.info("Waila integration disabled by configuration");
            return;
        }
        IWailaDataProvider provider = new DrawerWailaProvider();
        registrar.registerBodyProvider(provider, DrawerBlock.class);
        registrar.registerNBTProvider(provider, DrawerBlock.class);
        registrar.addConfig("Functional Storage", "functionalstorage.drawer", true);
    }

    public static void register() {
        FMLInterModComms
            .sendMessage(Mods.Waila.modid, "register", WailaIntegration.class.getName() + ".callbackRegister");
    }
}
