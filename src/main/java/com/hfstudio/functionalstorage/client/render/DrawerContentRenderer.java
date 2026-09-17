package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
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
        float previousDepth = renderer.zLevel;
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
                GL11.glScalef(scale / 16F, scale / 16F, 0.00001F);
                modelView.clear();
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
                // Keep flattening and face rotation out of GUI normal and light transforms.
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glMultMatrix(modelView);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadIdentity();
                renderer.zLevel = -50F;
                RenderHelper.enableGUIStandardItemLighting();
                renderer
                    .renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, -8, -8);
            }
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
