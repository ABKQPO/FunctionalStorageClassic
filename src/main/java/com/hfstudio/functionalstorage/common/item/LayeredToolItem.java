package com.hfstudio.functionalstorage.common.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class LayeredToolItem extends Item {

    private final String[] textures;
    @SideOnly(Side.CLIENT)
    private IIcon[] layers;

    protected LayeredToolItem(String name, String... textures) {
        this.textures = textures;
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName("functionalstorage." + name);
        setTextureName("functionalstorage:" + name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        layers = new IIcon[textures.length];
        for (int index = 0; index < textures.length; index++) {
            layers[index] = register.registerIcon("functionalstorage:" + textures[index]);
        }
        itemIcon = layers[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    public int getRenderPasses(int metadata) {
        return textures.length;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        return layers[Math.max(0, Math.min(pass, layers.length - 1))];
    }
}
