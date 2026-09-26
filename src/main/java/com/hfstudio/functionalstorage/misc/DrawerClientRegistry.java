package com.hfstudio.functionalstorage.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;

import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.model.DrawerModelProvider;
import com.hfstudio.functionalstorage.client.model.FramedModelHolder;
import com.hfstudio.functionalstorage.client.render.DrawerItemRenderer;
import com.hfstudio.functionalstorage.client.render.DrawerRenderer;
import com.hfstudio.functionalstorage.common.block.FramedBlock;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.tile.EnderDrawerTile;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.compact.CompactingDrawerTile;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Contains client registration to keep renderer classes off the dedicated server. */
@SideOnly(Side.CLIENT)
public class DrawerClientRegistry {

    private DrawerClientRegistry() {}

    public static void registerItemRenderer() {
        if (Minecraft.getMinecraft()
            .getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(new DrawerItemRenderer());
        }
    }

    public static void registerRenderers() {
        DrawerRenderer renderer = new DrawerRenderer();
        if (Minecraft.getMinecraft()
            .getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(renderer);
        }
        ClientRegistry.bindTileEntitySpecialRenderer(WoodDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(FramedDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(FluidDrawerTile.class, renderer);
        if (Mods.Thaumcraft.isModLoaded()) registerEssentiaRenderer(renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(CompactingDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(EnderDrawerTile.class, renderer);
    }

    @Optional.Method(modid = "Thaumcraft")
    private static void registerEssentiaRenderer(DrawerRenderer renderer) {
        ClientRegistry.bindTileEntitySpecialRenderer(EssentiaDrawerTile.class, renderer);
    }

    /** Registers blockstate and model resources with the GTNHLib client pipeline. */
    public static void registerModelSource() {
        ModelRegistry.registerModid(FunctionalStorage.MOD_ID);
        if (Minecraft.getMinecraft()
            .getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(DrawerModelProvider.INSTANCE);
        }
    }

    /** Registers per-part material colors for world and item rendering. */
    public static void registerBlockColors() {
        for (DrawerBlock block : RegistrationHandler.allDrawerBlocks()) {
            if (block instanceof FramedBlock) BlockColor.registerBlockColors(FramedModelHolder.provider(), block);
        }
    }
}
