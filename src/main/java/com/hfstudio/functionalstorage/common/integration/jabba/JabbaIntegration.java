package com.hfstudio.functionalstorage.common.integration.jabba;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;
import mcp.mobius.betterbarrels.common.items.dolly.api.IDollyHandler;
import mcp.mobius.betterbarrels.common.items.dolly.api.MovableRegistrar;

public class JabbaIntegration {

    @Optional.Method(modid = "JABBA")
    public static void register() {
        MovableRegistrar.INSTANCE.registerHandler(ControllableDrawerTile.class, new DrawerDollyHandler());
        FunctionalStorage.LOG.info("Registered Functional Storage drawers with the Jabba dolly");
    }

    public static class DrawerDollyHandler implements IDollyHandler {

        @Override
        public void onContainerPickup(World world, int x, int y, int z, TileEntity tileEntity) {
            if (tileEntity instanceof ControllableDrawerTile drawer) {
                drawer.detachFromController(world);
            }
        }
    }
}
