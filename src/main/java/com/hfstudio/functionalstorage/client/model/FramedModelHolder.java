package com.hfstudio.functionalstorage.client.model;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only holder for the framed drawer model provider.
 *
 * <p>
 * The framed drawer block needs to supply a custom baked model, but it is a
 * common class that must load on a dedicated server where the client model
 * classes do not exist. Routing the call through this holder keeps every client
 * reference out of the block's own class initialization.
 * </p>
 */
@SideOnly(Side.CLIENT)
public class FramedModelHolder {

    private static FramedDrawerModelProvider provider;

    private FramedModelHolder() {}

    /**
     * Resolves the material-aware model for a framed drawer.
     *
     * @param context context being rendered
     * @return the material-aware model
     */
    @Nonnull
    public static BakedModel model(@Nonnull BakedModelQuadContext context) {
        if (provider == null) {
            provider = new FramedDrawerModelProvider();
        }
        return provider.wrap(context, ModelRegistry.getBakedModel(context.getBlockState()));
    }

    /**
     * @return the shared provider, creating it on first use
     */
    @Nonnull
    public static FramedDrawerModelProvider provider() {
        if (provider == null) {
            provider = new FramedDrawerModelProvider();
        }
        return provider;
    }
}
