package com.hfstudio.functionalstorage.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.container.ContainerArmory;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client-localized search supplies a view; all inventory operations remain server-owned. */
public class ArmorySearchMessage implements IMessage {

    private int window;
    private int row;
    private String query = "";
    private int[] matches = new int[0];

    public ArmorySearchMessage() {}

    public ArmorySearchMessage(int window, int row, String query, int[] matches) {
        this.window = window;
        this.row = row;
        this.query = query.length() > 50 ? query.substring(0, 50) : query;
        this.matches = matches.clone();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        row = buffer.readInt();
        query = ByteBufUtils.readUTF8String(buffer);
        if (query.length() > 50) query = query.substring(0, 50);
        int count = buffer.readUnsignedShort();
        if (count > 8192 || buffer.readableBytes() < count * 2)
            throw new IllegalArgumentException("Invalid armory search size");
        matches = new int[count];
        for (int index = 0; index < count; index++) matches[index] = buffer.readUnsignedShort();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(row);
        ByteBufUtils.writeUTF8String(buffer, query);
        buffer.writeShort(matches.length);
        for (int slot : matches) buffer.writeShort(slot);
    }

    public static class Handler implements IMessageHandler<ArmorySearchMessage, IMessage> {

        @Override
        public IMessage onMessage(ArmorySearchMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerArmory armory && armory.windowId == message.window
                    && armory.canInteractWith(player)) armory.applySearch(message.row, message.query, message.matches);
            });
            return null;
        }
    }
}
