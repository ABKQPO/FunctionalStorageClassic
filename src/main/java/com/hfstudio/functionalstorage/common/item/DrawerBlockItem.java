package com.hfstudio.functionalstorage.common.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Section;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DrawerBlockItem extends ItemBlock {

    public DrawerBlockItem(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        for (Section section : DrawerTooltipData.read(stack)) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(section.title()));
            for (Entry entry : section.entries()) {
                String amount = entry.amount();
                tooltip.add(EnumChatFormatting.GRAY + "  " + entry.name() + (amount.isEmpty() ? "" : ": " + amount));
            }
        }
    }
}
