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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.ItemContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFlags;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.FramedBlock;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Replaces framed marker textures with selected materials and remaps sprite-local UVs. */
@SideOnly(Side.CLIENT)
public class FramedDrawerModelProvider implements IBlockModelProvider, IBlockColor {

    private static final String SIDE_MARKER = FunctionalStorage.MOD_ID + ":blocks/framed_side";
    private static final String FRONT_MARKER = FunctionalStorage.MOD_ID + ":blocks/framed_front_";

    private final Cache<SpriteKey, PartSprites> spriteCache = CacheBuilder.newBuilder()
        .maximumSize(256)
        .build();
    private final Cache<ModelKey, BakedModel> modelCache = CacheBuilder.newBuilder()
        .maximumSize(512)
        .build();

    public record ModelKey(BakedModel parent, PartSprites sprites) {}

    public record SpriteKey(FramedDrawerStyle style, int metadata) {}

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
     * @return the material-aware model, including separation of opaque and translucent faces
     */
    @Nonnull
    public BakedModel wrap(@Nonnull BakedModelQuadContext context, @Nonnull BakedModel base) {
        PartSprites sprites = context instanceof BakedModelQuadContext.World world ? spritesAt(world)
            : context instanceof BakedModelQuadContext.Item item ? spritesFor(item.getItemStack()) : null;
        if (sprites == null) sprites = PartSprites.EMPTY;
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

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        if (world == null || !(world.getTileEntity(x, y, z) instanceof ControllableDrawerTile tile)) return 0xFFFFFF;
        return resolve(tile.getStyle(), world.getBlockMetadata(x, y, z)).color(tintIndex);
    }

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        PartSprites sprites = stack == null ? null : spritesFor(stack);
        return sprites == null ? 0xFFFFFF : sprites.color(tintIndex);
    }

    @Nullable
    private PartSprites spritesAt(@Nonnull BakedModelQuadContext.World context) {
        IBlockAccess world = context.getWorld();
        if (world == null) {
            return null;
        }
        Object tile = world.getTileEntity(context.getX(), context.getY(), context.getZ());
        if (!(tile instanceof ControllableDrawerTile)) {
            return null;
        }
        FramedDrawerStyle style = ((ControllableDrawerTile) tile).getStyle();
        return style.isConfigured()
            ? resolve(style, world.getBlockMetadata(context.getX(), context.getY(), context.getZ()))
            : null;
    }

    @Nonnull
    private PartSprites resolve(@Nonnull FramedDrawerStyle style, int metadata) {
        SpriteKey key = new SpriteKey(style, metadata);
        PartSprites cached = spriteCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        PartSprites resolved = PartSprites.from(style, metadata);
        spriteCache.put(key, resolved);
        return resolved;
    }

    private static boolean isSideMarker(@Nonnull String iconName) {
        return iconName.startsWith(SIDE_MARKER)
            || iconName.startsWith(FunctionalStorage.MOD_ID + ":blocks/framed_part_side_");
    }

    private static boolean isFrontMarker(@Nonnull String iconName) {
        return iconName.startsWith(FRONT_MARKER)
            || iconName.startsWith(FunctionalStorage.MOD_ID + ":blocks/framed_part_front_");
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
    private static boolean isDivider(@Nonnull ModelQuadView quad, IIcon marker) {
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
    public static ModelQuad retexture(@Nonnull ModelQuadView quad, @Nonnull IIcon marker, @Nonnull IIcon replacement) {
        ModelQuad copy = copyQuad(quad);
        float markerWidth = marker.getMaxU() - marker.getMinU();
        float markerHeight = marker.getMaxV() - marker.getMinV();
        for (int vertex = 0; vertex < 4; vertex++) {
            float relativeU = markerWidth == 0F ? 0F : (quad.getTexU(vertex) - marker.getMinU()) / markerWidth;
            float relativeV = markerHeight == 0F ? 0F : (quad.getTexV(vertex) - marker.getMinV()) / markerHeight;
            copy.setTexU(vertex, replacement.getInterpolatedU(clamp(relativeU) * 16));
            copy.setTexV(vertex, replacement.getInterpolatedV(clamp(relativeV) * 16));
        }
        copy.setSprite(
            replacement instanceof MaterialIcon icon ? icon.getSprite()
                : replacement instanceof TextureAtlasSprite ? replacement : new MaterialIcon(replacement).getSprite());
        // Invalidate sprite-derived flags while preserving geometry and shading flags.
        copy.setFlags(
            quad.getFlags() & ~(ModelQuadFlags.IS_TRUSTED_SPRITE | ModelQuadFlags.IS_PASS_OPTIMIZABLE
                | ModelQuadFlags.IS_POPULATED));
        return copy;
    }

    private static ModelQuad copyQuad(ModelQuadView quad) {
        ModelQuad copy = new ModelQuad(quad);
        // GTNHLib 0.11.48 does not copy these lighting properties.
        copy.setDirectionalShading(quad.hasDirectionalShading());
        copy.setEmissiveness(quad.getEmissiveness());
        return copy;
    }

    private static float clamp(float value) {
        return value < 0F ? 0F : Math.min(value, 1F);
    }

    /**
     * Sprites for the three retextured parts of a framed drawer.
     */
    public static class PartSprites {

        private static final PartSprites EMPTY = new PartSprites(null, null, null);

        @Nullable
        public final IIcon exterior;
        @Nullable
        public final IIcon front;
        @Nullable
        public final IIcon divider;

        private final IIcon[] exteriorFaces = new IIcon[6];
        private final IIcon[] frontFaces = new IIcon[6];
        private final IIcon[] dividerFaces = new IIcon[6];
        private final int[] colors = { 0xFFFFFF, 0xFFFFFF, 0xFFFFFF };
        private final boolean[] translucent = new boolean[3];
        private final boolean[][] tinted = new boolean[3][6];

        public PartSprites(@Nullable IIcon exterior, @Nullable IIcon front, @Nullable IIcon divider) {
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
            ItemStack[] materials = { style.getExterior(), style.getFront(), style.getDivider() };
            PartSprites sprites = new PartSprites(
                spriteFor(materials[0]),
                spriteFor(materials[1]),
                spriteFor(materials[2]));
            IIcon[][] faces = { sprites.exteriorFaces, sprites.frontFaces, sprites.dividerFaces };
            for (int part = 0; part < materials.length; part++) {
                ItemStack material = materials[part];
                Block block = FramedDrawerStyle.materialBlock(material);
                if (block == null) continue;
                try {
                    // Facade colors come from the source item, not the drawer's block metadata.
                    sprites.colors[part] = BlockColor.getColor(block, material, 0);
                } catch (RuntimeException exception) {
                    FunctionalStorage.LOG.debug("Unable to resolve framed material color {}", material, exception);
                }
                sprites.translucent[part] = block.canRenderInPass(1);
                for (int side = 0; side < 6; side++) {
                    int localSide = localSide(ForgeDirection.getOrientation(side), metadata);
                    faces[part][side] = spriteFor(material, localSide);
                    sprites.tinted[part][side] = block != Blocks.grass || localSide == ForgeDirection.UP.ordinal();
                }
            }
            return sprites;
        }

        private int color(int tintIndex) {
            return tintIndex >= 0 && tintIndex < colors.length ? colors[tintIndex] : 0xFFFFFF;
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
        public static IIcon spriteFor(@Nullable ItemStack material) {
            return spriteFor(material, ForgeDirection.NORTH.ordinal());
        }

        @Nullable
        public static IIcon spriteFor(@Nullable ItemStack material, int side) {
            Block block = FramedDrawerStyle.materialBlock(material);
            if (block == null) return null;
            try {
                IIcon icon = block.getIcon(
                    side,
                    material.getItem()
                        .getMetadata(material.getItemDamage()));
                if (isMissing(icon) && block instanceof BlockModelInfo info && info.nhlib$isModeled()) {
                    ItemContext context = new ItemContext();
                    context.set(material, new Random(0));
                    icon = ModelRegistry.getBakedModel(context)
                        .getParticle(context);
                }
                if (icon == null && material.getItem()
                    .getSpriteNumber() == 0) icon = material.getIconIndex();
                if (icon != null) return new MaterialIcon(icon);
            } catch (RuntimeException exception) {
                FunctionalStorage.LOG
                    .debug("Unable to resolve framed material face {} for {}", side, material, exception);
            }
            return Minecraft.getMinecraft()
                .getTextureMapBlocks()
                .getAtlasSprite("missingno");
        }

        private static boolean isMissing(IIcon icon) {
            return icon == null || "missingno".equals(icon.getIconName());
        }

        public boolean isUsable() {
            return exterior != null && front != null;
        }
    }

    public static class FramedBakedModel implements BakedModel {

        private final BakedModel parent;
        private final PartSprites sprites;
        private final Map<List<ModelQuadView>, QuadPasses> quads = new ConcurrentHashMap<>();

        public FramedBakedModel(@Nonnull BakedModel parent, @Nonnull PartSprites sprites) {
            this.parent = parent;
            this.sprites = sprites;
        }

        @Override
        public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
            List<ModelQuadView> original = parent.getQuads(context);
            if (original == null || original.isEmpty()) {
                return original;
            }
            QuadPasses passes = parent.isDynamic() ? retextureQuads(original)
                : quads.computeIfAbsent(original, this::retextureQuads);
            if (!(context instanceof BakedModelQuadContext.World)) return passes.all();
            return ForgeHooksClient.getWorldRenderPass() == 1 ? passes.translucent() : passes.opaque();
        }

        private QuadPasses retextureQuads(List<ModelQuadView> original) {
            List<ModelQuadView> retextured = new ArrayList<>(original.size());
            List<ModelQuadView> opaque = new ArrayList<>(original.size());
            List<ModelQuadView> translucent = new ArrayList<>();
            for (ModelQuadView quad : original) {
                ModelQuadView replaced = retextureQuad(quad);
                if (parent.isDynamic() && replaced == quad) replaced = copyQuad(quad);
                retextured.add(replaced);
                (replaced.isTransparent() ? translucent : opaque).add(replaced);
            }
            return new QuadPasses(List.copyOf(retextured), List.copyOf(opaque), List.copyOf(translucent));
        }

        @NotNull
        private ModelQuadView retextureQuad(@Nonnull ModelQuadView quad) {
            Object spriteObject = quad.celeritas$getSprite();
            if (!(spriteObject instanceof IIcon marker)) {
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
            if (iconName.startsWith(FunctionalStorage.MOD_ID + ":blocks/framed_part_divider_")) {
                return materialQuad(quad, marker, sprites.dividerFaces[side], 2, side);
            }
            if (isSideMarker(iconName)) {
                return materialQuad(quad, marker, sprites.exteriorFaces[side], 0, side);
            }
            if (isFrontMarker(iconName)) {
                if (!iconName.startsWith(FunctionalStorage.MOD_ID + ":blocks/framed_part_front_")
                    && isDivider(quad, marker)) {
                    return materialQuad(quad, marker, sprites.dividerFaces[side], 2, side);
                }
                return materialQuad(quad, marker, sprites.frontFaces[side], 1, side);
            }
            return quad;
        }

        private ModelQuadView materialQuad(ModelQuadView quad, IIcon marker, IIcon icon, int part, int side) {
            if (icon == null) return quad;
            ModelQuad copy = retexture(quad, marker, icon);
            copy.setColorIndex(sprites.tinted[part][side] ? part : -1);
            if (sprites.translucent[part]) copy.setTransparent();
            return copy;
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

    public record QuadPasses(List<ModelQuadView> all, List<ModelQuadView> opaque, List<ModelQuadView> translucent) {}

    public static boolean isFramedDrawer(@Nullable Block block) {
        return block instanceof FramedBlock;
    }
}
