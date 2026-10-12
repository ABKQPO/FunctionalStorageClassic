package com.hfstudio.functionalstorage.common.item.upgrade;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

public class CreativeVendingUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public CreativeVendingUpgradeItem() {
        super("creative_vending_upgrade");
    }

    @Override
    public boolean isStorageUpgrade() {
        return true;
    }

    @Override
    public void applyUpgrade(@NotNull ItemStack stack, UpgradeState.@NotNull Builder builder) {
        builder.addFeature(StorageFeature.CREATIVE);
    }

    @Override
    public boolean hasEffect(ItemStack stack, int pass) {
        return true;
    }

    @Override
    public boolean conflictsWith(@NotNull ItemStack stack, @NotNull ItemStack other) {
        return other.getItem() instanceof CreativeVendingUpgradeItem;
    }
}
