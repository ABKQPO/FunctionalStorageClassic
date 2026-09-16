package com.hfstudio.functionalstorage.client.model;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Defers client model initialization so common blocks can load on a dedicated server. */
@SideOnly(Side.CLIENT)
public class FramedModelHolder {

    private static FramedDrawerModelProvider provider;

    private FramedModelHolder() {}

    @Nonnull
    public static BakedModel model(@Nonnull BakedModelQuadContext context) {
        if (provider == null) {
            provider = new FramedDrawerModelProvider();
        }
        return provider.wrap(context, DrawerModelProvider.INSTANCE.getModel(context));
    }

    @Nonnull
    public static FramedDrawerModelProvider provider() {
        if (provider == null) {
            provider = new FramedDrawerModelProvider();
        }
        return provider;
    }
}
