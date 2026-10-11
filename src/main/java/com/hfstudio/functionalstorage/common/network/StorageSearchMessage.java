package com.hfstudio.functionalstorage.common.network;

import java.util.BitSet;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.StorageSearchReceiver;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class StorageSearchMessage implements IMessage {

    private int window;
    private int row;
    private String query = "";
    private int[] matches = new int[0];
    private long session;
    private long revision;

    public StorageSearchMessage() {}

    public StorageSearchMessage(int window, int row, String query, int[] matches) {
        this.window = window;
        this.row = row;
        this.query = query.length() > 50 ? query.substring(0, 50) : query;
        this.matches = matches.clone();
    }

    public StorageSearchMessage(int window, int row, String query, int[] matches, long session, long revision) {
        this(window, row, query, matches);
        this.session = session;
        this.revision = revision;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        row = buffer.readInt();
        query = ByteBufUtils.readUTF8String(buffer);
        if (query.length() > 50) query = query.substring(0, 50);
        session = buffer.readLong();
        revision = buffer.readLong();
        int length = buffer.readInt();
        if (length < 0 || length > buffer.readableBytes())
            throw new IllegalArgumentException("Invalid storage search size");
        byte[] encoded = new byte[length];
        buffer.readBytes(encoded);
        matches = BitSet.valueOf(encoded)
            .stream()
            .toArray();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(row);
        ByteBufUtils.writeUTF8String(buffer, query);
        buffer.writeLong(session);
        buffer.writeLong(revision);
        BitSet selected = new BitSet();
        for (int index : matches) if (index >= 0) selected.set(index);
        byte[] encoded = selected.toByteArray();
        buffer.writeInt(encoded.length);
        buffer.writeBytes(encoded);
    }

    public static class Handler implements IMessageHandler<StorageSearchMessage, IMessage> {

        @Override
        public IMessage onMessage(StorageSearchMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (!(player.openContainer instanceof StorageSearchReceiver receiver)
                    || player.openContainer.windowId != message.window
                    || !player.openContainer.canInteractWith(player)) return;
                if (player.openContainer instanceof ContainerDrawer menu
                    && !menu.acceptsSearchView(message.session, message.revision)) return;
                receiver.applySearch(player, message.row, message.query, message.matches);
            });
            return null;
        }
    }
}
