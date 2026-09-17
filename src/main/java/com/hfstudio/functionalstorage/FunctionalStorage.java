package com.hfstudio.functionalstorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.hfstudio.functionalstorage.common.network.ArmorySearchMessage;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.CommonProxy;
import com.hfstudio.functionalstorage.misc.GuiHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = Tags.MODID,
    version = Tags.VERSION,
    name = Tags.MODNAME,
    dependencies = "required-after:gtnhlib@[0.11.48,)",
    guiFactory = "com.hfstudio.functionalstorage.misc.ConfigGuiFactory",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class FunctionalStorage {

    public static final String MOD_ID = Tags.MODID;
    public static final String MOD_NAME = Tags.MODNAME;
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOG = LogManager.getLogger(Tags.MODID);

    @Mod.Instance(Tags.MODID)
    public static FunctionalStorage instance;

    @SidedProxy(
        clientSide = "com.hfstudio.functionalstorage.misc.ClientProxy",
        serverSide = "com.hfstudio.functionalstorage.misc.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            FunctionalStorageConfig.registerConfig();
        } catch (ConfigException exception) {
            throw new IllegalStateException("Unable to register the Functional Storage config", exception);
        }
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        network.registerMessage(MenuSettingsMessage.Handler.class, MenuSettingsMessage.class, 0, Side.SERVER);
        network.registerMessage(ArmorySearchMessage.Handler.class, ArmorySearchMessage.class, 1, Side.SERVER);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void completeInit(FMLLoadCompleteEvent event) {
        proxy.completeInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void onMissingMappings(FMLMissingMappingsEvent event) {
        proxy.onMissingMappings(event);
    }
}
