package com.hfstudio.functionalstorage.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Model provider for framed drawers. The framed model marks its exterior and
 * front quads with dedicated marker textures; this provider replaces those
 * quads with the material the player applied, rescaling their UVs so the new
 * sprite is sampled across the full face instead of a corner of it.
 */
@SideOnly(Side.CLIENT)
public class FramedDrawerModelProvider implements IBlockModelProvider {

    private static final String SIDE_MARKER = "functionalstorage:blocks/framed_side";
    private static final String FRONT_MARKER = "functionalstorage:blocks/framed_front_";

    private final Map<String, PartSprites> spriteCache = new ConcurrentHashMap<>();

    @Override
    public BakedModel getModel(BakedModelQuadContext context) {
        return wrap(context, ModelRegistry.getBakedModel(context.getBlockState()));
    }

    /**
     * Wraps a baked model with the materials of the framed drawer at the
     * context's position.
     *
     * @param context context being rendered
     * @param base    unwrapped baked model
     * @return the material-aware model, or {@code base} when no material applies
     */
    @Nonnull
    public BakedModel wrap(@Nonnull BakedModelQuadContext context, @Nonnull BakedModel base) {
        if (!(context instanceof BakedModelQuadContext.World)) {
            return base;
        }
        PartSprites sprites = spritesAt((BakedModelQuadContext.World) context);
        return sprites == null ? base : new FramedBakedModel(base, sprites);
    }

    /**
     * @param stack framed drawer item stack
     * @return the material sprites that item should render with, or {@code null}
     */
    @Nullable
    public PartSprites spritesFor(@Nonnull ItemStack stack) {
        FramedDrawerStyle style = FramedDrawerStyle.fromDrawerStack(stack);
        return style.isConfigured() ? resolve(style) : null;
    }

    @Nullable
    private PartSprites spritesAt(@Nonnull BakedModelQuadContext.World context) {
        IBlockAccess world = context.getWorld();
        if (world == null) {
            return null;
        }
        Object tile = world.getTileEntity(context.getX(), context.getY(), context.getZ());
        if (!(tile instanceof FramedDrawerTile)) {
            return null;
        }
        FramedDrawerStyle style = ((FramedDrawerTile) tile).getStyle();
        return style.isConfigured() ? resolve(style) : null;
    }

    @Nonnull
    private PartSprites resolve(@Nonnull FramedDrawerStyle style) {
        PartSprites cached = spriteCache.get(style.getCacheKey());
        if (cached != null) {
            return cached;
        }
        PartSprites resolved = PartSprites.from(style);
        spriteCache.put(style.getCacheKey(), resolved);
        return resolved;
    }

    private static boolean isSideMarker(@Nonnull String iconName) {
        return iconName.startsWith(SIDE_MARKER);
    }

    private static boolean isFrontMarker(@Nonnull String iconName) {
        return iconName.startsWith(FRONT_MARKER);
    }

    private static boolean isCenterStrip(float min, float max) {
        return Math.abs(min - 7F) < 0.1F && Math.abs(max - 9F) < 0.1F;
    }

    /**
     * Divider quads occupy the narrow centre strip of the marker texture. Their
     * presence is what distinguishes a slot divider from a drawer front.
     *
     * @param quad candidate quad
     * @return whether the quad is a divider
     */
    private static boolean isDivider(@Nonnull ModelQuadView quad) {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float u = quad.getTexU(vertex);
            float v = quad.getTexV(vertex);
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        return isCenterStrip(minU, maxU) || isCenterStrip(minV, maxV);
    }

    /**
     * Copies a quad and swaps its sprite, remapping UVs from the marker sprite
     * into the replacement sprite's own range.
     *
     * @param quad        source quad
     * @param marker      sprite the quad currently samples
     * @param replacement sprite to sample instead
     * @return the retextured copy
     */
    @Nonnull
    public static ModelQuad retexture(@Nonnull ModelQuadView quad, @Nonnull TextureAtlasSprite marker,
        @Nonnull TextureAtlasSprite replacement) {
        ModelQuad copy = new ModelQuad(quad);
        float markerWidth = marker.getMaxU() - marker.getMinU();
        float markerHeight = marker.getMaxV() - marker.getMinV();
        float replacementWidth = replacement.getMaxU() - replacement.getMinU();
        float replacementHeight = replacement.getMaxV() - replacement.getMinV();
        for (int vertex = 0; vertex < 4; vertex++) {
            float relativeU = markerWidth <= 0F ? 0F : (quad.getTexU(vertex) - marker.getMinU()) / markerWidth;
            float relativeV = markerHeight <= 0F ? 0F : (quad.getTexV(vertex) - marker.getMinV()) / markerHeight;
            copy.setTexU(vertex, replacement.getMinU() + clamp(relativeU) * replacementWidth);
            copy.setTexV(vertex, replacement.getMinV() + clamp(relativeV) * replacementHeight);
        }
        copy.setSprite(replacement);
        return copy;
    }

