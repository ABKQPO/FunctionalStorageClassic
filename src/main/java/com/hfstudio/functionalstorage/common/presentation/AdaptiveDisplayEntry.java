package com.hfstudio.functionalstorage.common.presentation;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;

public record AdaptiveDisplayEntry(@Nonnull BigItemStack content, long capacity) {

    public AdaptiveDisplayEntry {
        content = Objects.requireNonNull(content, "content");
        capacity = Math.max(0L, capacity);
    }
}
