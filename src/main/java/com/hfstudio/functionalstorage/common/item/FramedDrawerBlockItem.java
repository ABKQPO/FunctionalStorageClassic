package com.hfstudio.functionalstorage.common.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FramedDrawerBlockItem extends DrawerBlockItem {

    public FramedDrawerBlockItem(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("frameddrawer.use.0"));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("frameddrawer.use.1"));
    }
}
