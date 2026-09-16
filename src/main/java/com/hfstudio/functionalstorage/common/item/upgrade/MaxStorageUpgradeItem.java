package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

/**
 * Upgrade that removes the capacity ceiling of a drawer. The drawer still
 * reports stored amounts, but accepts an unbounded number of compatible items.
 */
public class MaxStorageUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public MaxStorageUpgradeItem() {
        super("max_storage_upgrade");
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.MAX_CAPACITY);
    }
}
