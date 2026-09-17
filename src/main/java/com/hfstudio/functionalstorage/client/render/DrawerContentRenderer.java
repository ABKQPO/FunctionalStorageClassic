package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DrawerContentRenderer {

    private final RenderItem renderer = new RenderItem();
    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);

    public void render(ItemStack stack, float x, float y, float scale, boolean threeDimensional) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        IItemRenderer custom = MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.INVENTORY);
        boolean nativeBlock = stack.getItem() instanceof ItemBlock
            && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(stack.getItem()).getRenderType());
        boolean blockHelper = custom != null
            && custom.shouldUseRenderHelper(ItemRenderType.INVENTORY, stack, ItemRendererHelper.INVENTORY_BLOCK);
        boolean raised = threeDimensional && stack.getItem() instanceof ItemBlock && (nativeBlock || custom != null);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, raised ? scale / 12F + 0.002F : 0.002F);
        GL11.glScalef(scale / 16F, scale / 16F, raised ? scale / 96F : 0.00001F);
        if (raised) {
            // Center the native block helper or Forge's custom inventory origin on the same plane.
            GL11.glTranslatef(0F, 0F, blockHelper || custom == null && nativeBlock ? -7F : 3F);
        }
        modelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        // Keep flattening and face rotation out of the normal and light transforms.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMultMatrix(modelView);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        float previousDepth = renderer.zLevel;
        try {
            renderer.zLevel = -50F;
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            Minecraft minecraft = Minecraft.getMinecraft();
            renderer.renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, -8, -8);
        } finally {
            renderer.zLevel = previousDepth;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopAttrib();
            GL11.glMatrixMode(previousMatrixMode);
        }
    }
}
