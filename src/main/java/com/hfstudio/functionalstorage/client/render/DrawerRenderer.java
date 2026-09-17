package com.hfstudio.functionalstorage.client.render;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData;
import com.hfstudio.functionalstorage.common.block.DrawerAttachment;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem;
import com.hfstudio.functionalstorage.common.options.DrawerOptions;
import com.hfstudio.functionalstorage.common.tile.EnderDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;
import com.hfstudio.functionalstorage.util.HitBoxesUtil;

import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.Aspect;

/** Renders contents in the same face coordinates used by placement and interaction. */
public class DrawerRenderer extends TileEntitySpecialRenderer {

    private static final float Z_ICON = 0.002F;
    private static final float Z_TEXT = 0.003F;
    private static final float Z_INDICATOR = 0.004F;
    private static final float ICON_HALF_EXTENT = 0.5F;
    private static final float INDICATOR_HALF_WIDTH = 0.18F;
    private static final float INDICATOR_HALF_HEIGHT = 0.02F;
    private static final float TEXT_SCALE = 0.007F;

    private final DrawerContentRenderer contentRenderer = new DrawerContentRenderer();
    private ItemStack voidBadge;

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof ControllableDrawerTile drawer) || !(tile.getBlockType() instanceof DrawerBlock block)) {
            return;
        }
        if (!isWithinRenderRange(tile)) {
            return;
        }

        DrawerOptions options = drawer.getDrawerOptions();
        if (!options.isShowItemRender() && !options.isShowItemCount()
            && !options.isShowUpgrades()
            && options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) == 0) {
            return;
        }
        int metadata = tile.getBlockMetadata();
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_TEXTURE_BIT);
        applyBrightness(tile);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);
            applyFaceTransform(DrawerBlock.getAttachment(metadata), DrawerBlock.getHorizontalFacing(metadata));

            DrawerFaceLayout layout = block.getFaceLayout();
            IBigItemHandler itemHandler = drawer.getItemHandler();
            IBigFluidHandler fluidHandler = drawer.getFluidHandler();
            IBigAspectHandler aspectHandler = drawer.getAspectHandler();
            if (itemHandler != null) {
                renderItemSlots(itemHandler, layout, options);
            } else if (fluidHandler != null) {
                renderFluidSlots(fluidHandler, layout, options);
            } else if (aspectHandler != null) {
                renderAspectSlots(aspectHandler, layout, options);
            }
            if (options.isShowUpgrades()) {
                renderUpgrades(drawer);
            }
            if (drawer instanceof EnderDrawerTile ender && ender.getFrequency() != null) {
                int index = 0;
                for (ItemStack symbol : DrawerTooltipData.frequencyDisplay(
                    ender.getFrequency()
                        .toString())) {
                    contentRenderer.render(symbol, 0.3F + index++ * 0.1F, 0.12F, 0.08F, false);
                }
            }

        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
        }
    }

    private void renderItemSlots(@Nonnull IBigItemHandler handler, @Nonnull DrawerFaceLayout layout,
        @Nonnull DrawerOptions options) {
        for (int slot = 0; slot < Math.min(layout.getSlotCount(), handler.getStorageCount()); slot++) {
            BigItemStack snapshot = handler.getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            float centerX = layout.getSlotX(slot);
            float centerY = layout.getSlotY(slot);
            if (options.isShowItemRender()) {
                renderStack(snapshot.getTemplate(), centerX, centerY, iconScale(layout));
            }
            if (options.isShowItemCount()) {
                renderText(
                    NumberFormatUtil.formatNumberCompact(snapshot.getAmount()),
                    centerX,
                    centerY,
                    iconScale(layout));
            }
            if (options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) != 0) {
                renderIndicator(
                    centerX,
                    centerY,
                    iconScale(layout),
                    ratio(snapshot.getAmount(), handler.getCapacity(slot)),
                    options);
            }
        }
    }

    private void renderFluidSlots(@Nonnull IBigFluidHandler handler, @Nonnull DrawerFaceLayout layout,
        @Nonnull DrawerOptions options) {
        for (int slot = 0; slot < Math.min(layout.getSlotCount(), handler.getStorageCount()); slot++) {
            BigFluidStack snapshot = handler.getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            float centerX = layout.getSlotX(slot);
            float centerY = layout.getSlotY(slot);
            if (options.isShowItemRender()) {
                renderFluid(snapshot.getTemplate(), centerX, centerY, iconScale(layout));
            }
            if (options.isShowItemCount()) {
                renderText(NumberFormatUtil.formatFluid(snapshot.getAmount()), centerX, centerY, iconScale(layout));
            }
            if (options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) != 0) {
                renderIndicator(
                    centerX,
                    centerY,
                    iconScale(layout),
                    ratio(snapshot.getAmount(), handler.getCapacity(slot)),
                    options);
            }
        }
    }

    @Optional.Method(modid = "Thaumcraft")
    private void renderAspectSlots(@Nonnull IBigAspectHandler handler, @Nonnull DrawerFaceLayout layout,
        @Nonnull DrawerOptions options) {
        for (int slot = 0; slot < Math.min(layout.getSlotCount(), handler.getStorageCount()); slot++) {
            BigAspectStack snapshot = handler.getSnapshot(slot);
            Aspect aspect = snapshot.getAspect();
            if (aspect == null) {
                continue;
            }
            float centerX = layout.getSlotX(slot);
            float centerY = layout.getSlotY(slot);
            if (options.isShowItemRender()) {
                renderAspect(aspect, centerX, centerY, iconScale(layout));
            }
            if (options.isShowItemCount()) {
                renderText(
                    NumberFormatUtil.formatNumberCompact(snapshot.getAmount()),
                    centerX,
                    centerY,
                    iconScale(layout));
            }
            if (options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) != 0) {
                renderIndicator(
                    centerX,
                    centerY,
                    iconScale(layout),
                    ratio(snapshot.getAmount(), handler.getCapacity(slot)),
                    options);
            }
        }
    }

    private void applyBrightness(TileEntity tile) {
        ForgeDirection front = DrawerBlock.getFrontFacing(tile.getBlockMetadata());
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(
                tile.xCoord + front.offsetX,
                tile.yCoord + front.offsetY,
                tile.zCoord + front.offsetZ,
                0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536f);
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

    private float iconScale(DrawerFaceLayout layout) {
        return layout == DrawerFaceLayout.X_1 ? 0.5F : 0.25F;
    }

    private float ratio(long amount, long capacity) {
        return capacity <= 0L ? 0F : (float) Math.min(1D, amount / (double) capacity);
    }

    private void applyFaceTransform(DrawerAttachment attachment, ForgeDirection facing) {
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(-90F * HitBoxesUtil.horizontalIndex(facing), 0F, 1F, 0F);
        if (attachment != DrawerAttachment.WALL) {
            GL11.glRotatef(attachment == DrawerAttachment.FLOOR ? 90F : -90F, 1F, 0F, 0F);
        }
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        GL11.glTranslatef(1F, 1F, 0F);
        GL11.glScalef(-1F, -1F, -1F);
    }

    private void renderStack(ItemStack stack, float centerX, float centerY, float scale) {
        contentRenderer
            .render(stack, centerX, centerY, scale, FunctionalStorageConfig.CLIENT.threeDimensionalBlockDisplay);
    }

    private void renderUpgrades(ControllableDrawerTile drawer) {
        int count = drawer.getStorageUpgradeSlots() + drawer.getUtilityUpgradeSlots();
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = slot < drawer.getStorageUpgradeSlots() ? drawer.getStorageUpgrade(slot)
                : drawer.getUtilityUpgrade(slot - drawer.getStorageUpgradeSlots());
            contentRenderer.render(stack, 0.12F + slot * 0.1F, 0.91F, 0.085F, false);
        }
        if (drawer instanceof EnderDrawerTile && drawer.voidsOverflow()) {
            if (voidBadge == null) voidBadge = new ItemStack(RegistrationHandler.voidUpgrade);
            contentRenderer.render(voidBadge, 0.88F, 0.91F, 0.085F, false);
        }
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
     * aspect colour. Using the upstream texture keeps compound aspects and
     * addon aspects correct without shipping any Thaumcraft assets.
     *
     * @param aspect  aspect to draw
     * @param centerX local x of the slot centre
     * @param centerY local y of the slot centre
     * @param scale   icon scale relative to one block
     */
    @Optional.Method(modid = "Thaumcraft")
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

    private void renderText(String text, float centerX, float centerY, float iconScale) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = font.getStringWidth(text);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY + (iconScale > 0.25F ? 0.34F : 0.16F), Z_TEXT);
        GL11.glScalef(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        GL11.glDisable(GL11.GL_LIGHTING);
        font.drawStringWithShadow(text, -width / 2, 0, 0xFFFFFF);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private void renderIndicator(float centerX, float centerY, float iconScale, float fill, DrawerOptions options) {
        int mode = options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR);
        if (mode == 0) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY + (iconScale > 0.25F ? 0.425F : 0.22F), Z_INDICATOR);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator tessellator = Tessellator.instance;
        if (mode != 3) {
            tessellator.startDrawingQuads();
            tessellator.setColorOpaque_F(0.1F, 0.1F, 0.1F);
            addIndicatorQuad(tessellator, INDICATOR_HALF_WIDTH);
            tessellator.draw();
        }
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

    @Nullable
    public static IBigItemHandler itemHandlerOf(@Nullable TileEntity tile) {
        return tile instanceof ControllableDrawerTile ? ((ControllableDrawerTile) tile).getItemHandler() : null;
    }
}
