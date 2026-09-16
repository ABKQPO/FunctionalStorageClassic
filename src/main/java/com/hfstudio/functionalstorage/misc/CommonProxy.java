package com.hfstudio.functionalstorage.misc;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/**
 * Server side proxy. Content registration happens here and on the client
 * subclass so a dedicated server never touches client-only classes.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        registerContent();
    }

    public void init(FMLInitializationEvent event) {
        registerRecipes();
        registerIntegrations();
    }

    public void postInit(FMLPostInitializationEvent event) {
        FunctionalStorageRecipes.registerLateRecipes();
    }

    public void completeInit(FMLLoadCompleteEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void onMissingMappings(FMLMissingMappingsEvent event) {}

    /**
     * Registers blocks, items, and tile entities.
     */
    protected void registerContent() {
        RegistrationHandler.registerBlocks();
        RegistrationHandler.registerItems();
        RegistrationHandler.registerTileEntities();
        RegistrationHandler.registerBlockProperties();
    }

    /**
     * Registers crafting and compacting recipes.
     */
    protected void registerRecipes() {
        FunctionalStorageRecipes.registerEarlyRecipes();
    }

    /**
     * Registers optional cross-mod hooks that are safe on both sides.
     */
    protected void registerIntegrations() {
        RegistrationHandler.registerCommonIntegrations();
    }
}
