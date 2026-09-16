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
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.MonopartModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;
import com.hfstudio.functionalstorage.FunctionalStorage;

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

    @Override
    public BakedModel getModel(BakedModelQuadContext context) {
        BlockState state = context.getBlockState();
        BakedModel cached = models.getIfPresent(state);
        if (cached != null) {
            return cached;
        }
        BakedModel baked = bake(state);
        models.put(state.clone(), baked);
        return baked;
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
        FramedModelHolder.provider()
            .clearCache();
    }
}
