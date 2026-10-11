package com.hfstudio.functionalstorage.common.presentation;

import java.util.List;

import javax.annotation.Nonnull;

public interface AdaptiveLayoutStrategy {

    @Nonnull
    String getId();

    @Nonnull
    List<Integer> allocate(int entryCount, @Nonnull List<Integer> drawerCapacities,
        @Nonnull List<Integer> requestedCells);
}
