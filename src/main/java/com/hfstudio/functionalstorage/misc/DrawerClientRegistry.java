package com.hfstudio.functionalstorage.misc;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.render.DrawerRenderer;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.compact.CompactingDrawerTile;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only registration. Kept separate from the proxy so a dedicated server
 * never loads renderer or model loader classes.
 */
@SideOnly(Side.CLIENT)
public class DrawerClientRegistry {

    private DrawerClientRegistry() {}

    /**
     * Registers tile entity special renderers for every drawer kind.
     */
    public static void registerRenderers() {
        DrawerRenderer renderer = new DrawerRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(WoodDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(FramedDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(FluidDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(EssentiaDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(CompactingDrawerTile.class, renderer);
    }

    /**
     * Registers this mod id with GTNHLib so its resource pack is scanned for
     * blockstate and model files. This is client-only, because the model loader
     * touches client resource classes that a dedicated server does not have.
     */
    public static void registerModelSource() {
        ModelRegistry.registerModid(FunctionalStorage.MOD_ID);
    }

    /**
     * Registers block colour handlers. Currently a no-op placeholder kept so
     * the proxy contract stays stable when tinting is added.
     */
    public static void registerBlockColors() {}
}
