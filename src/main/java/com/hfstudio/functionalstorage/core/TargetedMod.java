package com.hfstudio.functionalstorage.core;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetedMod implements ITargetMod {

    INVENTORY_BOGO_SORTER("bogosorter");

    private final TargetModBuilder builder;

    TargetedMod(String modId) {
        builder = new TargetModBuilder().setModId(modId);
    }

    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