    private static float clamp(float value) {
        return value < 0F ? 0F : Math.min(value, 1F);
    }

    /**
     * Sprites for the three retextured parts of a framed drawer.
     */
    public static class PartSprites {

        @Nullable
        public final TextureAtlasSprite exterior;
        @Nullable
        public final TextureAtlasSprite front;
        @Nullable
        public final TextureAtlasSprite divider;

        public PartSprites(@Nullable TextureAtlasSprite exterior, @Nullable TextureAtlasSprite front,
            @Nullable TextureAtlasSprite divider) {
            this.exterior = exterior;
            this.front = front;
            this.divider = divider;
        }

        /**
         * Resolves the sprites for a material selection.
         *
         * @param style material selection
         * @return the resolved sprites
         */
        @Nonnull
        public static PartSprites from(@Nonnull FramedDrawerStyle style) {
            return new PartSprites(
                spriteFor(style.getExterior()),
                spriteFor(style.getFront()),
                spriteFor(style.getDivider()));
        }

        /**
         * Resolves the atlas sprite of a block item.
         *
         * @param material block item stack
         * @return the sprite, or {@code null}
         */
        @Nullable
        public static TextureAtlasSprite spriteFor(@Nullable ItemStack material) {
            if (material == null || material.getItem() == null || !(material.getItem() instanceof ItemBlock)) {
                return null;
            }
            Block block = ((ItemBlock) material.getItem()).field_150939_a;
            if (block == null) {
                return null;
            }
            IIcon icon = block.getIcon(0, material.getItemDamage());
            return icon instanceof TextureAtlasSprite ? (TextureAtlasSprite) icon : null;
        }

        /**
         * @return whether at least the exterior and front are available
         */
        public boolean isUsable() {
            return exterior != null && front != null;
        }
    }

    /**
     * Wraps a baked model and substitutes the framed drawer's marker quads with
     * the applied materials.
     */
    public static class FramedBakedModel implements BakedModel {

        private final BakedModel parent;
        private final PartSprites sprites;

        public FramedBakedModel(@Nonnull BakedModel parent, @Nonnull PartSprites sprites) {
            this.parent = parent;
            this.sprites = sprites;
        }

        @Override
        public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
            List<ModelQuadView> original = parent.getQuads(context);
            if (original == null || original.isEmpty() || !sprites.isUsable()) {
                return original;
            }
            List<ModelQuadView> retextured = new ArrayList<>(original.size());
            boolean dynamic = false;
            for (ModelQuadView quad : original) {
                ModelQuadView replaced = retextureQuad(quad);
                retextured.add(replaced);
                dynamic |= replaced != quad;
            }
            return retextured;
        }

        @NotNull
        private ModelQuadView retextureQuad(@Nonnull ModelQuadView quad) {
            Object spriteObject = quad.celeritas$getSprite();
            if (!(spriteObject instanceof TextureAtlasSprite marker)) {
                return quad;
            }
            String iconName = marker.getIconName();
            if (iconName == null) {
                return quad;
            }
            if (isSideMarker(iconName)) {
                return sprites.exterior == null ? quad : retexture(quad, marker, sprites.exterior);
            }
            if (isFrontMarker(iconName)) {
                if (isDivider(quad)) {
                    return sprites.divider == null ? quad : retexture(quad, marker, sprites.divider);
                }
                return sprites.front == null ? quad : retexture(quad, marker, sprites.front);
            }
            return quad;
        }

        @Override
        public boolean isDynamic() {
            return true;
        }

        @Override
        public IIcon getParticle(BakedModelQuadContext context) {
            return sprites.exterior != null ? sprites.exterior : parent.getParticle(context);
        }

        @Override
        public Position.ModelDisplay getDisplay(Position pos, BakedModelQuadContext context) {
            return parent.getDisplay(pos, context);
        }

        @Override
        public int getColor(IBlockAccess world, int x, int y, int z, Block block, int meta, Random random) {
            return parent.getColor(world, x, y, z, block, meta, random);
        }
    }

    /**
     * @return whether a block is a framed drawer
     */
    public static boolean isFramedDrawer(@Nullable Block block) {
        return block instanceof FramedDrawerBlock;
    }
}
