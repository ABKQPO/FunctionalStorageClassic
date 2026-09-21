package com.hfstudio.functionalstorage.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.functionalstorage.client.gui.DrawerGuiTextures;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipRenderer;
import com.hfstudio.functionalstorage.client.integration.BogoSorterShortcuts;
import com.hfstudio.functionalstorage.client.integration.MouseTweaksShortcuts;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.client.integration.NEIStorageShortcuts;
import com.hfstudio.functionalstorage.client.integration.NEIStorageTooltips;
import com.hfstudio.functionalstorage.client.integration.StorageOverlayHandler;
import com.hfstudio.functionalstorage.client.render.DrawerContainerHintOverlay;
import com.hfstudio.functionalstorage.client.render.LinkingToolOverlay;
import com.hfstudio.functionalstorage.common.integration.Mods;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        DrawerClientRegistry.registerModelSource();
        DrawerClientRegistry.registerRenderers();
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(DrawerGuiTextures.INSTANCE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new DrawerTooltipRenderer());
        MinecraftForge.EVENT_BUS.register(new DrawerContainerHintOverlay());
        MinecraftForge.EVENT_BUS.register(new LinkingToolOverlay());
        if (Mods.NotEnoughItems.isModLoaded()) {
            NEIGuiIntegration.register();
            NEIStorageShortcuts.register();
            NEIStorageTooltips.register();
            StorageOverlayHandler.register();
        }
        if (Mods.InventoryBogoSorter.isModLoaded()) MinecraftForge.EVENT_BUS.register(new BogoSorterShortcuts());
        if (Mods.MouseTweaks.isModLoaded()) FMLCommonHandler.instance()
            .bus()
            .register(new MouseTweaksShortcuts());
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
