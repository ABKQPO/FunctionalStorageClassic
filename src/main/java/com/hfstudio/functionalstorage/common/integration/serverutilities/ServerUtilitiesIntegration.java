package com.hfstudio.functionalstorage.common.integration.serverutilities;

import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.functionalstorage.common.integration.Mods;

import cpw.mods.fml.common.Optional;
import serverutils.data.ClaimedChunks;

public class ServerUtilitiesIntegration {

    public static boolean blocksInteraction(EntityPlayer player, int x, int y, int z) {
        return Mods.ServerUtilities.isModLoaded() && checkInteraction(player, x, y, z);
    }

    @Optional.Method(modid = "serverutilities")
    private static boolean checkInteraction(EntityPlayer player, int x, int y, int z) {
        return player.worldObj != null
            && ClaimedChunks.blockBlockInteractions(player, x, y, z, player.worldObj.getBlockMetadata(x, y, z));
    }
}
