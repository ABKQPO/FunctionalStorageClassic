package com.hfstudio.functionalstorage.client.gui;

import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;

public class GuiController extends GuiDrawer {

    public GuiController(EntityPlayer player, StorageNetworkTile tile) {
        super(player, tile);
    }

    @Override
    protected boolean showsDrawerFace() {
        return false;
    }

    @Override
    protected boolean showsPriority() {
        return false;
    }

    @Override
    protected String storageUpgradeLabel() {
        return "gui.functionalstorage.storage_range";
    }
}
