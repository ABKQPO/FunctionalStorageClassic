package com.hfstudio.functionalstorage.common.tile;

import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;

public class AdaptiveDrawerTile extends WoodDrawerTile {

    private boolean automaticLinkChecked;

    public AdaptiveDrawerTile() {
        super(DrawerLayout.X_4);
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            tryAutoLink();
        }
        super.updateEntity();
    }

    public void tryAutoLink() {
        AdaptiveLinking.tryLink(this);
    }
}
