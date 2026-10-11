package com.hfstudio.functionalstorage.client.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.MonopartModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.AdaptiveDrawerBlock;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveDrawerTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveEssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveFluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SideOnly(Side.CLIENT)
public class DrawerModelProvider implements IBlockModelProvider, IResourceManagerReloadListener {

    public static final DrawerModelProvider INSTANCE = new DrawerModelProvider();

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
        .create();

    private final Map<Block, StateModelMap> states = new ConcurrentHashMap<>();
    private final Cache<BlockState, BakedModel> models = CacheBuilder.newBuilder()
        .maximumSize(512)
        .build();
    private final Cache<BlockState, JSONVariant> adaptiveRotations = CacheBuilder.newBuilder()
        .maximumSize(512)
        .build();
    private final Cache<AdaptiveModelKey, BakedModel> adaptiveModels = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .build();

    private record AdaptiveModelKey(BlockState state, String model, int x, int y, boolean uvLock) {}

    @Override
    public BakedModel getModel(BakedModelQuadContext context) {
        BlockState state = context.getBlockState();
        if (context instanceof BakedModelQuadContext.World world) {
            BakedModel adaptive = adaptiveModel(world, state);
            if (adaptive != null) return adaptive;
        }
        BakedModel cached = models.getIfPresent(state);
        if (cached != null) {
            return cached;
        }
        BakedModel baked = bake(state);
        models.put(state.clone(), baked);
        return baked;
    }

    private BakedModel adaptiveModel(BakedModelQuadContext.World context, BlockState state) {
        Block block = state.getBlock();
        Object tile = context.getWorld()
            .getTileEntity(context.getX(), context.getY(), context.getZ());
        String modelName = adaptiveModelName(block, tile);
        if (modelName == null) return null;
        JSONVariant rotation = adaptiveRotations.getIfPresent(state);
        if (rotation == null) {
            StateModelMap map = states.computeIfAbsent(block, this::loadState);
            UnbakedModel selected = map.selectModel(state);
            if (!(selected instanceof MonopartDough single) || single.variants()
                .isEmpty()) return null;
            rotation = single.variants()
                .get(0)
                .thing();
            adaptiveRotations.put(state.clone(), rotation);
        }
        AdaptiveModelKey key = new AdaptiveModelKey(
            state.clone(),
            modelName,
            rotation.x(),
            rotation.y(),
            rotation.uvLock());
        BakedModel cached = adaptiveModels.getIfPresent(key);
        if (cached != null) return cached;
        JSONVariant variant = new JSONVariant(
            new ModelLoc(FunctionalStorage.MOD_ID, "blocks/" + modelName),
            rotation.x(),
            rotation.y(),
            rotation.uvLock());
        BakedModel baked = new FaceRotatedModel(ModelRegistry.getJSONModel(variant.model())).bake(variant);
        adaptiveModels.put(key, baked);
        return baked;
    }

    private static String adaptiveModelName(Block block, Object tile) {
        if (tile instanceof AdaptiveDrawerTile drawer && block instanceof AdaptiveDrawerBlock adaptive) {
            DrawerFaceLayout layout = drawer.getAdaptiveDisplayLayout();
            if (layout == null) layout = drawer.getDrawerLayout()
                .getFaceLayout();
            return "adaptive_" + adaptive.getWoodName()
                + "_drawer"
                + modelSuffix(layout)
                + (drawer.isAdaptiveDisplayPowered() ? "" : "_offline");
        }
        if (tile instanceof AdaptiveFluidDrawerTile fluid) {
            return "adaptive_fluid_" + fluid.getDrawerLayout()
                .getSlotCount() + (fluid.isAdaptiveDisplayPowered() ? "_online" : "_offline");
        }
        if (tile instanceof AdaptiveEssentiaDrawerTile essentia) {
            return "adaptive_essentia_" + essentia.getDrawerLayout()
                .getSlotCount() + (essentia.isAdaptiveDisplayPowered() ? "_online" : "_offline");
        }
        if (tile instanceof AdaptiveMatrixTile matrix) {
            return "adaptive_controller_" + (matrix.isPowered() ? "online" : "offline");
        }
        if (tile instanceof AdaptiveGridTile grid) {
            AdaptiveMatrixTile matrix = grid.getMatrix();
            String base = tile instanceof AdaptiveCraftingGridTile ? "adaptive_crafting_grid_" : "adaptive_grid_";
            return base + (matrix != null && matrix.isPowered() ? "online" : "offline");
        }
        return null;
    }

    private static String modelSuffix(DrawerFaceLayout layout) {
        return switch (layout.getSlotCount()) {
            case 1 -> "";
            case 2 -> "_2x1";
            default -> "_2x2";
        };
    }

    private BakedModel bake(BlockState state) {
        StateModelMap map = states.computeIfAbsent(state.getBlock(), this::loadState);
        UnbakedModel selected = map.selectModel(state);
        if (!(selected instanceof MonopartDough single)) {
            return selected.bake();
        }
        ObjectArrayList<Weighted<BakedModel>> variants = new ObjectArrayList<>();
        for (var weighted : single.variants()) {
            var variant = weighted.thing();
            BakedModel baked = new FaceRotatedModel(ModelRegistry.getJSONModel(variant.model())).bake(variant);
            variants.add(new Weighted<>(baked, weighted.weight()));
        }
        return variants.size() == 1 ? variants.get(0)
            .thing() : new MonopartModel(variants);
    }

    private StateModelMap loadState(Block block) {
        ResourceLocation name = new ResourceLocation(Block.blockRegistry.getNameForObject(block));
        ResourceLocation resource = new ResourceLocation(
            name.getResourceDomain(),
            "blockstates/" + name.getResourcePath() + ".json");
        try (Reader reader = new InputStreamReader(
            Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(resource)
                .getInputStream(),
            StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, StateModelMap.class);
        } catch (IOException | RuntimeException exception) {
            FunctionalStorage.LOG.error("Unable to load drawer blockstate {}", resource, exception);
            return state -> data -> ModelRegistry.getBakedModel(state);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        states.clear();
        models.invalidateAll();
        adaptiveRotations.invalidateAll();
        adaptiveModels.invalidateAll();
        FramedModelHolder.provider()
            .clearCache();
    }
}
