package com.hfstudio.functionalstorage.common.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.client.gui.CodeChickenTooltips;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DrawerBlockItem extends ItemBlock {

    public DrawerBlockItem(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        CodeChickenTooltips.appendPlaceholder(stack, tooltip);
    }
}
