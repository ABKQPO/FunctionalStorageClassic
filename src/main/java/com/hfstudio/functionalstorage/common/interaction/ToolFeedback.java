package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.GTNHLib;

public class ToolFeedback {

    private ToolFeedback() {}

    public static void send(EntityPlayer player, IChatComponent message) {
        if (player instanceof EntityPlayerMP serverPlayer) {
            message.getChatStyle()
                .setColor(EnumChatFormatting.WHITE);
            GTNHLib.proxy.sendMessageAboveHotbar(serverPlayer, message, 60, true, true);
        }
    }
}
