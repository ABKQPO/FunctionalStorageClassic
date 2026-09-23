package com.hfstudio.functionalstorage.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Accepts item templates without transferring inventory contents. */
public interface GhostFilterMenu {

    void applyFilter(EntityPlayer player, int slot, ItemStack template);
}
