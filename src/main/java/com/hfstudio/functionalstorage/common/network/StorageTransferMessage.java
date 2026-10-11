package com.hfstudio.functionalstorage.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
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
    private long session;
    private long revision;
    private boolean click;
    private int button;
    private int mode;

    public StorageTransferMessage() {}

    public StorageTransferMessage(int window, int slot, Action action, int amount, boolean reverse) {
        this.window = window;
        this.slot = slot;
        this.action = action;
        this.amount = amount;
        this.reverse = reverse;
    }

    public StorageTransferMessage(int window, int slot, Action action, int amount, boolean reverse, long session,
        long revision) {
        this(window, slot, action, amount, reverse);
        this.session = session;
        this.revision = revision;
    }

    public static StorageTransferMessage click(int window, int slot, int button, int mode, long session,
        long revision) {
        StorageTransferMessage message = new StorageTransferMessage();
        message.window = window;
        message.slot = slot;
        message.click = true;
        message.button = button;
        message.mode = mode;
        message.session = session;
        message.revision = revision;
        return message;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        slot = buffer.readInt();
        int ordinal = buffer.readUnsignedByte();
        Action[] actions = Action.values();
        action = ordinal < actions.length ? actions[ordinal] : null;
        amount = buffer.readInt();
        reverse = buffer.readBoolean();
        session = buffer.readLong();
        revision = buffer.readLong();
        click = buffer.readBoolean();
        button = buffer.readInt();
        mode = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(slot);
        buffer.writeByte(action == null ? 255 : action.ordinal());
        buffer.writeInt(amount);
        buffer.writeBoolean(reverse);
        buffer.writeLong(session);
        buffer.writeLong(revision);
        buffer.writeBoolean(click);
        buffer.writeInt(button);
        buffer.writeInt(mode);
    }

    public static class Handler implements IMessageHandler<StorageTransferMessage, IMessage> {

        @Override
        public IMessage onMessage(StorageTransferMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (player.openContainer != null && player.openContainer.windowId == message.window) {
                    if (player.openContainer instanceof ContainerDrawer menu) {
                        if (!menu.canInteractWith(player) || !menu.acceptsView(message.session, message.revision))
                            return;
                        if (message.click) {
                            if (message.mode < 0 || message.mode > 6
                                || message.mode == 5
                                || message.button < 0
                                || message.button > 8
                                || message.slot < 0
                                || message.slot >= menu.inventorySlots.size()) return;
                            menu.slotClick(message.slot, message.button, message.mode, player);
                            return;
                        }
                    }
                    StorageTransfers.execute(player, message.slot, message.action, message.amount, message.reverse);
                }
            });
            return null;
        }
    }
}
