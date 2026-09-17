package com.hfstudio.functionalstorage.common.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EnderDrawerBlockItem extends ItemBlock {

    public EnderDrawerBlockItem(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        NBTTagCompound tileData = DrawerBlock.getTileData(stack);
        if (tileData == null || tileData.getString("Frequency")
            .isEmpty()) {
            return;
        }
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("linkingtool.ender.frequency"));
        tooltip.add(tileData.getString("Frequency"));
    }
}
