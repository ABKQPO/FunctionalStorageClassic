package com.hfstudio.functionalstorage.common.container;

import net.minecraft.entity.player.EntityPlayer;

public interface StorageSearchReceiver {

    void applySearch(EntityPlayer player, int row, String query, int[] matches);
}
