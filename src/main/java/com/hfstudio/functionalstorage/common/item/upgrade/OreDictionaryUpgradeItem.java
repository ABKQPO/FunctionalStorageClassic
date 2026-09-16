package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

/**
 * Upgrade that lets a drawer treat items sharing a permitted ore dictionary
 * entry as interchangeable within one slot.
 */
public class OreDictionaryUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public OreDictionaryUpgradeItem() {
        super("ore_dictionary_upgrade");
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.EQUIVALENT_ITEMS);
    }
}
