package com.hfstudio.functionalstorage.common.network;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class StorageSnapshotMessage implements IPacket {

    private int window;
    private NBTTagCompound data;

    public StorageSnapshotMessage() {}

    public StorageSnapshotMessage(int window, NBTTagCompound data) {
        this.window = window;
        this.data = data;
    }

    @Override
    public void encode(PacketBuffer buffer) throws IOException {
        buffer.writeInt(window);
        byte[] encoded = CompressedStreamTools.compress(data);
        buffer.writeInt(encoded.length);
        buffer.writeBytes(encoded);
    }

    @Override
    public void decode(PacketBuffer buffer) throws IOException {
        window = buffer.readInt();
        int length = buffer.readInt();
        if (length < 0 || length > buffer.readableBytes()) throw new IOException("Invalid storage snapshot length");
        byte[] encoded = new byte[length];
        buffer.readBytes(encoded);
        data = CompressedStreamTools.func_152457_a(encoded, new NBTSizeTracker(16L * 1024 * 1024));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer != null && client.thePlayer.openContainer instanceof ContainerDrawer menu
            && menu.windowId == window
            && data != null) menu.receiveSnapshot(data);
        return null;
    }
}
