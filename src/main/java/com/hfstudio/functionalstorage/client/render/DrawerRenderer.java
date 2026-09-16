package com.hfstudio.functionalstorage.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.common.block.DrawerAttachment;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.compact.CompactingDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.NumberUtils;

import thaumcraft.api.aspects.Aspect;

/**
 * Draws the stored icon, amount, and fill indicator onto a drawer's front face.
 * The face transform is derived from the block metadata, so wall, floor, and
 * ceiling placements all render upright and correctly mirrored.
 */
public class DrawerRenderer extends TileEntitySpecialRenderer {

    private static final float Z_ICON = 0.98F;
    private static final float Z_TEXT = 0.985F;
    private static final float Z_INDICATOR = 0.99F;
    private static final float ICON_HALF_EXTENT = 0.15F;
    private static final float INDICATOR_HALF_WIDTH = 0.24F;
    private static final float INDICATOR_HALF_HEIGHT = 0.02F;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof ControllableDrawerTile) || !(tile.getBlockType() instanceof DrawerBlock)) {
            return;
        }
        if (!isWithinRenderRange(tile)) {
            return;
        }

        DrawerBlock block = (DrawerBlock) tile.getBlockType();
        int metadata = tile.getBlockMetadata();
        applyBrightness(tile);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        applyFaceTransform(
            DrawerBlock.getAttachment(metadata),
            DrawerBlock.getHorizontalFacing(metadata),
            block.getFaceLayout());

        DrawerOptions options = ((ControllableDrawerTile) tile).getDrawerOptions();
        if (tile instanceof FluidDrawerTile) {
            renderFluidDrawer((FluidDrawerTile) tile, block.getFaceLayout(), options);
        } else if (tile instanceof EssentiaDrawerTile) {
            renderEssentiaDrawer((EssentiaDrawerTile) tile, block.getFaceLayout(), options);
        } else if (tile instanceof CompactingDrawerTile) {
            renderCompactingDrawer((CompactingDrawerTile) tile, block.getFaceLayout(), options);
        } else if (tile instanceof WoodDrawerTile) {
            renderItemDrawer((WoodDrawerTile) tile, block.getFaceLayout(), options);
        }

        GL11.glPopMatrix();
    }

    private void renderCompactingDrawer(CompactingDrawerTile tile, DrawerFaceLayout layout, DrawerOptions options) {
        for (int slot = 0; slot < slotCount(layout); slot++) {
            BigItemStack snapshot = tile.getItemHandler()
                .getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            ItemStack stack = snapshot.getTemplate();
            long capacity = tile.getItemHandler()
                .getCapacity(slot);
            float[] center = slotCenter(layout, slot);
            float scale = iconScale(layout);
            if (options.isShowItemRender()) {
                renderStack(stack, center[0], center[1], scale);
            }
            if (options.isShowItemCount()) {
                renderText(NumberUtils.formatCompact(snapshot.getAmount()), center[0], center[1]);
            }
            renderIndicator(center[0], center[1], ratio(snapshot.getAmount(), capacity), options);
        }
    }

    private void renderEssentiaDrawer(EssentiaDrawerTile tile, DrawerFaceLayout layout, DrawerOptions options) {
        for (int slot = 0; slot < slotCount(layout); slot++) {
            BigAspectStack snapshot = tile.getAspectHandler()
                .getSnapshot(slot);
            Aspect aspect = snapshot.getAspect();
            if (aspect == null) {
                continue;
            }
            long capacity = tile.getAspectHandler()
                .getCapacity(slot);
            float[] center = slotCenter(layout, slot);
            float scale = iconScale(layout);
            if (options.isShowItemRender()) {
                renderAspect(aspect, center[0], center[1], scale);
            }
            if (options.isShowItemCount()) {
                renderText(NumberUtils.formatAspect(snapshot.getAmount()), center[0], center[1]);
            }
            renderIndicator(center[0], center[1], ratio(snapshot.getAmount(), capacity), options);
        }
    }

    private void applyBrightness(TileEntity tile) {
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(tile.xCoord + 1, tile.yCoord + 1, tile.zCoord + 1, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
    }

    private boolean isWithinRenderRange(TileEntity tile) {
        if (Minecraft.getMinecraft().thePlayer == null) {
            return false;
        }
        double range = FunctionalStorageConfig.CLIENT.drawerRenderRange;
        return tile.getDistanceFrom(
            Minecraft.getMinecraft().thePlayer.posX,
            Minecraft.getMinecraft().thePlayer.posY,
            Minecraft.getMinecraft().thePlayer.posZ) <= range * range;
    }

    private void renderItemDrawer(WoodDrawerTile tile, DrawerFaceLayout layout, DrawerOptions options) {
        for (int slot = 0; slot < slotCount(layout); slot++) {
            BigItemStack snapshot = tile.getItemHandler()
                .getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            ItemStack stack = snapshot.getTemplate();
            long capacity = tile.getItemHandler()
                .getCapacity(slot);
            float[] center = slotCenter(layout, slot);
            float scale = iconScale(layout);
            if (options.isShowItemRender()) {
                renderStack(stack, center[0], center[1], scale);
            }
            if (options.isShowItemCount()) {
                renderText(NumberUtils.formatCompact(snapshot.getAmount()), center[0], center[1]);
            }
            renderIndicator(center[0], center[1], ratio(snapshot.getAmount(), capacity), options);
        }
    }

    private void renderFluidDrawer(FluidDrawerTile tile, DrawerFaceLayout layout, DrawerOptions options) {
        for (int slot = 0; slot < slotCount(layout); slot++) {
            BigFluidStack snapshot = tile.getFluidHandler()
                .getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            FluidStack fluid = snapshot.getTemplate();
            long capacity = tile.getFluidHandler()
                .getCapacity(slot);
            float[] center = slotCenter(layout, slot);
            float scale = iconScale(layout);
            if (options.isShowItemRender()) {
                renderFluid(fluid, center[0], center[1], scale);
            }
            if (options.isShowItemCount()) {
                renderText(NumberUtils.formatFluid(snapshot.getAmount()), center[0], center[1]);
            }
            renderIndicator(center[0], center[1], ratio(snapshot.getAmount(), capacity), options);
        }
    }

    private int slotCount(DrawerFaceLayout layout) {
        switch (layout) {
            case X_2:
                return 2;
            case X_4:
                return 4;
            default:
                return 1;
        }
    }

    private float iconScale(DrawerFaceLayout layout) {
        return layout == DrawerFaceLayout.X_1 ? 0.5F : 0.25F;
    }

    private float ratio(long amount, long capacity) {
        return capacity <= 0L ? 0F : (float) Math.min(1D, amount / (double) capacity);
    }

    private float[] slotCenter(DrawerFaceLayout layout, int slot) {
        switch (layout) {
            case X_2:
                return new float[] { 0.5F, slot == 0 ? 0.25F : 0.75F };
            case X_4:
                return new float[] { slot % 2 == 0 ? 0.25F : 0.75F, slot < 2 ? 0.25F : 0.75F };
            default:
                return new float[] { 0.5F, 0.5F };
        }
    }

    /**
     * Rotates the render space so local coordinates match the drawer's front
     * face regardless of how the block was placed.
     *
     * @param attachment surface the drawer is mounted on
     * @param facing     horizontal rotation stored in metadata
     * @param layout     face layout, used to pick the front plane depth
     */
    private void applyFaceTransform(DrawerAttachment attachment, ForgeDirection facing, DrawerFaceLayout layout) {
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(180F - facingAngle(facing), 0F, 1F, 0F);
        GL11.glTranslated(0D, 0D, 0.5D);
        switch (attachment) {
            case FLOOR:
                GL11.glRotatef(-90F, 1F, 0F, 0F);
                break;
            case CEILING:
                GL11.glRotatef(90F, 1F, 0F, 0F);
                break;
            default:
                break;
        }
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
    }

    private float facingAngle(ForgeDirection facing) {
        switch (facing) {
            case SOUTH:
                return 0F;
            case WEST:
                return 90F;
            case NORTH:
                return 180F;
            case EAST:
                return 270F;
            default:
                return 0F;
        }
    }

    private void renderStack(ItemStack stack, float centerX, float centerY, float scale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, Z_ICON);
        GL11.glScalef(scale, scale, 0.00001F);
        GL11.glRotatef(180F, 0F, 0F, 1F);
        RenderHelper.enableStandardItemLighting();
        RenderItem itemRenderer = new RenderItem();
        itemRenderer.setRenderManager(RenderManager.instance);
        itemRenderer.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            -8,
            -8);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
    }

    private void renderFluid(FluidStack fluid, float centerX, float centerY, float scale) {
        if (fluid == null || fluid.getFluid() == null) {
            return;
        }
        IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) {
            return;
        }
        int color = fluid.getFluid()
            .getColor(fluid);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, Z_ICON);
        GL11.glScalef(scale, scale, 1F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-ICON_HALF_EXTENT, ICON_HALF_EXTENT, 0D, icon.getMinU(), icon.getMaxV());
        tessellator.addVertexWithUV(ICON_HALF_EXTENT, ICON_HALF_EXTENT, 0D, icon.getMaxU(), icon.getMaxV());
        tessellator.addVertexWithUV(ICON_HALF_EXTENT, -ICON_HALF_EXTENT, 0D, icon.getMaxU(), icon.getMinV());
        tessellator.addVertexWithUV(-ICON_HALF_EXTENT, -ICON_HALF_EXTENT, 0D, icon.getMinU(), icon.getMinV());
        tessellator.draw();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /**
     * Draws an aspect using Thaumcraft's own aspect icon, tinted with the
     * aspect colour and blended with the aspect's configured blend mode. Using
     * the upstream texture keeps compound aspects and addon aspects correct
     * without shipping any Thaumcraft assets.
     *
     * @param aspect  aspect to draw
     * @param centerX local x of the slot centre
     * @param centerY local y of the slot centre
     * @param scale   icon scale relative to one block
     */
    private void renderAspect(Aspect aspect, float centerX, float centerY, float scale) {
        ResourceLocation image = aspect.getImage();
        if (image == null) {
            return;
        }
        int color = aspect.getColor();
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, Z_ICON);
        GL11.glScalef(scale, scale, 1F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(image);
        GL11.glColor4f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-ICON_HALF_EXTENT, ICON_HALF_EXTENT, 0D, 0D, 1D);
        tessellator.addVertexWithUV(ICON_HALF_EXTENT, ICON_HALF_EXTENT, 0D, 1D, 1D);
        tessellator.addVertexWithUV(ICON_HALF_EXTENT, -ICON_HALF_EXTENT, 0D, 1D, 0D);
        tessellator.addVertexWithUV(-ICON_HALF_EXTENT, -ICON_HALF_EXTENT, 0D, 0D, 0D);
        tessellator.draw();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private void renderText(String text, float centerX, float centerY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = font.getStringWidth(text);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY + 0.16F, Z_TEXT);
        GL11.glScalef(0.02F, -0.02F, 0.02F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        font.drawStringWithShadow(text, -width / 2, 0, 0xFFFFFF);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private void renderIndicator(float centerX, float centerY, float fill, DrawerOptions options) {
        int mode = options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR);
        if (mode == 0) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY - 0.17F, Z_INDICATOR);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorOpaque_F(0.1F, 0.1F, 0.1F);
        addIndicatorQuad(tessellator, INDICATOR_HALF_WIDTH);
        tessellator.draw();
        if (mode == 1 || fill >= 1F) {
            tessellator.startDrawingQuads();
            tessellator.setColorOpaque_F(0.2F, 0.8F, 0.2F);
            addIndicatorQuad(tessellator, INDICATOR_HALF_WIDTH * Math.max(0.02F, fill));
            tessellator.draw();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private void addIndicatorQuad(Tessellator tessellator, float halfWidth) {
        tessellator.addVertex(-halfWidth, INDICATOR_HALF_HEIGHT, 0D);
        tessellator.addVertex(halfWidth, INDICATOR_HALF_HEIGHT, 0D);
        tessellator.addVertex(halfWidth, -INDICATOR_HALF_HEIGHT, 0D);
        tessellator.addVertex(-halfWidth, -INDICATOR_HALF_HEIGHT, 0D);
    }
}
