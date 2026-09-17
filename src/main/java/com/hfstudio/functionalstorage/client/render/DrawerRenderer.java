package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;

@SideOnly(Side.CLIENT)
public class DrawerRenderer extends TileEntitySpecialRenderer {

    private static final float Z_ICON = -0.035F;
    private static final float Z_TEXT = -0.02F;
    private static final float Z_INDICATOR = 0.004F;
    private static final float ICON_HALF_EXTENT = 0.5F;
    private static final float INDICATOR_HALF_WIDTH = 0.18F;
    private static final float INDICATOR_HALF_HEIGHT = 0.02F;
    private static final float TEXT_SCALE = 0.01F;
    private ItemStack voidBadge;

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
            ResourceLocation texture = multipleRenderPasses
                ? item.getSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture
                : textureManager.getResourceLocation(stack.getItemSpriteNumber());
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
                    renderItem(symbol, 0.3F + index++ * 0.1F, 0.12F, 0.08F, false);
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
                renderFluid(snapshot.getTemplate(), centerX, centerY, layout);
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
        return layout == DrawerFaceLayout.X_1 ? 0.65F : 0.25F;
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
        renderItem(stack, centerX, centerY, scale, FunctionalStorageConfig.CLIENT.threeDimensionalBlockDisplay);
    }

    private void renderUpgrades(ControllableDrawerTile drawer) {
        int count = drawer.getStorageUpgradeSlots() + drawer.getUtilityUpgradeSlots();
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = slot < drawer.getStorageUpgradeSlots() ? drawer.getStorageUpgrade(slot)
                : drawer.getUtilityUpgrade(slot - drawer.getStorageUpgradeSlots());
            renderItem(stack, 0.12F + slot * 0.1F, 0.91F, 0.085F, false);
        }
        if (drawer instanceof EnderDrawerTile && drawer.voidsOverflow()) {
            if (voidBadge == null) voidBadge = new ItemStack(RegistrationHandler.voidUpgrade);
            renderItem(voidBadge, 0.88F, 0.91F, 0.085F, false);
        }
    }

    private void renderItem(ItemStack stack, float x, float y, float scale, boolean threeDimensional) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        boolean raised = threeDimensional && stack.getItem() instanceof ItemBlock
            && (RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(stack.getItem())
                    .getRenderType())
                || MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED) != null);
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
                    .renderItem(minecraft.thePlayer, stack, 0, IItemRenderer.ItemRenderType.EQUIPPED);
            } else {
                GL11.glTranslatef(x, y, -0.02f);
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

    private void renderFluid(FluidStack fluid, float centerX, float centerY, DrawerFaceLayout layout) {
        if (fluid == null || fluid.getFluid() == null) {
            return;
        }
        IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) {
            return;
        }
        float scale = iconScale(layout);
        int color = fluid.getFluid()
            .getColor(fluid);
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, Z_ICON);
        GL11.glScalef(layout == DrawerFaceLayout.X_2 ? scale * 3 : scale, scale, 1F);
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
        GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(centerX, centerY + (iconScale > 0.25F ? 0.30F : 0.1F), Z_TEXT);
            GL11.glScalef(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            font.drawStringWithShadow(text, -width / 2, 0, 0xFFFFFF);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
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
        return tile instanceof ControllableDrawerTile controllableDrawerTile ? controllableDrawerTile.getItemHandler()
            : null;
    }
}
