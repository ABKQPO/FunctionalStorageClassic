package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

public class RedstoneUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public RedstoneUpgradeItem() {
        super("redstone_upgrade");
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.REDSTONE_OUTPUT);
    }
}
