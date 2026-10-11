package com.hfstudio.functionalstorage.client.integration;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.functionalstorage.mixins.late.gregtech.AccessorGTMultiTextureRender;
import com.hfstudio.functionalstorage.mixins.late.gregtech.AccessorGTSidedTextureRender;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IConnectable;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.common.blocks.BlockMachines;
import gregtech.common.blocks.ItemMachines;
import gregtech.common.render.GTMultiTextureRender;
import gregtech.common.render.GTSidedTextureRender;
import gregtech.common.render.IIconTexture;

@SideOnly(Side.CLIENT)
public class GT5MachineTextureAdapter {

    private static final int PIPE_CONNECTIONS = IConnectable.CONNECTED_WEST | IConnectable.CONNECTED_EAST;

    private GT5MachineTextureAdapter() {}

    @Nullable
    @Optional.Method(modid = "gregtech_nh")
    public static IIcon spriteFor(@Nullable ItemStack material, int side) {
        if (side < 0 || side >= ForgeDirection.VALID_DIRECTIONS.length || !isMachine(material)) return null;
        try {
            IMetaTileEntity metaTile = ItemMachines.getMetaTileEntity(material);
            if (metaTile == null) return null;

            IIcon icon = iconFor(firstLayer(metaTile.getInventoryTextures(), side), side);
            if (icon != null) return icon;

            return iconFor(firstLayer(textureFor(metaTile, side), 0), side);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isMachine(@Nullable ItemStack material) {
        if (material == null || !(material.getItem() instanceof ItemMachines)) return false;
        return Block.getBlockFromItem(material.getItem()) instanceof BlockMachines;
    }

    @Nullable
    private static ITexture firstLayer(@Nullable ITexture[][] textures, int side) {
        if (textures == null || side < 0 || side >= textures.length) return null;
        return firstLayer(textures[side], 0);
    }

    @Nullable
    private static ITexture firstLayer(@Nullable ITexture[] textures, int index) {
        return textures == null || index < 0 || index >= textures.length ? null : textures[index];
    }

    @Nullable
    private static ITexture[] textureFor(IMetaTileEntity metaTile, int side) {
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        if (metaTile instanceof MetaPipeEntity pipe) {
            boolean end = direction.offsetX != 0;
            return pipe.getTexture(
                pipe.getBaseMetaTileEntity(),
                end ? ForgeDirection.WEST : ForgeDirection.DOWN,
                PIPE_CONNECTIONS,
                -1,
                end,
                false);
        }
        return metaTile.getTexture(metaTile.getBaseMetaTileEntity(), direction, ForgeDirection.WEST, -1, true, false);
    }

    @Nullable
    private static IIcon iconFor(@Nullable ITexture texture, int side) {
        if (texture instanceof IIconTexture iconTexture) return iconTexture.getIcon(side, null);
        if (texture instanceof GTMultiTextureRender) {
            return iconFor(
                firstLayer(((AccessorGTMultiTextureRender) texture).functionalStorage$getTextures(), 0),
                side);
        }
        if (texture instanceof GTSidedTextureRender) {
            return iconFor(
                firstLayer(((AccessorGTSidedTextureRender) texture).functionalStorage$getTextures(), side),
                side);
        }
        return null;
    }

}
