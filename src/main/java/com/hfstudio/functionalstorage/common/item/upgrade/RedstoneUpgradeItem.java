package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

public class RedstoneUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public RedstoneUpgradeItem() {
        super("redstone_upgrade");
    }

    public int getSlot(ItemStack stack) {
        return Math.floorMod(UpgradeSettings.get(stack, "OutputSlot"), 4);
    }

    public void cycleSlot(ItemStack stack, int count) {
        UpgradeSettings.set(stack, "OutputSlot", (getSlot(stack) + 1) % Math.max(1, Math.min(4, count)));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) cycleSlot(stack, 4);
        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(StatCollector.translateToLocal("item.utility.slot") + getSlot(stack));
        tooltip.add(StatCollector.translateToLocal("item.utility.direction.desc"));
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.REDSTONE_OUTPUT);
    }
}
