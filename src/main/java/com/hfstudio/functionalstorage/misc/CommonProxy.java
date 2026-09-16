package com.hfstudio.functionalstorage.misc;

import com.hfstudio.functionalstorage.common.integration.thaumcraft.ThaumcraftIntegration;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Common registration lifecycle for both physical sides. */
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
        if (FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Loader.isModLoaded("Thaumcraft")) {
            ThaumcraftIntegration.register();
        }
    }

    public void completeInit(FMLLoadCompleteEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void onMissingMappings(FMLMissingMappingsEvent event) {}

    protected void registerContent() {
        RegistrationHandler.registerBlocks();
        RegistrationHandler.registerItems();
        RegistrationHandler.registerTileEntities();
        RegistrationHandler.registerBlockProperties();
    }

    protected void registerRecipes() {
        FunctionalStorageRecipes.registerEarlyRecipes();
    }

    protected void registerIntegrations() {
        RegistrationHandler.registerCommonIntegrations();
    }
}
