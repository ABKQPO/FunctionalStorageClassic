package com.hfstudio.functionalstorage.common.item.upgrade;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;

/**
 * Void upgrade. Makes a drawer destroy items and fluids that no longer fit
 * instead of rejecting them, which keeps automation from stalling on a full
 * drawer. Only one may be installed per drawer.
 */
public class VoidUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    public VoidUpgradeItem() {
        super("void_upgrade");
    }

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {
        builder.addFeature(StorageFeature.VOID_OVERFLOW);
    }

    @Override
    public boolean conflictsWith(@Nonnull ItemStack stack, @Nonnull ItemStack otherStack) {
        return otherStack.getItem() instanceof VoidUpgradeItem;
    }
}
