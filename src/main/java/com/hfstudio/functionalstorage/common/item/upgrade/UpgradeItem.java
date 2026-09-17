package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/**
 * Base class for every drawer upgrade item. Carries the registry name, icon,
 * and tooltip plumbing shared by all upgrade kinds.
 */
public class UpgradeItem extends Item {

    @Getter
    private final String id;
    private IIcon icon;

    public UpgradeItem(String id) {
        this.id = id;
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName(FunctionalStorage.MOD_ID + "." + id);
    }

    /** Assigns the registry name without registering items during construction. */
    public void setUpgradeName(String name) {
        setUnlocalizedName(FunctionalStorage.MOD_ID + "." + name);
        setTextureName(FunctionalStorage.MOD_ID + ":" + name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icon = register.registerIcon(FunctionalStorage.MOD_ID + ":" + id);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        boolean storage = this instanceof IStorageUpgrade upgrade && upgrade.isStorageUpgrade();
        if (this instanceof IStorageUpgrade) {
            tooltip.add(
                EnumChatFormatting.GOLD + StatCollector.translateToLocal("upgrade.type")
                    + EnumChatFormatting.WHITE
                    + StatCollector.translateToLocal(storage ? "upgrade.type.storage" : "upgrade.type.utility"));
        }
        if (this instanceof StorageUpgradeItem || this instanceof GenerationUpgradeItem) {
            return;
        }
        addDescription(tooltip);
    }

    public void addDescription(List<String> tooltip) {
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(getUnlocalizedName() + ".tooltip"));
    }
}
