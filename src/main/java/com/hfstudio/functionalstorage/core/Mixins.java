package com.hfstudio.functionalstorage.core;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    BOGO_SORTER_DROP_OFF(new MixinBuilder().addCommonMixins("bogosorter.MixinDropOffHandler")
        .addRequiredMod(Mods.INVENTORY_BOGO_SORTER)
        .setPhase(Phase.LATE)),
    OK_BACKPACK_DEPOSIT(new MixinBuilder().addCommonMixins("okbackpack.MixinInventoryInteractionHelpers")
        .addRequiredMod(Mods.OK_BACKPACK)
        .setPhase(Phase.LATE));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
