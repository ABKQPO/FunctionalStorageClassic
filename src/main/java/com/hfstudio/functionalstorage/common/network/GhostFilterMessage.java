package com.hfstudio.functionalstorage.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.ServerThreadUtil;
import com.hfstudio.functionalstorage.common.container.ContainerAdaptiveCrafting;
import com.hfstudio.functionalstorage.common.container.GhostFilterMenu;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class GhostFilterMessage implements IMessage {

    private int window;
    private int slot;
    private ItemStack template;
    private ItemStack[] recipe;
    private boolean maxTransfer;

    public GhostFilterMessage() {}

    public GhostFilterMessage(int window, int slot, ItemStack template) {
        this.window = window;
        this.slot = slot;
        this.template = template.copy();
        this.template.stackSize = 1;
    }

    public GhostFilterMessage(int window, ItemStack[] recipe, boolean maxTransfer) {
        this.window = window;
        this.recipe = recipe.clone();
        this.maxTransfer = maxTransfer;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        window = buffer.readInt();
        slot = buffer.readInt();
        template = ByteBufUtils.readItemStack(buffer);
        if (template != null) template.stackSize = 1;
        boolean crafting = buffer.readBoolean();
        if (crafting) {
            recipe = new ItemStack[9];
            maxTransfer = buffer.readBoolean();
            for (int index = 0; index < recipe.length; index++) recipe[index] = ByteBufUtils.readItemStack(buffer);
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(window);
        buffer.writeInt(slot);
        ByteBufUtils.writeItemStack(buffer, template);
        buffer.writeBoolean(recipe != null);
        if (recipe != null) {
            buffer.writeBoolean(maxTransfer);
            for (int index = 0; index < 9; index++) ByteBufUtils.writeItemStack(buffer, recipe[index]);
        }
    }

    public static class Handler implements IMessageHandler<GhostFilterMessage, IMessage> {

        @Override
        public IMessage onMessage(GhostFilterMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            ServerThreadUtil.addScheduledTask(() -> {
                if (player.openContainer == null || player.openContainer.windowId != message.window
                    || !player.openContainer.canInteractWith(player)) return;
                if (message.recipe != null && player.openContainer instanceof ContainerAdaptiveCrafting crafting) {
                    crafting.applyRecipe(player, message.recipe, message.maxTransfer);
                    return;
                }
                if (message.template != null && message.template.getItem() != null
                    && player.openContainer.windowId == message.window
                    && player.openContainer.canInteractWith(player)
                    && player.openContainer instanceof GhostFilterMenu menu) {
                    menu.applyFilter(player, message.slot, message.template);
                }
            });
            return null;
        }
    }
}
