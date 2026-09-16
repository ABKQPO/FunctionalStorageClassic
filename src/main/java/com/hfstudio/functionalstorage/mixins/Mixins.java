package com.hfstudio.functionalstorage.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/**
 * Declarative registry of every mixin shipped by this mod.
 *
 * <p>
 * The mod currently ships no mixins. Cross-mod support is implemented with
 * public APIs instead, which avoids coupling to another mod's internals:
 * Applied Energistics 2 through {@code IExternalStorageRegistry}, and Waila
 * through its plugin callback. The enum is kept so a future mixin can be
 * declared here with its required mods and loading phase.
 * </p>
 */
public enum Mixins implements IMixins {

    ;

    /**
     * Registers a late mixin group for one side.
     *
     * @param side   side the mixins apply to
     * @param mixins mixin class names relative to the mixin package
     * @return a builder configured for the late phase
     */
    public static MixinBuilder late(Side side, String... mixins) {
        return new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(Phase.LATE);
    }

    @Override
    public MixinBuilder getBuilder() {
        throw new UnsupportedOperationException("No mixins are registered by this mod");
    }

    /**
     * Optional mods this mod can target, kept so future mixins can declare
     * their requirements without restructuring this enum.
     */
    public enum TargetMod implements ITargetMod {

        APPLIED_ENERGISTICS_2(new TargetModBuilder().setModId("appliedenergistics2")),
        WAILA(new TargetModBuilder().setModId("Waila"));

        private final TargetModBuilder builder;

        TargetMod(TargetModBuilder builder) {
            this.builder = builder;
        }

        @Override
        public TargetModBuilder getBuilder() {
            return builder;
        }
    }
}
