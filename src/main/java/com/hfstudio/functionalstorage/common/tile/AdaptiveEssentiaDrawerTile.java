package com.hfstudio.functionalstorage.common.tile;

import com.hfstudio.functionalstorage.common.integration.AdaptiveLinking;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;

/** Essentia drawer that participates in the adaptive matrix network. */
public class AdaptiveEssentiaDrawerTile extends EssentiaDrawerTile {

    private boolean automaticLinkChecked;

    public AdaptiveEssentiaDrawerTile() {
        super(DrawerLayout.X_1);
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && !worldObj.isRemote && getAdaptiveMatrixPosition() == null && !automaticLinkChecked) {
            automaticLinkChecked = true;
            AdaptiveLinking.tryLink(this);
        }
        super.updateEntity();
    }
}
