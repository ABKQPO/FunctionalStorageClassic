package com.hfstudio.functionalstorage.client.model;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.hfstudio.functionalstorage.common.block.FramedDrawerBlock;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Replaces framed marker textures with selected materials and remaps sprite-local UVs. */
@SideOnly(Side.CLIENT)
public class FramedDrawerModelProvider implements IBlockModelProvider {

    private static final String SIDE_MARKER = "functionalstorage:blocks/framed_side";
    private static final String FRONT_MARKER = "functionalstorage:blocks/framed_front_";

    private final Cache<String, PartSprites> spriteCache = CacheBuilder.newBuilder()
        .maximumSize(256)
        .build();
    private final Cache<ModelKey, BakedModel> modelCache = CacheBuilder.newBuilder()
        .maximumSize(512)
        .build();

    public record ModelKey(BakedModel parent, PartSprites sprites) {}

    public void clearCache() {
        spriteCache.invalidateAll();
        modelCache.invalidateAll();
    }

    @Override
    public BakedModel getModel(BakedModelQuadContext context) {
        return wrap(context, DrawerModelProvider.INSTANCE.getModel(context));
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
        PartSprites sprites = context instanceof BakedModelQuadContext.World world ? spritesAt(world)
            : context instanceof BakedModelQuadContext.Item item ? spritesFor(item.getItemStack()) : null;
        if (sprites == null) {
            return base;
        }
        ModelKey key = new ModelKey(base, sprites);
        BakedModel cached = modelCache.getIfPresent(key);
        if (cached == null) {
            cached = new FramedBakedModel(base, sprites);
            modelCache.put(key, cached);
        }
        return cached;
    }

    @Nullable
    public PartSprites spritesFor(@Nonnull ItemStack stack) {
        FramedDrawerStyle style = FramedDrawerStyle.fromDrawerStack(stack);
        return style.isConfigured() ? resolve(style, 0) : null;
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
        return style.isConfigured()
            ? resolve(style, world.getBlockMetadata(context.getX(), context.getY(), context.getZ()))
            : null;
    }

    @Nonnull
    private PartSprites resolve(@Nonnull FramedDrawerStyle style, int metadata) {
        String key = style.getCacheKey() + "/" + metadata;
        PartSprites cached = spriteCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        PartSprites resolved = PartSprites.from(style, metadata);
        spriteCache.put(key, resolved);
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
    private static boolean isDivider(@Nonnull ModelQuadView quad, TextureAtlasSprite marker) {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float u = (quad.getTexU(vertex) - marker.getMinU()) * 16F / (marker.getMaxU() - marker.getMinU());
            float v = (quad.getTexV(vertex) - marker.getMinV()) * 16F / (marker.getMaxV() - marker.getMinV());
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

        private final TextureAtlasSprite[] exteriorFaces = new TextureAtlasSprite[6];
        private final TextureAtlasSprite[] frontFaces = new TextureAtlasSprite[6];
        private final TextureAtlasSprite[] dividerFaces = new TextureAtlasSprite[6];

        public PartSprites(@Nullable TextureAtlasSprite exterior, @Nullable TextureAtlasSprite front,
            @Nullable TextureAtlasSprite divider) {
            this.exterior = exterior;
            this.front = front;
            this.divider = divider;
            Arrays.fill(exteriorFaces, exterior);
            Arrays.fill(frontFaces, front);
            Arrays.fill(dividerFaces, divider);
        }

        @Nonnull
        public static PartSprites from(@Nonnull FramedDrawerStyle style) {
            return from(style, 0);
        }

        private static PartSprites from(FramedDrawerStyle style, int metadata) {
            PartSprites sprites = new PartSprites(
                spriteFor(style.getExterior()),
                spriteFor(style.getFront()),
                spriteFor(style.getDivider()));
            for (int side = 0; side < 6; side++) {
                int localSide = localSide(ForgeDirection.getOrientation(side), metadata);
                sprites.exteriorFaces[side] = spriteFor(style.getExterior(), localSide);
                sprites.frontFaces[side] = spriteFor(style.getFront(), localSide);
                sprites.dividerFaces[side] = spriteFor(style.getDivider(), localSide);
            }
            return sprites;
        }

        private static int localSide(ForgeDirection side, int metadata) {
            int x = side.offsetX;
            int y = side.offsetY;
            int z = side.offsetZ;
            for (int turn = 0; turn < (metadata & 3); turn++) {
                int previousX = x;
                x = z;
                z = -previousX;
            }
            int attachment = (metadata & 12) >> 2;
            if (attachment == 1) {
                int previousY = y;
                y = z;
                z = -previousY;
            } else if (attachment == 2) {
                int previousY = y;
                y = -z;
                z = previousY;
            }
            for (ForgeDirection candidate : ForgeDirection.VALID_DIRECTIONS) {
                if (candidate.offsetX == x && candidate.offsetY == y && candidate.offsetZ == z) {
                    return candidate.ordinal();
                }
            }
            return side.ordinal();
        }

        @Nullable
        public static TextureAtlasSprite spriteFor(@Nullable ItemStack material) {
            return spriteFor(material, ForgeDirection.NORTH.ordinal());
        }

        @Nullable
        public static TextureAtlasSprite spriteFor(@Nullable ItemStack material, int side) {
            if (material == null || material.getItem() == null || !(material.getItem() instanceof ItemBlock)) {
                return null;
            }
            Block block = ((ItemBlock) material.getItem()).field_150939_a;
            if (block == null) {
                return null;
            }
            IIcon icon = block.getIcon(
                side,
                material.getItem()
                    .getMetadata(material.getItemDamage()));
            return icon instanceof TextureAtlasSprite ? (TextureAtlasSprite) icon : null;
        }

        public boolean isUsable() {
            return exterior != null && front != null;
        }
    }

    public static class FramedBakedModel implements BakedModel {

        private final BakedModel parent;
        private final PartSprites sprites;
        private final Map<List<ModelQuadView>, List<ModelQuadView>> quads = new ConcurrentHashMap<>();

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
            return parent.isDynamic() ? retextureQuads(original)
                : quads.computeIfAbsent(original, this::retextureQuads);
        }

        private List<ModelQuadView> retextureQuads(List<ModelQuadView> original) {
            List<ModelQuadView> retextured = new ArrayList<>(original.size());
            for (ModelQuadView quad : original) {
                ModelQuadView replaced = retextureQuad(quad);
                retextured.add(parent.isDynamic() && replaced == quad ? new ModelQuad(quad) : replaced);
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
            int side = quad.getLightFace()
                .toForgeDir()
                .ordinal();
            if (side >= 6) {
                side = ForgeDirection.NORTH.ordinal();
            }
            if (isSideMarker(iconName)) {
                return sprites.exteriorFaces[side] == null ? quad
                    : retexture(quad, marker, sprites.exteriorFaces[side]);
            }
            if (isFrontMarker(iconName)) {
                if (isDivider(quad, marker)) {
                    return sprites.dividerFaces[side] == null ? quad
                        : retexture(quad, marker, sprites.dividerFaces[side]);
                }
                return sprites.frontFaces[side] == null ? quad : retexture(quad, marker, sprites.frontFaces[side]);
            }
            return quad;
        }

        @Override
        public boolean isDynamic() {
            return parent.isDynamic();
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

    public static boolean isFramedDrawer(@Nullable Block block) {
        return block instanceof FramedDrawerBlock;
    }
}
