package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

/**
 * Upgrade that makes a drawer emit a comparator-style redstone signal based on
 * how full it is. The behaviour itself lives on the tile, this only declares
 * the intent so the tile can detect the upgrade.
 */
public class RedstoneUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public RedstoneUpgradeItem() {
        super("redstone_upgrade");
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.REDSTONE_OUTPUT);
    }
}
