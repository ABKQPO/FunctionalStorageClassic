package com.hfstudio.functionalstorage.misc;

import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.integration.ae2.AE2Integration;
import com.hfstudio.functionalstorage.common.integration.bogosorter.BogoSorterIntegration;
import com.hfstudio.functionalstorage.common.integration.findit.FindItIntegration;
import com.hfstudio.functionalstorage.common.integration.jabba.JabbaIntegration;
import com.hfstudio.functionalstorage.common.integration.thaumcraft.ThaumcraftIntegration;
import com.hfstudio.functionalstorage.common.integration.waila.WailaIntegration;
import com.hfstudio.functionalstorage.common.interaction.DrawerClickHandler;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        if (FunctionalStorageConfig.COMPATIBILITY.enableAE2Compatibility && Mods.AE2.isModLoaded()) {
            // Register before Thaumic Energistics' generic aspect-container handler.
            AE2Integration.register();
        }
        RegistrationHandler.registerBlocks();
        RegistrationHandler.registerItems();
        RegistrationHandler.registerTileEntities();
        RegistrationHandler.registerBlockProperties();
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new DrawerClickHandler());
        if (Mods.Waila.isModLoaded()) WailaIntegration.register();
        if (Mods.FindIt.isModLoaded() && FunctionalStorageConfig.COMPATIBILITY.enableFindItCompatibility) {
            FindItIntegration.register(isClientSide());
        }
    }

    public boolean isClientSide() {
        return false;
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Mods.InventoryBogoSorter.isModLoaded()) BogoSorterIntegration.register();
        if (FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility && Mods.Thaumcraft.isModLoaded()) {
            ThaumcraftIntegration.register();
            if (FunctionalStorageConfig.COMPATIBILITY.enableAE2Compatibility && Mods.ThaumicEnergistics.isModLoaded()) {
                AE2Integration.registerEssentiaTransport();
            }
        }
        if (Mods.Jabba.isModLoaded()) {
            JabbaIntegration.register();
        }
    }

    public void completeInit(FMLLoadCompleteEvent event) {
        FunctionalStorageRecipes.registerRecipes();
    }

    public void serverStarting(FMLServerStartingEvent event) {}

    public void onMissingMappings(FMLMissingMappingsEvent event) {
        for (MissingMapping mapping : event.get()) {
            boolean missingEssentia = (!Mods.Thaumcraft.isModLoaded()
                || !FunctionalStorageConfig.COMPATIBILITY.enableThaumcraftCompatibility)
                && ("functionalstorage:essentia_1".equals(mapping.name)
                    || "functionalstorage:essentia_2".equals(mapping.name)
                    || "functionalstorage:essentia_4".equals(mapping.name));
            if (missingEssentia) {
                mapping.ignore();
            }
        }
    }
}
