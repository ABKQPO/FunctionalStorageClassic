package com.hfstudio.functionalstorage.client.render;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.model.ItemContext;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DrawerItemRenderer implements IItemRenderer, IResourceManagerReloadListener {

    private final ItemContext context = new ItemContext();
    private final Random random = new Random(0L);
    private final ModelISBRH quadRenderer = new ModelISBRH();

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        register();
    }

    private void register() {
        for (DrawerBlock block : RegistrationHandler.allDrawerBlocks()) {
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block), this);
        }
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        context.set(stack, random);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            BakedModel model = ModelRegistry.getBakedModel(context);
            applyDisplay(model, type);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            Block block = Block.getBlockFromItem(stack.getItem());
            int color = model.getColor(null, 0, 0, 0, block, stack.getItemDamage(), random);
            for (ModelQuadFacing face : ModelQuadFacing.values()) {
                context.quadFacing = face;
                random.setSeed(0L);
                for (ModelQuadView quad : model.getQuads(context)) {
                    int tint = quad.getColorIndex() < 0 ? color
                        : BlockColor.getColor(block, stack, quad.getColorIndex());
                    float shade = quad.hasDirectionalShading() ? ModelISBRH.diffuseLight(quad.getComputedFaceNormal())
                        : 1F;
                    tessellator.setColorOpaque_F(
                        (tint >> 16 & 255) / 255F * shade,
                        (tint >> 8 & 255) / 255F * shade,
                        (tint & 255) / 255F * shade);
                    quadRenderer.renderQuad(quad, 0F, 0F, 0F, tessellator, null);
                }
            }
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            context.reset();
        }
    }

    private void applyDisplay(BakedModel model, ItemRenderType type) {
        Position position = switch (type) {
            case INVENTORY -> Position.GUI;
            case EQUIPPED -> Position.THIRDPERSON_RIGHTHAND;
            case EQUIPPED_FIRST_PERSON -> Position.FIRSTPERSON_RIGHTHAND;
            default -> RenderItem.renderInFrame ? Position.FIXED : Position.GROUND;
        };
        var display = model.getDisplay(position, context);
        var rotation = display.rotation();
        var translation = display.translation();
        var scale = display.scale();
        if (type == ItemRenderType.INVENTORY) {
            GL11.glTranslatef(8F, 8F, 0F);
            GL11.glScalef(16F, -16F, 16F);
        } else if (type == ItemRenderType.ENTITY) {
            // Forge already applies the vanilla ground scale before calling this renderer.
            GL11.glScalef(4F, 4F, 4F);
        } else {
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        }
        GL11.glTranslatef(translation.x / 16F, translation.y / 16F, translation.z / 16F);
        GL11.glRotatef(rotation.x, 1F, 0F, 0F);
        GL11.glRotatef(rotation.y, 0F, 1F, 0F);
        GL11.glRotatef(rotation.z, 0F, 0F, 1F);
        GL11.glScalef(scale.x, scale.y, scale.z);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
    }
}
