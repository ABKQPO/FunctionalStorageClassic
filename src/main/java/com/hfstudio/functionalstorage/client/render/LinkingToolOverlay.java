package com.hfstudio.functionalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.common.item.LinkingToolItem;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LinkingToolOverlay {

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof LinkingToolItem)) {
            return;
        }
        int[] target = LinkingToolItem.getTarget(held);
        if (target.length != 4 || target[3] != player.dimension
            || !player.worldObj.blockExists(target[0], target[1], target[2])
            || !(player.worldObj
                .getTileEntity(target[0], target[1], target[2]) instanceof DrawerControllerTile controller)) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            EntityLivingBase camera = minecraft.renderViewEntity;
            if (camera == null) camera = player;
            GL11.glTranslated(
                -(camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks),
                -(camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks),
                -(camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks));
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2F);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            outline(target[0], target[1], target[2], 0xFF2883FA);
            int[] first = LinkingToolItem.getFirstPosition(held);
            MovingObjectPosition hit = camera.rayTrace(8D, event.partialTicks);
            if (first.length == 4 && first[3] == player.dimension
                && hit != null
                && hit.typeOfHit == MovingObjectType.BLOCK) {
                RenderGlobal.drawOutlinedBoundingBox(
                    AxisAlignedBB.getBoundingBox(
                        Math.min(first[0], hit.blockX) - 0.002D,
                        Math.min(first[1], hit.blockY) - 0.002D,
                        Math.min(first[2], hit.blockZ) - 0.002D,
                        Math.max(first[0], hit.blockX) + 1.002D,
                        Math.max(first[1], hit.blockY) + 1.002D,
                        Math.max(first[2], hit.blockZ) + 1.002D),
                    0xFFFFFF);
                return;
            }
            for (long position : controller.getDrawers()) {
                outline(
                    (int) (position >> 38),
                    (int) (position >> 26 & 0xFFF),
                    (int) (position << 38 >> 38),
                    0xFF00FFAA);
            }
            int range = controller.getLinkingRange();
            RenderGlobal.drawOutlinedBoundingBox(
                AxisAlignedBB.getBoundingBox(
                    target[0] - range - 0.002D,
                    Math.max(0, target[1] - range) - 0.002D,
                    target[2] - range - 0.002D,
                    target[0] + range + 1.002D,
                    Math.min(256, target[1] + range + 1) + 0.002D,
                    target[2] + range + 1.002D),
                0x80FF80);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void outline(int x, int y, int z, int color) {
        RenderGlobal.drawOutlinedBoundingBox(
            AxisAlignedBB.getBoundingBox(x - 0.002D, y - 0.002D, z - 0.002D, x + 1.002D, y + 1.002D, z + 1.002D),
            color);
    }
}
