package com.hfstudio.functionalstorage.common.presentation;

import java.util.List;

import javax.annotation.Nonnull;

public interface AdaptiveSortStrategy {

    @Nonnull
    String getId();

    void sort(@Nonnull List<AdaptiveDisplayEntry> entries);
}
