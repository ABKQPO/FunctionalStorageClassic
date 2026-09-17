package com.hfstudio.functionalstorage.client.gui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.client.model.ItemContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.FramedBlock;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class DrawerGuiTextures extends Gui implements IResourceManagerReloadListener {

    public static final DrawerGuiTextures INSTANCE = new DrawerGuiTextures();
    public static final ResourceLocation BACKGROUND = new ResourceLocation(
        "functionalstorage",
        "textures/gui/background.png");
    private static final ResourceLocation INVENTORY = new ResourceLocation(
        "minecraft",
        "textures/gui/container/inventory.png");
    private final Map<Block, ResourceLocation> fronts = new HashMap<>();
    private final Cache<FrontKey, List<ModelQuadView>> framedFronts = CacheBuilder.newBuilder()
        .maximumSize(256)
        .build();

    public record FrontKey(Block block, FramedDrawerStyle style) {}

    public void panel(int x, int y, int width, int height) {
        bind(BACKGROUND);
        region(x, y, width, height, 4, 4, 168, 72);
        region(x, y, width, 4, 0, 0, 176, 4);
        region(x, y + height - 4, width, 4, 0, 162, 176, 4);
        region(x, y + 4, 4, height - 8, 0, 4, 4, 72);
        region(x + width - 4, y + 4, 4, height - 8, 172, 4, 4, 72);
    }

    public void slot(int x, int y) {
        bind(INVENTORY);
        drawTexturedModalRect(x - 1, y - 1, 7, 83, 18, 18);
    }

    public void front(Block block, int x, int y) {
        ResourceLocation sprite = fronts.computeIfAbsent(block, this::loadFront);
        bind(TextureMap.locationBlocksTexture);
        IIcon icon = Minecraft.getMinecraft()
            .getTextureMapBlocks()
            .getAtlasSprite(sprite.toString());
        drawTexturedModelRectFromIcon(x, y, icon, 48, 48);
    }

    public void front(ControllableDrawerTile tile, int x, int y) {
        if (!(tile.getBlockType() instanceof FramedBlock)) {
            front(tile.getBlockType(), x, y);
            return;
        }
        FrontKey key = new FrontKey(tile.getBlockType(), tile.getStyle());
        List<ModelQuadView> quads = framedFronts.getIfPresent(key);
        if (quads == null) {
            quads = framedQuads(key);
            framedFronts.put(key, quads);
        }
        bind(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (ModelQuadView quad : quads) for (int vertex = 0; vertex < 4; vertex++) {
            tessellator.addVertexWithUV(
                x + (1 - quad.getX(vertex)) * 48,
                y + (1 - quad.getY(vertex)) * 48,
                zLevel,
                quad.getTexU(vertex),
                quad.getTexV(vertex));
        }
        tessellator.draw();
    }

    private List<ModelQuadView> framedQuads(FrontKey key) {
        ItemStack stack = new ItemStack(key.block());
        key.style()
            .applyDrawerStyle(stack);
        Random random = new Random(0);
        ItemContext context = new ItemContext();
        context.set(stack, random);
        BakedModel model = ModelRegistry.getBakedModel(context);
        List<ModelQuadView> quads = new ArrayList<>();
        for (ModelQuadFacing face : ModelQuadFacing.values()) {
            context.quadFacing = face;
            random.setSeed(0);
            for (ModelQuadView quad : model.getQuads(context)) {
                if (quad.getNormalFace() == ModelQuadFacing.NEG_Z
                    && (!(quad.celeritas$getSprite() instanceof IIcon icon) || !icon.getIconName()
                        .endsWith("fluid_inner")))
                    quads.add(quad);
            }
        }
        quads.sort(
            Comparator.comparingDouble((ModelQuadView quad) -> quad.getZ(0))
                .reversed());
        return List.copyOf(quads);
    }

    private ResourceLocation loadFront(Block block) {
        ResourceLocation name = new ResourceLocation(Block.blockRegistry.getNameForObject(block));
        ResourceLocation model = new ResourceLocation(
            name.getResourceDomain(),
            "models/blocks/" + name.getResourcePath() + ".json");
        try (InputStreamReader reader = new InputStreamReader(
            Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(model)
                .getInputStream(),
            StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader)
                .getAsJsonObject();
            ResourceLocation sprite = new ResourceLocation(
                json.getAsJsonObject("textures")
                    .get("front")
                    .getAsString());
            return sprite;
        } catch (IOException | RuntimeException error) {
            FunctionalStorage.LOG.error("Unable to load drawer GUI front {}", model, error);
            return new ResourceLocation("functionalstorage", "blocks/oak_front_1");
        }
    }

    private void region(int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
        func_152125_a(x, y, u, v, textureWidth, textureHeight, width, height, 256, 256);
    }

    private void bind(ResourceLocation texture) {
        GL11.glColor4f(1, 1, 1, 1);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        fronts.clear();
        framedFronts.invalidateAll();
    }
}
