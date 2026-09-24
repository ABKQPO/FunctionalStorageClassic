package com.hfstudio.functionalstorage.core;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.hfstudio.functionalstorage.common.integration.Mods;

public enum Mixins implements IMixins {

    BOGO_SORTER_DROP_OFF(new MixinBuilder().addCommonMixins("bogosorter.MixinDropOffHandler")
        .addRequiredMod(Mods.InventoryBogoSorter)
        .setPhase(Phase.LATE)),
    OK_BACKPACK_DEPOSIT(new MixinBuilder().addCommonMixins("okbackpack.MixinInventoryInteractionHelpers")
        .addRequiredMod(Mods.OKBackpack)
        .setPhase(Phase.LATE)),
    THAUMIC_ENERGISTICS_TRANSPORT(
        new MixinBuilder().addCommonMixins("thaumicenergistics.MixinEssentiaTileContainerHelper")
            .addRequiredMod(Mods.ThaumicEnergistics)
            .setPhase(Phase.LATE)),
    JABBA_DRAWER_DOLLY(new MixinBuilder().addCommonMixins("jabba.MixinItemBarrelMover")
        .addRequiredMod(Mods.Jabba)
        .setPhase(Phase.LATE)),
    FINDIT_DRAWER_NETWORK(
        new MixinBuilder().addClientMixins("findit.MixinClientBlockFindService", "findit.MixinClientItemFindService")
            .addRequiredMod(Mods.FindIt)
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
