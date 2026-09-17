package com.hfstudio.functionalstorage.common.container;

import net.minecraft.entity.player.EntityPlayer;

public interface MenuSettingsReceiver {

    void applySettings(EntityPlayer player, int value, String text);
}
