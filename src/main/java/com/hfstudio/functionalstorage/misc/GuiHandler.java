package com.hfstudio.functionalstorage.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.client.gui.GuiDrawer;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Routes GUI requests to the server container and the sided client screen. */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_DRAWER = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_DRAWER) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return null;
        }
        return drawer.hasUpgradeSlots() ? new ContainerDrawer(drawer, player) : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_DRAWER) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return null;
        }
        return drawer.hasUpgradeSlots() ? new GuiDrawer(player, drawer) : null;
    }
}
