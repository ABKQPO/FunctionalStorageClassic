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
import com.hfstudio.functionalstorage.common.block.AdaptiveCraftingGridBlock;
import com.hfstudio.functionalstorage.common.block.AdaptiveDrawerBlock;
import com.hfstudio.functionalstorage.common.block.AdaptiveEssentiaDrawerBlock;
import com.hfstudio.functionalstorage.common.block.AdaptiveFluidDrawerBlock;
import com.hfstudio.functionalstorage.common.block.AdaptiveGridBlock;
import com.hfstudio.functionalstorage.common.block.controller.AdaptiveMatrixBlock;
import com.hfstudio.functionalstorage.common.model.DrawerModelStateCache;

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
        Integer cachedKey = DrawerModelStateCache
            .get(context.getWorld(), context.getX(), context.getY(), context.getZ());
        String modelName = adaptiveModelName(block, cachedKey == null ? 0 : cachedKey);
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

    private static String adaptiveModelName(Block block, int modelKey) {
        boolean powered = (modelKey & 1) != 0;
        int slotCount = adaptiveSlotCount(modelKey);
        if (block instanceof AdaptiveDrawerBlock adaptive) {
            return "adaptive_" + adaptive.getWoodName()
                + "_drawer"
                + modelSuffix(slotCount)
                + (powered ? "" : "_offline");
        }
        if (block instanceof AdaptiveFluidDrawerBlock) {
            return "adaptive_fluid_" + slotCount + (powered ? "_online" : "_offline");
        }
        if (block instanceof AdaptiveEssentiaDrawerBlock) {
            return "adaptive_essentia_" + slotCount + (powered ? "_online" : "_offline");
        }
        if (block instanceof AdaptiveMatrixBlock) {
            return "adaptive_controller_" + (powered ? "online" : "offline");
        }
        if (block instanceof AdaptiveCraftingGridBlock) {
            return "adaptive_crafting_grid_" + (powered ? "online" : "offline");
        }
        if (block instanceof AdaptiveGridBlock) {
            return "adaptive_grid_" + (powered ? "online" : "offline");
        }
        return null;
    }

    private static int adaptiveSlotCount(int modelKey) {
        int slotCount = modelKey >> 1;
        return slotCount == 2 ? 2 : slotCount >= 4 ? 4 : 1;
    }

    private static String modelSuffix(int slotCount) {
        return switch (slotCount) {
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
