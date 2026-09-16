package com.hfstudio.functionalstorage.common.item;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Tool that links a drawer to a storage controller. Right-click a controller to
 * select it, then right-click drawers to bind them.
 */
public class LinkingToolItem extends Item {

    private static final String KEY_TARGET = "ControllerTarget";

    public LinkingToolItem() {
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName("functionalstorage.linking_tool");
        setTextureName("functionalstorage:linking_tool");
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof DrawerControllerTile) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound()
                .setIntArray(KEY_TARGET, new int[] { x, y, z, world.provider.dimensionId });
            player.addChatMessage(new ChatComponentTranslation("functionalstorage.linking_tool.selected", x, y, z));
            return true;
        }
        if (tile instanceof ControllableDrawerTile && hasTarget(stack)) {
            int[] target = stack.getTagCompound()
                .getIntArray(KEY_TARGET);
            if (target.length == 4 && target[3] == world.provider.dimensionId) {
                TileEntity controller = world.getTileEntity(target[0], target[1], target[2]);
                if (controller instanceof DrawerControllerTile) {
                    ((ControllableDrawerTile) tile).setControllerPosition(target[0], target[1], target[2]);
                    ((DrawerControllerTile) controller).addDrawer(x, y, z);
                    player.addChatMessage(new ChatComponentTranslation("functionalstorage.linking_tool.linked"));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (hasTarget(stack)) {
            int[] target = stack.getTagCompound()
                .getIntArray(KEY_TARGET);
            if (target.length == 4) {
                tooltip.add(
                    StatCollector.translateToLocalFormatted(
                        "functionalstorage.linking_tool.tooltip.target",
                        target[0],
                        target[1],
                        target[2]));
                return;
            }
        }
        tooltip.add(StatCollector.translateToLocal("functionalstorage.linking_tool.tooltip"));
    }

    public static boolean hasTarget(@Nonnull ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(KEY_TARGET);
    }
}
