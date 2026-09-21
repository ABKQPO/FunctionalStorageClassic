package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.WeakHashMap;

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

    /**
     * Face-plane offsets along the block's depth axis. The face transform negates
     * the emitted z, so a larger emitted value ends up closer to the viewer:
     * the count is drawn in front of the flat icon plane rather than sharing it.
     */
    private static final float Z_ICON = -0.035F;
    private static final float Z_ICON_2D = -0.02F;
    private static final float Z_TEXT = -0.01F;
    private static final float Z_INDICATOR = 0.004F;
    private static final float ICON_HALF_EXTENT = 0.5F;
    /**
     * Fluid cavity bounds in model units, matching the {@code fluid_inner} panels
     * of the fluid drawer models. Every fluid model shares these walls; the wider
     * layouts only add dividers inside them.
     */
    private static final float CAVITY_NEAR = 1F;
    private static final float CAVITY_FAR = 14.5F;
    /**
     * Insets kept between the fluid volume and the cavity walls it would
     * otherwise share a depth plane with. The front frame and the rear wall both
     * sit exactly on the cavity bounds, so a volume drawn right up to them
     * z-fights; a hair of clearance removes the flicker and is imperceptible
     * because the fluid fills the opening.
     */
    private static final float CAVITY_FRONT_INSET = 0.01F;
    private static final float CAVITY_BACK_INSET = 0.02F;
    private static final float CAVITY_LEFT = 1.5F;
    private static final float CAVITY_RIGHT = 14.5F;
    private static final float CAVITY_TOP = 14.5F;
    private static final float CAVITY_BOTTOM = 1.5F;
    /** Centre of the divider that splits the cavity in two, in model units. */
    private static final float DIVIDER_CENTER = 8F;
    /** Half thickness of a divider, so slot bounds stop at its surface. */
    private static final float DIVIDER_HALF = 1F;
    private static final float FLUID_MINIMUM_HEIGHT = 1F / 64F;
    private static final float FLUID_INTERPOLATION_RATE = 8F;
    private static final float MAX_FLUID_INTERPOLATION_SECONDS = 0.25F;
    private static final float INDICATOR_HALF_WIDTH = 0.18F;
    private static final float INDICATOR_HALF_HEIGHT = 0.02F;
    private static final float TEXT_SCALE = 0.01F;
    private ItemStack voidBadge;

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final RenderBlocks inventoryBlocks = new RenderBlocks();
    private final Map<ControllableDrawerTile, FluidRenderState> fluidRenderStates = new WeakHashMap<>();
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
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            try {
                for (int pass = 0; pass < renderPasses; pass++) {
                    textureManager.bindTexture(texture);
                    IIcon icon = multipleRenderPasses ? item.getIcon(stack, pass) : stack.getIconIndex();
                    if (icon == null) {
                        continue;
                    }
                    int color = item.getColorFromItemStack(stack, pass);
                    if (renderWithColor) {
                        GL11.glColor4f(
                            (color >> 16 & 0xFF) / 255F,
                            (color >> 8 & 0xFF) / 255F,
                            (color & 0xFF) / 255F,
                            1F);
                    }
                    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                    GL11.glPolygonOffset(-1F, -1F);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    // Item icons are opaque; blending them would let the sprite's
                    // soft edges mix with what is behind and read as translucent.
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    renderIcon(x, y, icon, 16, 16);
                    if (renderEffect && stack.hasEffect(pass)) {
                        GL11.glDisable(GL11.GL_ALPHA_TEST);
                        renderEffect(textureManager, x, y);
                    }
                    GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                }
            } finally {
                GL11.glPopAttrib();
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
                renderFluidSlots(drawer, fluidHandler, layout, options);
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

    private void renderFluidSlots(@Nonnull ControllableDrawerTile drawer, @Nonnull IBigFluidHandler handler,
        @Nonnull DrawerFaceLayout layout, @Nonnull DrawerOptions options) {
        int slotCount = Math.min(layout.getSlotCount(), handler.getStorageCount());
        FluidRenderState state = fluidRenderStates.computeIfAbsent(drawer, ignored -> new FluidRenderState());
        state.beginFrame(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            BigFluidStack snapshot = handler.getSnapshot(slot);
            boolean hasTemplate = snapshot.hasTemplate();
            if (hasTemplate) {
                state.setTemplate(slot, snapshot.getTemplate());
            }
            float fill = state
                .interpolate(slot, hasTemplate ? ratio(snapshot.getAmount(), handler.getCapacity(slot)) : 0F);
            FluidStack fluid = state.getTemplate(slot);
            if (fluid == null) continue;
            if (!hasTemplate && fill == 0F) {
                state.clearTemplate(slot);
                continue;
            }
            float[] bounds = fluidBounds(layout, slot);
            if (options.isShowItemRender()) {
                renderFluidVolume(fluid, bounds, fill);
            }
            float centerX = (bounds[0] + bounds[1]) / 2F;
            float centerY = (bounds[2] + bounds[3]) / 2F;
            if (hasTemplate && options.isShowItemCount()) {
                renderText(NumberFormatUtil.formatFluid(snapshot.getAmount()), centerX, centerY, iconScale(layout));
            }
            if (hasTemplate && options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) != 0) {
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
                GL11.glTranslatef(x, y, Z_ICON_2D);
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

    /**
     * Resolves the inner cavity region one fluid slot occupies, as
     * {@code [left, right, top, bottom]} in the space the fluid quads are
     * emitted in.
     *
     * <p>
     * The volume must sit strictly inside the model's cavity, so the bounds come
     * from the cavity walls rather than from the interactive face regions. Wider
     * layouts are split at the divider the models place across the cavity, which
     * keeps every slot clear of both the divider and the surrounding walls.
     *
     * <p>
     * Cavity walls are authored in model units; the face transform mirrors both
     * horizontal axes, so each bound is converted with {@code 1 - value} and the
     * vertical pair swaps.
     *
     * @param layout model layout
     * @param slot   slot index
     * @return slot bounds in the emitted coordinate space
     */
    private static float[] fluidBounds(DrawerFaceLayout layout, int slot) {
        float modelLeft = CAVITY_LEFT / 16F;
        float modelRight = CAVITY_RIGHT / 16F;
        float modelTop = CAVITY_BOTTOM / 16F;
        float modelBottom = CAVITY_TOP / 16F;
        if (layout != DrawerFaceLayout.X_1) {
            float divider = DIVIDER_CENTER / 16F;
            float dividerHalf = DIVIDER_HALF / 16F;
            // Face regions number slots from the top down, while the cavity uses
            // upward model units. Only the divider side is pulled back; the outer
            // edge stays against the cavity wall. With four slots the index runs
            // left to right, then top to bottom, so the row is the index above
            // the column bit rather than the low bit itself.
            boolean upperRow = layout == DrawerFaceLayout.X_4 ? slot < 2 : slot % 2 == 0;
            if (upperRow) {
                modelTop = divider + dividerHalf;
                modelBottom = CAVITY_TOP / 16F;
            } else {
                modelTop = CAVITY_BOTTOM / 16F;
                modelBottom = divider - dividerHalf;
            }
            if (layout == DrawerFaceLayout.X_4) {
                if (slot % 2 == 0) {
                    modelRight = divider - dividerHalf;
                } else {
                    modelLeft = divider + dividerHalf;
                }
            }
        }
        return new float[] { 1F - modelRight, 1F - modelLeft, 1F - modelBottom, 1F - modelTop };
    }

    private static class FluidRenderState {

        private long lastFrameNanos;
        private float interpolationFactor = 1F;
        private float[] fills = new float[0];
        private boolean[] initialized = new boolean[0];
        private FluidStack[] templates = new FluidStack[0];

        private void beginFrame(int slotCount) {
            if (fills.length != slotCount) {
                fills = new float[slotCount];
                initialized = new boolean[slotCount];
                templates = new FluidStack[slotCount];
                lastFrameNanos = 0L;
            }
            long now = System.nanoTime();
            if (lastFrameNanos == 0L) {
                interpolationFactor = 1F;
            } else {
                float elapsed = Math.min(MAX_FLUID_INTERPOLATION_SECONDS, (now - lastFrameNanos) / 1_000_000_000F);
                interpolationFactor = 1F - (float) Math.exp(-FLUID_INTERPOLATION_RATE * elapsed);
            }
            lastFrameNanos = now;
        }

        private float interpolate(int slot, float target) {
            if (!initialized[slot]) {
                fills[slot] = target;
                initialized[slot] = true;
            } else {
                fills[slot] += (target - fills[slot]) * interpolationFactor;
                if (Math.abs(target - fills[slot]) < 0.0001F) {
                    fills[slot] = target;
                }
            }
            return fills[slot];
        }

        private void setTemplate(int slot, FluidStack template) {
            templates[slot] = template;
        }

        private FluidStack getTemplate(int slot) {
            return templates[slot];
        }

        private void clearTemplate(int slot) {
            templates[slot] = null;
        }
    }

    /**
     * Renders one slot's fluid volume inside the model cavity.
     *
     * @param fluid  fluid to render
     * @param bounds slot cavity bounds as {@code [left, right, top, bottom]} in
     *               normalised model space
     * @param fill   fill ratio in {@code [0, 1]}
     */
    private void renderFluidVolume(FluidStack fluid, float[] bounds, float fill) {
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
        float height = bounds[3] - bounds[2];
        float left = bounds[0];
        float right = bounds[1];
        float bottom = bounds[3];
        float top = bottom - Math.max(FLUID_MINIMUM_HEIGHT, height * fill);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            addFluidVolume(
                tessellator,
                icon,
                left,
                right,
                top,
                bottom,
                -(CAVITY_NEAR + CAVITY_FRONT_INSET) / 16F,
                -(CAVITY_FAR - CAVITY_BACK_INSET) / 16F,
                color);
            tessellator.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void addFluidVolume(Tessellator tessellator, IIcon icon, float left, float right, float top, float bottom,
        float front, float back, int color) {
        float uLeft = icon.getInterpolatedU(0D);
        float uRight = icon.getInterpolatedU((right - left) * 16F);
        float uSideLeft = icon.getInterpolatedU(0D);
        float depth = Math.abs(back - front);
        float uSideRight = icon.getInterpolatedU(depth * 16F);
        float vTop = icon.getInterpolatedV(0D);
        float vHeightBottom = icon.getInterpolatedV((bottom - top) * 16F);
        float vDepthBottom = icon.getInterpolatedV(depth * 16F);
        float alpha = 1F;
        addFluidFace(tessellator, left, right, top, bottom, front, uLeft, uRight, vTop, vHeightBottom, color, alpha);
        addFluidFace(
            tessellator,
            right,
            left,
            top,
            bottom,
            back,
            uLeft,
            uRight,
            vTop,
            vHeightBottom,
            color,
            alpha * 0.8F);
        addFluidTop(tessellator, left, right, top, front, back, uLeft, uRight, vTop, vDepthBottom, color, alpha);
        addFluidSide(
            tessellator,
            left,
            top,
            bottom,
            front,
            back,
            uSideLeft,
            uSideRight,
            vTop,
            vHeightBottom,
            color,
            alpha * 0.7F);
        addFluidSide(
            tessellator,
            right,
            bottom,
            top,
            front,
            back,
            uSideLeft,
            uSideRight,
            vTop,
            vHeightBottom,
            color,
            alpha * 0.7F);
        addFluidBottom(
            tessellator,
            left,
            right,
            bottom,
            front,
            back,
            uLeft,
            uRight,
            vTop,
            vDepthBottom,
            color,
            alpha * 0.6F);
    }

    private void addFluidFace(Tessellator tessellator, float left, float right, float top, float bottom, float depth,
        float uLeft, float uRight, float vTop, float vBottom, int color, float alpha) {
        tintFluid(tessellator, color, alpha);
        tessellator.addVertexWithUV(left, bottom, depth, uLeft, vBottom);
        tessellator.addVertexWithUV(right, bottom, depth, uRight, vBottom);
        tessellator.addVertexWithUV(right, top, depth, uRight, vTop);
        tessellator.addVertexWithUV(left, top, depth, uLeft, vTop);
    }

    private void addFluidTop(Tessellator tessellator, float left, float right, float top, float front, float back,
        float uLeft, float uRight, float vNear, float vFar, int color, float alpha) {
        tintFluid(tessellator, color, alpha);
        tessellator.addVertexWithUV(left, top, back, uLeft, vFar);
        tessellator.addVertexWithUV(right, top, back, uRight, vFar);
        tessellator.addVertexWithUV(right, top, front, uRight, vNear);
        tessellator.addVertexWithUV(left, top, front, uLeft, vNear);
    }

    private void addFluidSide(Tessellator tessellator, float side, float top, float bottom, float front, float back,
        float uLeft, float uRight, float vTop, float vBottom, int color, float alpha) {
        tintFluid(tessellator, color, alpha);
        tessellator.addVertexWithUV(side, bottom, back, uLeft, vBottom);
        tessellator.addVertexWithUV(side, bottom, front, uRight, vBottom);
        tessellator.addVertexWithUV(side, top, front, uRight, vTop);
        tessellator.addVertexWithUV(side, top, back, uLeft, vTop);
    }

    private void addFluidBottom(Tessellator tessellator, float left, float right, float bottom, float front, float back,
        float uLeft, float uRight, float vNear, float vFar, int color, float alpha) {
        tintFluid(tessellator, color, alpha);
        tessellator.addVertexWithUV(left, bottom, front, uLeft, vNear);
        tessellator.addVertexWithUV(right, bottom, front, uRight, vNear);
        tessellator.addVertexWithUV(right, bottom, back, uRight, vFar);
        tessellator.addVertexWithUV(left, bottom, back, uLeft, vFar);
    }

    private void tintFluid(Tessellator tessellator, int color, float alpha) {
        tessellator
            .setColorRGBA_F((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, alpha);
    }

    /**
     * Draws an essentia icon on a slot. State is pushed rather than toggled and
     * restored by hand so it survives an exception while the quad is drawn.
     *
     * @param aspect  aspect to draw
     * @param centerX slot centre on the face
     * @param centerY slot centre on the face
     * @param scale   icon scale
     */
    @Optional.Method(modid = "Thaumcraft")
    private void renderAspect(Aspect aspect, float centerX, float centerY, float scale) {
        ResourceLocation image = aspect.getImage();
        if (image == null) {
            return;
        }
        int color = aspect.getColor();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
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
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Draws a slot's count in front of the slot's icon.
     *
     * <p>
     * Depth testing stays enabled so the label is occluded by the world exactly
     * like the block it belongs to; disabling it would draw the count through
     * every block in front of the drawer. Only depth writes are masked, which
     * keeps the glyph quads from stacking against each other, and the scale is
     * applied to the text extents alone so the chosen depth survives the
     * transform.
     *
     * @param text      label to draw
     * @param centerX   slot centre on the face
     * @param centerY   slot centre on the face
     * @param iconScale icon scale, used to place the label below the icon
     */
    private void renderText(String text, float centerX, float centerY, float iconScale) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int width = font.getStringWidth(text);
        GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_ENABLE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(centerX, centerY + (iconScale > 0.25F ? 0.30F : 0.1F), Z_TEXT);
            GL11.glScalef(TEXT_SCALE, TEXT_SCALE, 1F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            font.drawStringWithShadow(text, -width / 2, 0, 0xFFFFFF);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    /**
     * Draws the fill indicator bar under a slot's icon.
     *
     * <p>
     * Lighting and texturing are pushed rather than toggled and restored by hand,
     * so the state survives an exception partway through drawing the bar.
     *
     * @param centerX   slot centre on the face
     * @param centerY   slot centre on the face
     * @param iconScale icon scale, used to place the bar below the icon
     * @param fill      fill ratio in {@code [0, 1]}
     * @param options   drawer display options
     */
    private void renderIndicator(float centerX, float centerY, float iconScale, float fill, DrawerOptions options) {
        int mode = options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR);
        if (mode == 0) {
            return;
        }
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glPushMatrix();
        try {
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
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
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
