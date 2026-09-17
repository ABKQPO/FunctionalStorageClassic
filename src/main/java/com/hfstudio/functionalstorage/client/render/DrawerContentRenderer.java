package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DrawerContentRenderer {

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final RenderBlocks inventoryBlocks = new RenderBlocks();
    private final RenderItem renderer = new RenderItem() {

        private final RenderBlocks guiBlocks = new RenderBlocks();

        @Override
        public void renderItemIntoGUI(FontRenderer fontRenderer, TextureManager textureManager, ItemStack stack, int x,
            int y, boolean renderEffect) {
            if (stack.getItemSpriteNumber() == 0 && RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(stack.getItem())
                    .getRenderType())) {
                renderBlockIntoGUI(textureManager, stack, x, y);
                return;
            }

            Item item = stack.getItem();
            boolean multipleRenderPasses = item.requiresMultipleRenderPasses();
            ResourceLocation texture = multipleRenderPasses ? item.getSpriteNumber() == 0 ? TextureMap.locationBlocksTexture
                : TextureMap.locationItemsTexture : textureManager.getResourceLocation(stack.getItemSpriteNumber());
            int renderPasses = item.getRenderPasses(stack.getItemDamage());
            for (int pass = 0; pass < renderPasses; pass++) {
                textureManager.bindTexture(texture);
                IIcon icon = multipleRenderPasses ? item.getIcon(stack, pass) : stack.getIconIndex();
                if (icon == null) {
                    continue;
                }
                int color = item.getColorFromItemStack(stack, pass);
                if (renderWithColor) {
                    GL11.glColor4f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
                }
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(-1F, -1F);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                renderIcon(x, y, icon, 16, 16);
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LIGHTING);
                if (renderEffect && stack.hasEffect(pass)) {
                    renderEffect(textureManager, x, y);
                }
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }
        }

        private void renderBlockIntoGUI(TextureManager textureManager, ItemStack stack, int x, int y) {
            Block block = Block.getBlockFromItem(stack.getItem());
            textureManager.bindTexture(TextureMap.locationBlocksTexture);
            if (block.getRenderBlockPass() != 0) {
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            } else {
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
                GL11.glDisable(GL11.GL_BLEND);
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(x - 2F, y + 3F, zLevel - 3F);
            GL11.glScalef(10F, 10F, 10F);
            GL11.glTranslatef(1F, 0.5F, 1F);
            GL11.glScalef(1F, 1F, -1F);
            GL11.glRotatef(210F, 1F, 0F, 0F);
            GL11.glRotatef(45F, 0F, 1F, 0F);
            int color = stack.getItem()
                .getColorFromItemStack(stack, 0);
            if (renderWithColor) {
                GL11.glColor4f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
            }
            GL11.glRotatef(-90F, 0F, 1F, 0F);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1F, -1F);
            guiBlocks.useInventoryTint = renderWithColor;
            guiBlocks.renderBlockAsItem(block, stack.getItemDamage(), 1F);
            guiBlocks.useInventoryTint = true;
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            if (block.getRenderBlockPass() == 0) {
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            }
            GL11.glPopMatrix();
        }
    };

    public void render(ItemStack stack, float x, float y, float scale, boolean threeDimensional) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        boolean raised = threeDimensional && stack.getItem() instanceof ItemBlock
            && (RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(stack.getItem())
                    .getRenderType())
                || MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED) != null);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            Minecraft minecraft = Minecraft.getMinecraft();
            if (raised) {
                // Seat the full model in the face instead of floating it ahead of the label.
                GL11.glTranslatef(x, y, 0.002F - scale / 3f);
                GL11.glScalef(scale / 1.4f, -scale / 1.4f, -scale / 1.4f);
                RenderHelper.enableStandardItemLighting();
                minecraft.entityRenderer.itemRenderer
                    .renderItem(minecraft.thePlayer, stack, 0, ItemRenderType.EQUIPPED);
            } else {
                GL11.glTranslatef(x, y, 0.002F);
                GL11.glScalef(scale / 16F, scale / 16F, 0.0001F);
                modelView.clear();
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
                // Keep flattening and face rotation out of GUI normal and light transforms.
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glMultMatrix(modelView);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadIdentity();
                renderGuiItem(minecraft, stack);
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopAttrib();
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private void renderGuiItem(Minecraft minecraft, ItemStack stack) {
        float previousDepth = renderer.zLevel;
        try {
            renderer.zLevel = 0F;
            RenderHelper.enableGUIStandardItemLighting();
            if (!ForgeHooksClient.renderInventoryItem(
                inventoryBlocks,
                minecraft.getTextureManager(),
                stack,
                true,
                renderer.zLevel,
                -8F,
                -8F)) {
                renderer.renderItemIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, -8, -8, true);
            }
        } finally {
            renderer.zLevel = previousDepth;
        }
    }
}
