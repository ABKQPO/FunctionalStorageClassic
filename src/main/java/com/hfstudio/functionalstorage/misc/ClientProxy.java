package com.hfstudio.functionalstorage.misc;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Client side proxy. Adds item and tile-entity renderers on top of the common
 * registration path.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        DrawerClientRegistry.registerModelSource();
        DrawerClientRegistry.registerRenderers();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        DrawerClientRegistry.registerBlockColors();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
        DrawerClientRegistry.registerItemRenderer();
    }
}
