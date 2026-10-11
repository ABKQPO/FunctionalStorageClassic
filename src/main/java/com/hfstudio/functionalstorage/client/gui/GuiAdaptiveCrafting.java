package com.hfstudio.functionalstorage.client.gui;

import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.functionalstorage.common.container.ContainerAdaptiveCrafting;
import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;

public class GuiAdaptiveCrafting extends GuiDrawer {

    public GuiAdaptiveCrafting(EntityPlayer player, AdaptiveCraftingGridTile tile) {
        super(player, tile, new ContainerAdaptiveCrafting(tile, player));
    }
}
