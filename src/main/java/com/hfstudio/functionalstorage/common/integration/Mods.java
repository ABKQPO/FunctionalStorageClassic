package com.hfstudio.functionalstorage.common.integration;

import java.util.Locale;
import java.util.function.Supplier;

import com.gtnewhorizon.gtnhlib.util.data.IMod;

import cpw.mods.fml.common.Loader;

public enum Mods implements IMod {

    // spotless:off
    AE2("appliedenergistics2"),
    NotEnoughItems("NotEnoughItems"),
    NeverEnoughCharacters("nech"),
    Thaumcraft("Thaumcraft"),
    Waila("Waila"),
    ;
    // spotless:on

    public final String modid;
    public final String resourceDomain;
    private final Supplier<Boolean> supplier;
    private Boolean loaded;

    Mods(String modid) {
        this(modid, null);
    }

    Mods(Supplier<Boolean> supplier) {
        this(null, supplier);
    }

    Mods(String modid, Supplier<Boolean> supplier) {
        this.modid = modid;
        this.resourceDomain = modid != null ? modid.toLowerCase(Locale.ENGLISH) : null;
        this.supplier = supplier;
    }

    @Override
    public boolean isModLoaded() {
        if (loaded == null) {
            if (supplier != null) {
                loaded = supplier.get();
            } else if (modid != null) {
                loaded = Loader.isModLoaded(modid);
            } else loaded = false;
        }
        return loaded;
    }

    @Override
    public String getID() {
        return modid;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }
}
