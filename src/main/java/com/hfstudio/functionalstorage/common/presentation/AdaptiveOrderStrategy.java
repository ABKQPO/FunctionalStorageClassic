package com.hfstudio.functionalstorage.common.presentation;

import java.util.List;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public interface AdaptiveOrderStrategy {

    @Nonnull
    String getId();

    void sort(@Nonnull List<ControllableDrawerTile> drawers);
}
