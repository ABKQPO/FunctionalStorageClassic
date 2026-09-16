package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base class for every drawer upgrade item. Carries the registry name, icon,
 * and tooltip plumbing shared by all upgrade kinds.
 */
public class UpgradeItem extends Item {

    private final String id;
    private IIcon icon;

    public UpgradeItem(String id) {
        this.id = id;
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName("functionalstorage." + id);
    }

    /**
     * @return the stable identifier used for registration and localization
     */
    public String getId() {
        return id;
    }

    /**
     * Assigns the registry name. Called by the registration handler so the
     * constructor stays free of registration side effects.
     *
     * @param name registry name
     */
    public void setUpgradeName(String name) {
        setUnlocalizedName("functionalstorage." + name);
        setTextureName("functionalstorage:" + name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon("functionalstorage:" + id);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        String description = StatCollector.translateToLocal(getUnlocalizedName() + ".tooltip");
        if (description != null && !description.isEmpty() && !description.startsWith("item.")) {
            tooltip.add(description);
        }
    }
}
