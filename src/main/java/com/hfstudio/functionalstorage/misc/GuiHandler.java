package com.hfstudio.functionalstorage.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.client.gui.GuiAdaptiveCrafting;
import com.hfstudio.functionalstorage.client.gui.GuiAdaptiveGrid;
import com.hfstudio.functionalstorage.client.gui.GuiAdaptiveMatrix;
import com.hfstudio.functionalstorage.client.gui.GuiArmory;
import com.hfstudio.functionalstorage.client.gui.GuiController;
import com.hfstudio.functionalstorage.client.gui.GuiDrawer;
import com.hfstudio.functionalstorage.client.gui.GuiUpgrade;
import com.hfstudio.functionalstorage.common.container.ContainerAdaptiveCrafting;
import com.hfstudio.functionalstorage.common.container.ContainerArmory;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.ContainerUpgrade;
import com.hfstudio.functionalstorage.common.integration.serverutilities.ServerUtilitiesIntegration;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Routes GUI requests to the server container and the sided client screen. */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_DRAWER = 0;
    public static final int GUI_UPGRADE_BASE = 100;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return null;
        }
        if (ServerUtilitiesIntegration.blocksInteraction(player, x, y, z)) {
            return null;
        }
        if (isUpgradeGui(drawer, id)) {
            return new ContainerUpgrade(drawer, player, id - GUI_UPGRADE_BASE);
        }
        return id == GUI_DRAWER
            ? drawer instanceof ArmoryCabinetTile armory ? new ContainerArmory(armory, player)
                : drawer instanceof AdaptiveCraftingGridTile grid ? new ContainerAdaptiveCrafting(grid, player)
                    : new ContainerDrawer(drawer, player)
            : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return null;
        }
        if (isUpgradeGui(drawer, id)) {
            return new GuiUpgrade(player, drawer, id - GUI_UPGRADE_BASE);
        }
        return id == GUI_DRAWER
            ? drawer instanceof ArmoryCabinetTile armory ? new GuiArmory(player, armory)
                : drawer instanceof AdaptiveCraftingGridTile grid ? new GuiAdaptiveCrafting(player, grid)
                    : drawer instanceof AdaptiveGridTile grid ? new GuiAdaptiveGrid(player, grid)
                        : drawer instanceof AdaptiveMatrixTile matrix ? new GuiAdaptiveMatrix(player, matrix)
                            : drawer instanceof StorageNetworkTile controller ? new GuiController(player, controller)
                                : new GuiDrawer(player, drawer)
            : null;
    }

    private boolean isUpgradeGui(ControllableDrawerTile drawer, int id) {
        int slot = id - GUI_UPGRADE_BASE;
        return slot >= 0 && slot < drawer.getUtilityUpgradeSlots()
            && drawer.getUtilityUpgrade(slot) != null
            && drawer.getUtilityUpgrade(slot)
                .getItem() instanceof AutomationUpgradeItem upgrade
            && upgrade.canOpenConfigurationGui();
    }
}
