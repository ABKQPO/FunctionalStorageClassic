package com.hfstudio.functionalstorage.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers;
import com.hfstudio.functionalstorage.common.interaction.StorageTransfers.Action;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class StorageTransferMessage implements IMessage {

    private int window;
    private int slot;
    private Action action;
    private int amount;
    private boolean reverse;

    public StorageTransferMessage() {}

    public StorageTransferMessage(int window, int slot, Action action, int amount, boolean reverse) {
        this.window = window;
        this.slot = slot;
        this.action = action;
        this.amount = amount;
        this.reverse = reverse;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        slot = buffer.readInt();
        int ordinal = buffer.readUnsignedByte();
        Action[] actions = Action.values();
        action = ordinal < actions.length ? actions[ordinal] : null;
        amount = buffer.readUnsignedByte();
        reverse = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(slot);
        buffer.writeByte(action.ordinal());
        buffer.writeByte(amount);
        buffer.writeBoolean(reverse);
    }

    public static class Handler implements IMessageHandler<StorageTransferMessage, IMessage> {

        @Override
        public IMessage onMessage(StorageTransferMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (player.openContainer != null && player.openContainer.windowId == message.window) {
                    StorageTransfers.execute(player, message.slot, message.action, message.amount, message.reverse);
                }
            });
            return null;
        }
    }
}
