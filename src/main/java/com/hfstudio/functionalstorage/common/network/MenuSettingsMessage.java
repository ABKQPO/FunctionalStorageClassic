package com.hfstudio.functionalstorage.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.container.MenuSettingsReceiver;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class MenuSettingsMessage implements IMessage {

    private int window;
    private int value;
    private String text = "";

    public MenuSettingsMessage() {}

    public MenuSettingsMessage(int window, int value, String text) {
        this.window = window;
        this.value = value;
        this.text = text.length() > 50 ? text.substring(0, 50) : text;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        value = buffer.readInt();
        text = ByteBufUtils.readUTF8String(buffer);
        if (text.length() > 50) text = text.substring(0, 50);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(value);
        ByteBufUtils.writeUTF8String(buffer, text);
    }

    public static class Handler implements IMessageHandler<MenuSettingsMessage, IMessage> {

        @Override
        public IMessage onMessage(MenuSettingsMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (player.openContainer.windowId == message.window && player.openContainer.canInteractWith(player)
                    && player.openContainer instanceof MenuSettingsReceiver receiver) {
                    receiver.applySettings(player, message.value, message.text);
                }
            });
            return null;
        }
    }
}
