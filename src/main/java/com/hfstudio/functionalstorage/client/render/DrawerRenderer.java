package com.hfstudio.functionalstorage.client.render;

import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.init.Blocks;
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
import com.hfstudio.functionalstorage.FunctionalStorage;
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
public class DrawerRenderer extends TileEntitySpecialRenderer implements IResourceManagerReloadListener {

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
    private static final float CAVITY_FRONT_INSET = 0.01F;
    private static final float CAVITY_BACK_INSET = 0.02F;
    private static final float CAVITY_LEFT = 1.5F;
    private static final float CAVITY_RIGHT = 14.5F;
    private static final float CAVITY_TOP = 14.5F;
    private static final float CAVITY_BOTTOM = 1.5F;
    private static final float DIVIDER_CENTER = 8F;
    private static final float DIVIDER_HALF = 1F;
    private static final float FLUID_MINIMUM_HEIGHT = 1F / 64F;
    private static final float FLUID_INTERPOLATION_RATE = 8F;
    private static final float MAX_FLUID_INTERPOLATION_SECONDS = 0.25F;
    private static final float INDICATOR_HALF_WIDTH = 0.18F;
    private static final float INDICATOR_HALF_HEIGHT = 0.02F;
    private static final float TEXT_SCALE = 0.01F;
    private static final float LOCK_HALF = 0.5F / 16F;
    private static final float LOCK_CENTER_Y = LOCK_HALF;
    private static final float Z_LOCK = 0.006F;
    private static final int MAX_CACHED_BLOCK_MODELS = 256;
    private static final ResourceLocation LOCK_TEXTURE = new ResourceLocation(
        FunctionalStorage.MOD_ID,
        "textures/blocks/lock.png");
    private static final ResourceLocation ITEM_GLINT_TEXTURE = new ResourceLocation(
        "textures/misc/enchanted_item_glint.png");
    private ItemStack voidBadge;

    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final RenderBlocks inventoryBlocks = new RenderBlocks();
    private final Map<ControllableDrawerTile, FluidRenderState> fluidRenderStates = new WeakHashMap<>();
    private final Map<BlockModelKey, Integer> blockModelLists = new LinkedHashMap<>(32, 0.75F, true);
    private final BigItemStack[] itemSnapshots = new BigItemStack[DrawerFaceLayout.X_4.getSlotCount()];
    private final SimpleIconBatch simpleIcons = new SimpleIconBatch();
    private final TextLabel[] textLabels = new TextLabel[DrawerFaceLayout.X_4.getSlotCount()];
    private int textLabelCount;
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
                    GL11.glDisable(GL11.GL_LIGHTING);
                    // Item icons are opaque; blending them would let the sprite's
                    // soft edges mix with what is behind and read as translucent.
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    renderIcon(x, y, icon, 16, 16);
                    if (renderEffect && stack.hasEffect(pass)) {
                        renderEffect(textureManager, x, y);
                    }
                }
            } finally {
                GL11.glPopAttrib();
            }
        }

        @Override
        public void renderEffect(TextureManager textureManager, int x, int y) {
            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_CURRENT_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT
                    | GL11.GL_TEXTURE_BIT);
            try {
                GL11.glDepthFunc(GL11.GL_EQUAL);
                GL11.glDepthMask(false);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
                GL11.glColor4f(0.5F, 0.25F, 0.8F, 1F);
                textureManager.bindTexture(ITEM_GLINT_TEXTURE);
                long time = Minecraft.getSystemTime();
                for (int pass = 0; pass < 2; pass++) {
                    int period = 3000 + pass * 1873;
                    float scroll = (time % period) / (float) period;
                    float shear = pass == 0 ? 4F : -1F;
                    float span = 16F / 256F;
                    // Match renderIcon's vertices exactly; a larger quad loses GL_EQUAL precision in perspective.
                    Tessellator tessellator = Tessellator.instance;
                    tessellator.startDrawingQuads();
                    tessellator.addVertexWithUV(x, y + 16, zLevel, scroll + span * shear, span);
                    tessellator.addVertexWithUV(x + 16, y + 16, zLevel, scroll + span * (1F + shear), span);
                    tessellator.addVertexWithUV(x + 16, y, zLevel, scroll + span, 0D);
                    tessellator.addVertexWithUV(x, y, zLevel, scroll, 0D);
                    tessellator.draw();
                }
            } finally {
                GL11.glPopAttrib();
            }
        }

        private void renderBlockIntoGUI(TextureManager textureManager, ItemStack stack, int x, int y) {
            Block block = Block.getBlockFromItem(stack.getItem());
            boolean previousTint = guiBlocks.useInventoryTint;
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_POLYGON_BIT);
            GL11.glPushMatrix();
            try {
                textureManager.bindTexture(TextureMap.locationBlocksTexture);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                if (block.getRenderBlockPass() != 0) {
                    GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                    GL11.glEnable(GL11.GL_BLEND);
                    OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                } else {
                    GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
                    GL11.glDisable(GL11.GL_BLEND);
                    // Flattened opaque models must not let their rear faces compete at the silhouette.
                    GL11.glEnable(GL11.GL_CULL_FACE);
                    GL11.glCullFace(GL11.GL_BACK);
                    GL11.glFrontFace(GL11.GL_CCW);
                }
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
                guiBlocks.useInventoryTint = renderWithColor;
                guiBlocks.renderBlockAsItem(block, stack.getItemDamage(), 1F);
            } finally {
                guiBlocks.useInventoryTint = previousTint;
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
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
        // Locking is drawer state rather than a display option, so a locked drawer
        // still shows its badge when the contents are hidden. Only a drawer with
        // nothing at all to draw, and which is not locked, is skipped.
        boolean contents = options.isShowItemRender() || options.isShowItemCount()
            || options.isShowUpgrades()
            || options.getAdvancedValue(ConfigurationToolItem.ConfigurationAction.INDICATOR) != 0;
        if (!contents && !drawer.isLocked()) {
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
            simpleIcons.clear();
            textLabelCount = 0;

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
            simpleIcons.flush();
            if (drawer instanceof EnderDrawerTile ender && ender.getFrequency() != null) {
                int index = 0;
                for (ItemStack symbol : DrawerTooltipData.frequencyDisplay(
                    ender.getFrequency()
                        .toString())) {
                    renderFlatStack(symbol, 0.3F + index++ * 0.1F, 0.12F, 0.08F);
                }
                simpleIcons.flush();
            }
            if (drawer.isLocked()) {
                renderLockBadge();
            }
            renderTextLabels();

        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        clearBlockModelLists();
    }

    private void renderItemSlots(@Nonnull IBigItemHandler handler, @Nonnull DrawerFaceLayout layout,
        @Nonnull DrawerOptions options) {
        int slotCount = Math.min(layout.getSlotCount(), handler.getStorageCount());
        for (int slot = 0; slot < slotCount; slot++) {
            BigItemStack snapshot = itemSnapshots[slot] = handler.getSnapshot(slot);
            if (!snapshot.hasTemplate()) {
                continue;
            }
            float centerX = layout.getSlotX(slot);
            float centerY = layout.getSlotY(slot);
            if (options.isShowItemRender()) {
                renderStack(snapshot.getTemplate(), centerX, centerY, iconScale(layout));
            }
        }
        simpleIcons.flush();
        for (int slot = 0; slot < slotCount; slot++) {
            BigItemStack snapshot = itemSnapshots[slot];
            if (!snapshot.hasTemplate()) {
                continue;
            }
            float centerX = layout.getSlotX(slot);
            float centerY = layout.getSlotY(slot);
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
        if (!simpleIcons.tryAdd(stack, centerX, centerY, scale)) {
            simpleIcons.flush();
            renderItem(stack, centerX, centerY, scale, FunctionalStorageConfig.CLIENT.threeDimensionalBlockDisplay);
        }
    }

    private void renderUpgrades(ControllableDrawerTile drawer) {
        int count = drawer.getStorageUpgradeSlots() + drawer.getUtilityUpgradeSlots();
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = slot < drawer.getStorageUpgradeSlots() ? drawer.getStorageUpgrade(slot)
                : drawer.getUtilityUpgrade(slot - drawer.getStorageUpgradeSlots());
            renderFlatStack(stack, 0.12F + slot * 0.1F, 0.91F, 0.085F);
        }
        if (drawer instanceof EnderDrawerTile && drawer.voidsOverflow()) {
            if (voidBadge == null) voidBadge = new ItemStack(RegistrationHandler.voidUpgrade);
            renderFlatStack(voidBadge, 0.88F, 0.91F, 0.085F);
        }
    }

    private void renderFlatStack(ItemStack stack, float centerX, float centerY, float scale) {
        if (!simpleIcons.tryAdd(stack, centerX, centerY, scale)) {
            simpleIcons.flush();
            renderItem(stack, centerX, centerY, scale, false);
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
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            Minecraft minecraft = Minecraft.getMinecraft();
            if (raised) {
                // Seat the full model in the face instead of floating it ahead of the label.
                GL11.glTranslatef(x, y, 0.002F - scale / 3f);
                GL11.glScalef(scale / 1.4f, -scale / 1.4f, -scale / 1.4f);
                RenderHelper.enableStandardItemLighting();
                if (!renderCachedBlock(stack, minecraft)) {
                    minecraft.entityRenderer.itemRenderer
                        .renderItem(minecraft.thePlayer, stack, 0, IItemRenderer.ItemRenderType.EQUIPPED);
                }
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

    private boolean renderCachedBlock(ItemStack stack, Minecraft minecraft) {
        if (!isCacheableBlock(stack)) {
            return false;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        int damage = stack.getItemDamage();
        BlockModelKey key = new BlockModelKey(block, damage);
        Integer list = blockModelLists.get(key);
        if (list == null) {
            if (blockModelLists.size() >= MAX_CACHED_BLOCK_MODELS) {
                BlockModelKey oldest = blockModelLists.keySet()
                    .iterator()
                    .next();
                GLAllocation.deleteDisplayLists(blockModelLists.remove(oldest));
            }
            list = compileBlockModel(block, damage);
            blockModelLists.put(key, list);
        }
        TextureManager textureManager = minecraft.getTextureManager();
        textureManager.bindTexture(TextureMap.locationBlocksTexture);
        boolean translucent = block.getRenderBlockPass() != 0;
        if (translucent) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glDepthMask(false);
        }
        GL11.glCallList(list);
        if (translucent) {
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        return true;
    }

    private boolean isCacheableBlock(ItemStack stack) {
        if (stack.hasTagCompound() || !(stack.getItem() instanceof ItemBlock) || stack.getItemSpriteNumber() != 0) {
            return false;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return RenderBlocks.renderItemIn3d(block.getRenderType())
            && MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED) == null;
    }

    private int compileBlockModel(Block block, int damage) {
        int list = GLAllocation.generateDisplayLists(1);
        boolean compiled = false;
        GL11.glNewList(list, GL11.GL_COMPILE);
        try {
            if (block.getRenderType() == 0) {
                compileCube(block, damage);
            } else {
                inventoryBlocks.renderBlockAsItem(block, damage, 1F);
            }
            compiled = true;
        } finally {
            GL11.glEndList();
            if (!compiled) {
                GLAllocation.deleteDisplayLists(list);
            }
        }
        return list;
    }

    private void compileCube(Block block, int damage) {
        if (block == Blocks.dispenser || block == Blocks.dropper || block == Blocks.furnace) {
            damage = 3;
        }
        block.setBlockBoundsForItemRender();
        inventoryBlocks.setRenderBoundsFromBlock(block);
        GL11.glRotatef(90F, 0F, 1F, 0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        int tint = block.getRenderColor(damage);
        // Per-vertex normals and tint keep all six faces in a single VAO draw.
        for (int side = 0; side < 6; side++) {
            tessellator.setColorOpaque_I(block == Blocks.grass && side != 1 ? 0xFFFFFF : tint);
            IIcon icon = inventoryBlocks.getBlockIconFromSideAndMetadata(block, side, damage);
            switch (side) {
                case 0 -> {
                    tessellator.setNormal(0F, -1F, 0F);
                    inventoryBlocks.renderFaceYNeg(block, 0D, 0D, 0D, icon);
                }
                case 1 -> {
                    tessellator.setNormal(0F, 1F, 0F);
                    inventoryBlocks.renderFaceYPos(block, 0D, 0D, 0D, icon);
                }
                case 2 -> {
                    tessellator.setNormal(0F, 0F, -1F);
                    inventoryBlocks.renderFaceZNeg(block, 0D, 0D, 0D, icon);
                }
                case 3 -> {
                    tessellator.setNormal(0F, 0F, 1F);
                    inventoryBlocks.renderFaceZPos(block, 0D, 0D, 0D, icon);
                }
                case 4 -> {
                    tessellator.setNormal(-1F, 0F, 0F);
                    inventoryBlocks.renderFaceXNeg(block, 0D, 0D, 0D, icon);
                }
                case 5 -> {
                    tessellator.setNormal(1F, 0F, 0F);
                    inventoryBlocks.renderFaceXPos(block, 0D, 0D, 0D, icon);
                }
            }
        }
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private void clearBlockModelLists() {
        for (Integer list : blockModelLists.values()) {
            GLAllocation.deleteDisplayLists(list);
        }
        blockModelLists.clear();
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

    private class SimpleIconBatch {

        private FlatIcon[] entries = new FlatIcon[16];
        private int size;

        private void clear() {
            size = 0;
        }

        private boolean tryAdd(ItemStack stack, float centerX, float centerY, float scale) {
            if (stack == null || stack.getItem() == null) {
                return true;
            }
            if (stack.getItem()
                .getRenderPasses(stack.getItemDamage()) != 1) {
                return false;
            }
            Item item = stack.getItem();
            if (item.requiresMultipleRenderPasses()
                || MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.INVENTORY) != null) {
                return false;
            }
            int spriteNumber = stack.getItemSpriteNumber();
            if (spriteNumber == 0 && RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(item)
                    .getRenderType())) {
                return false;
            }
            IIcon icon = stack.getIconIndex();
            if (icon == null) {
                return false;
            }
            if (size == entries.length) {
                FlatIcon[] expanded = new FlatIcon[entries.length * 2];
                System.arraycopy(entries, 0, expanded, 0, entries.length);
                entries = expanded;
            }
            FlatIcon entry = entries[size];
            if (entry == null) {
                entry = entries[size] = new FlatIcon();
            }
            entry.texture = Minecraft.getMinecraft()
                .getTextureManager()
                .getResourceLocation(spriteNumber);
            entry.icon = icon;
            entry.color = item.getColorFromItemStack(stack, 0);
            entry.centerX = centerX;
            entry.centerY = centerY;
            entry.scale = scale;
            entry.glint = stack.hasEffect(0);
            size++;
            return true;
        }

        private void flush() {
            if (size == 0) {
                return;
            }
            TextureManager textureManager = Minecraft.getMinecraft()
                .getTextureManager();
            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            try {
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(true);
                GL11.glColor4f(1F, 1F, 1F, 1F);
                for (int index = 0; index < size; index++) {
                    if (entries[index].texture == null) {
                        continue;
                    }
                    ResourceLocation texture = entries[index].texture;
                    textureManager.bindTexture(texture);
                    Tessellator tessellator = Tessellator.instance;
                    tessellator.startDrawingQuads();
                    for (int entryIndex = index; entryIndex < size; entryIndex++) {
                        FlatIcon entry = entries[entryIndex];
                        if (!texture.equals(entry.texture)) {
                            continue;
                        }
                        addIcon(tessellator, entry);
                    }
                    tessellator.draw();
                    for (int next = index + 1; next < size; next++) {
                        if (texture.equals(entries[next].texture)) {
                            entries[next].texture = null;
                        }
                    }
                }
                renderGlint(textureManager);
            } finally {
                GL11.glPopAttrib();
                size = 0;
            }
        }

        private void renderGlint(TextureManager textureManager) {
            boolean hasGlint = false;
            for (int index = 0; index < size; index++) {
                hasGlint |= entries[index].glint;
            }
            if (!hasGlint) {
                return;
            }
            GL11.glDepthFunc(GL11.GL_EQUAL);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            textureManager.bindTexture(ITEM_GLINT_TEXTURE);
            long time = Minecraft.getSystemTime();
            for (int pass = 0; pass < 2; pass++) {
                int period = 3000 + pass * 1873;
                float scroll = (time % period) / (float) period;
                float shear = pass == 0 ? 4F : -1F;
                float span = 16F / 256F;
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.setColorRGBA_F(0.5F, 0.25F, 0.8F, 1F);
                for (int index = 0; index < size; index++) {
                    FlatIcon entry = entries[index];
                    if (!entry.glint) {
                        continue;
                    }
                    float half = entry.scale / 2F;
                    float left = entry.centerX - half;
                    float right = entry.centerX + half;
                    float top = entry.centerY - half;
                    float bottom = entry.centerY + half;
                    tessellator.addVertexWithUV(left, bottom, Z_ICON_2D, scroll + span * shear, span);
                    tessellator.addVertexWithUV(right, bottom, Z_ICON_2D, scroll + span * (1F + shear), span);
                    tessellator.addVertexWithUV(right, top, Z_ICON_2D, scroll + span, 0D);
                    tessellator.addVertexWithUV(left, top, Z_ICON_2D, scroll, 0D);
                }
                tessellator.draw();
            }
        }

        private void addIcon(Tessellator tessellator, FlatIcon entry) {
            tessellator.setColorOpaque_I(entry.color);
            float half = entry.scale / 2F;
            float left = entry.centerX - half;
            float right = entry.centerX + half;
            float top = entry.centerY - half;
            float bottom = entry.centerY + half;
            IIcon icon = entry.icon;
            tessellator.addVertexWithUV(left, bottom, Z_ICON_2D, icon.getMinU(), icon.getMaxV());
            tessellator.addVertexWithUV(right, bottom, Z_ICON_2D, icon.getMaxU(), icon.getMaxV());
            tessellator.addVertexWithUV(right, top, Z_ICON_2D, icon.getMaxU(), icon.getMinV());
            tessellator.addVertexWithUV(left, top, Z_ICON_2D, icon.getMinU(), icon.getMinV());
        }
    }

    private static class FlatIcon {

        private ResourceLocation texture;
        private IIcon icon;
        private int color;
        private float centerX;
        private float centerY;
        private float scale;
        private boolean glint;
    }

    private record BlockModelKey(Block block, int damage) {}

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
                    modelLeft = divider + dividerHalf;
                } else {
                    modelRight = divider - dividerHalf;
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
        float uLeft = icon.getMinU();
        float uRight = icon.getMaxU();
        float vTop = icon.getMinV();
        float vBottom = icon.getMaxV();
        float alpha = 1F;
        addFluidFace(tessellator, left, right, top, bottom, front, uLeft, uRight, vTop, vBottom, color, alpha);
        addFluidFace(tessellator, right, left, top, bottom, back, uLeft, uRight, vTop, vBottom, color, alpha * 0.8F);
        addFluidTop(tessellator, left, right, top, front, back, uLeft, uRight, vTop, vBottom, color, alpha);
        addFluidSide(tessellator, left, top, bottom, front, back, uLeft, uRight, vTop, vBottom, color, alpha * 0.7F);
        addFluidSide(tessellator, right, bottom, top, front, back, uLeft, uRight, vTop, vBottom, color, alpha * 0.7F);
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
            vBottom,
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

    private void renderLockBadge() {
        // Only text labels follow; the enclosing drawer render restores GL state.
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(LOCK_TEXTURE);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0.5F - LOCK_HALF, LOCK_CENTER_Y + LOCK_HALF, Z_LOCK, 0D, 1D);
        tessellator.addVertexWithUV(0.5F + LOCK_HALF, LOCK_CENTER_Y + LOCK_HALF, Z_LOCK, 1D, 1D);
        tessellator.addVertexWithUV(0.5F + LOCK_HALF, LOCK_CENTER_Y - LOCK_HALF, Z_LOCK, 1D, 0D);
        tessellator.addVertexWithUV(0.5F - LOCK_HALF, LOCK_CENTER_Y - LOCK_HALF, Z_LOCK, 0D, 0D);
        tessellator.draw();
    }

    private void renderText(String text, float centerX, float centerY, float iconScale) {
        TextLabel label = textLabels[textLabelCount];
        if (label == null) {
            label = textLabels[textLabelCount] = new TextLabel();
        }
        label.text = text;
        label.x = centerX;
        label.y = centerY + (iconScale > 0.25F ? 0.30F : 0.1F);
        textLabelCount++;
    }

    private void renderTextLabels() {
        if (textLabelCount == 0) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        for (int index = 0; index < textLabelCount; index++) {
            TextLabel label = textLabels[index];
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(label.x, label.y, Z_TEXT);
                GL11.glScalef(TEXT_SCALE, TEXT_SCALE, 1F);
                font.drawStringWithShadow(label.text, -font.getStringWidth(label.text) / 2, 0, 0xFFFFFF);
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    private static class TextLabel {

        private String text;
        private float x;
        private float y;
    }

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
