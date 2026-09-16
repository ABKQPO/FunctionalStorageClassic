package com.hfstudio.functionalstorage.misc;

import com.hfstudio.functionalstorage.client.render.DrawerRenderer;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.compact.CompactingDrawerTile;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only registration. Kept separate from the proxy so a dedicated server
 * never loads renderer classes.
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
        ClientRegistry.bindTileEntitySpecialRenderer(FluidDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(EssentiaDrawerTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(CompactingDrawerTile.class, renderer);
    }

    /**
     * Registers block colour handlers. Currently a no-op placeholder kept so
     * the proxy contract stays stable when tinting is added.
     */
    public static void registerBlockColors() {}
}
