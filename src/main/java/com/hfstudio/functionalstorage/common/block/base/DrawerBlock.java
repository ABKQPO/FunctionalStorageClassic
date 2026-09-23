package com.hfstudio.functionalstorage.common.block.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.hfstudio.functionalstorage.client.model.DrawerModelProvider;
import com.hfstudio.functionalstorage.common.block.DrawerAttachment;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.integration.serverutilities.ServerUtilitiesIntegration;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;
import com.hfstudio.functionalstorage.util.HitBoxesUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Metadata encodes attachment and horizontal rotation for all twelve orientations. */
public abstract class DrawerBlock extends BlockContainer implements IBlockModelProvider {

    public static final float HARDNESS = 2.5F;
    public static final float RESISTANCE = 8.0F;
    public static final String[] EFFECTIVE_TOOLS = { "axe", "pickaxe" };

    private final DrawerFaceLayout faceLayout;

    protected DrawerBlock(@Nonnull DrawerFaceLayout faceLayout, @Nonnull String textureBase) {
        super(Material.wood);
        this.faceLayout = faceLayout;
        setHardness(HARDNESS);
        setResistance(RESISTANCE);
        setStepSound(Block.soundTypeWood);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setBlockName(textureBase);
        setBlockTextureName(textureBase);
        setLightOpacity(255);
        useNeighborBrightness = true;
    }

    @Override
    public boolean isToolEffective(String type, int metadata) {
        for (String effective : EFFECTIVE_TOOLS) {
            if (effective.equals(type)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public DrawerFaceLayout getFaceLayout() {
        return faceLayout;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, int x, int y, int z, int metadata, EffectRenderer effectRenderer) {
        IIcon icon = blockIcon;
        for (int particleX = 0; particleX < 4; particleX++) {
            for (int particleY = 0; particleY < 4; particleY++) {
                for (int particleZ = 0; particleZ < 4; particleZ++) {
                    double offsetX = (particleX + 0.5D) / 4D;
                    double offsetY = (particleY + 0.5D) / 4D;
                    double offsetZ = (particleZ + 0.5D) / 4D;
                    EntityDiggingFX particle = new EntityDiggingFX(
                        world,
                        x + offsetX,
                        y + offsetY,
                        z + offsetZ,
                        offsetX - 0.5D,
                        offsetY - 0.5D,
                        offsetZ - 0.5D,
                        this,
                        metadata);
                    particle.setParticleIcon(icon);
                    effectRenderer.addEffect(particle.applyColourMultiplier(x, y, z));
                }
            }
        }
        return true;
    }

    @Nonnull
    public abstract Class<? extends TileEntity> getTileEntityClass();

    @Nonnull
    public static DrawerAttachment getAttachment(int metadata) {
        return DrawerAttachment.byIndex((metadata & 0xF) / 4);
    }

    @Nonnull
    public static ForgeDirection getHorizontalFacing(int metadata) {
        return HitBoxesUtil.HORIZONTAL[metadata & 3];
    }

    @Nonnull
    public static ForgeDirection getFrontFacing(int metadata) {
        return switch (getAttachment(metadata)) {
            case FLOOR -> ForgeDirection.UP;
            case CEILING -> ForgeDirection.DOWN;
            default -> getHorizontalFacing(metadata);
        };
    }

    public static int getMetadata(@Nonnull DrawerAttachment attachment, @Nonnull ForgeDirection horizontalFacing) {
        return attachment.ordinal() * 4 + HitBoxesUtil.horizontalIndex(horizontalFacing);
    }

    @Override
    public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
        return ForgeDirection.VALID_DIRECTIONS;
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        if (axis == null || axis == ForgeDirection.UNKNOWN) {
            return false;
        }
        int metadata = world.getBlockMetadata(x, y, z);
        DrawerAttachment updatedAttachment;
        ForgeDirection updatedFacing = getHorizontalFacing(metadata);
        if (axis == ForgeDirection.UP) {
            updatedAttachment = DrawerAttachment.FLOOR;
        } else if (axis == ForgeDirection.DOWN) {
            updatedAttachment = DrawerAttachment.CEILING;
        } else {
            updatedAttachment = DrawerAttachment.WALL;
            updatedFacing = axis;
        }
        int updated = getMetadata(updatedAttachment, updatedFacing);
        if (updated == metadata) {
            return false;
        }
        world.setBlockMetadataWithNotify(x, y, z, updated, 3);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return ModelISBRH.JSON_ISBRH_ID;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BakedModel getModel(BakedModelQuadContext context) {
        return DrawerModelProvider.INSTANCE.getModel(context);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getLightOpacity() {
        return 0;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof ControllableDrawerTile ? ((ControllableDrawerTile) tile).getRedstoneSignal(side) : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return side == ForgeDirection.UP.ordinal() ? isProvidingWeakPower(world, x, y, z, side) : 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        world.setBlockMetadataWithNotify(x, y, z, metadataForPlacement(0, placer), 2);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ControllableDrawerTile && stack != null) {
            ((ControllableDrawerTile) tile).loadFromItemStack(stack);
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile)) {
            return 0;
        }
        return ((ControllableDrawerTile) tile).getComparatorOutput();
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    /**
     * Exposes the drawer's item storage as a vanilla inventory so hoppers, pipes,
     * and other 1.7.10 automation can insert and extract.
     *
     * @param world world containing the drawer
     * @param x     block x
     * @param y     block y
     * @param z     block z
     * @return the inventory view, or {@code null} when this drawer stores no items
     */
    @Nullable
    public IInventory getInventoryView(@Nonnull World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof ControllableDrawerTile ? ((ControllableDrawerTile) tile).getInventoryView() : null;
    }

    /**
     * Exposes the drawer's tanks as a Forge fluid handler so fluid pipes can
     * fill and drain them.
     *
     * @param world world containing the drawer
     * @param x     block x
     * @param y     block y
     * @param z     block z
     * @return the fluid handler, or {@code null} when this drawer stores no fluid
     */
    @Nullable
    public IFluidHandler getFluidHandlerView(@Nonnull World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof FluidDrawerTile ? ((FluidDrawerTile) tile).getForgeFluidHandler() : null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (ServerUtilitiesIntegration.blocksInteraction(player, x, y, z)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return false;
        }
        int slot = getHitSlot(world.getBlockMetadata(x, y, z), side, hitX, hitY, hitZ);
        boolean handled = drawer.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
        drawer.sendStorageUpdate(player);
        return handled;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        if (ServerUtilitiesIntegration.blocksInteraction(player, x, y, z)) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return;
        }
        int slot = getHitSlot(world, x, y, z, player);
        if (slot >= 0) {
            drawer.onSlotClicked(player, slot);
            drawer.sendStorageUpdate(player);
        }
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (protectCreativeClick(world, player, x, y, z)) {
            return false;
        }
        return willHarvest || world.setBlockToAir(x, y, z);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, false);
    }

    public boolean protectCreativeClick(World world, EntityPlayer player, int x, int y, int z) {
        return player.capabilities.isCreativeMode && !player.isSneaking() && getHitSlot(world, x, y, z, player) >= 0;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ControllableDrawerTile drawer) {
            drawer.onBlockBroken();
            drawer.detachFromController(world);
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ControllableDrawerTile && FunctionalStorageConfig.GENERAL.keepContentsOnBreak) {
            drops.add(((ControllableDrawerTile) tile).createDropStack(new ItemStack(this, 1, 0)));
        } else {
            drops.add(new ItemStack(this, 1, 0));
        }
        return drops;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ControllableDrawerTile) {
            return ((ControllableDrawerTile) tile).createDropStack(new ItemStack(this, 1, 0));
        }
        return new ItemStack(this, 1, 0);
    }

    /**
     * Resolves which drawer slot the player is looking at.
     *
     * @param world  world containing the drawer
     * @param x      block x
     * @param y      block y
     * @param z      block z
     * @param player interacting player
     * @return the slot index, or {@code -1} when the front face was not hit
     */
    public int getHitSlot(World world, int x, int y, int z, EntityPlayer player) {
        int metadata = world.getBlockMetadata(x, y, z);
        double eyeY = player.posY + player.getEyeHeight() - (world.isRemote ? player.getDefaultEyeHeight() : 0D);
        Vec3 start = Vec3.createVectorHelper(player.posX, eyeY, player.posZ);
        double reach = player instanceof EntityPlayerMP serverPlayer
            ? serverPlayer.theItemInWorldManager.getBlockReachDistance()
            : 5D;
        Vec3 look = player.getLookVec();
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition hit = world.rayTraceBlocks(start, end, false);
        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z) {
            return -1;
        }
        double localX = hit.hitVec.xCoord - x;
        double localY = hit.hitVec.yCoord - y;
        double localZ = hit.hitVec.zCoord - z;
        return getHitSlot(metadata, hit.sideHit, localX, localY, localZ);
    }

    public int getHitSlot(int metadata, int side, double hitX, double hitY, double hitZ) {
        if (side != getFrontFacing(metadata).ordinal()) {
            return -1;
        }
        return HitBoxesUtil
            .resolveSlot(faceLayout, getAttachment(metadata), getHorizontalFacing(metadata), hitX, hitY, hitZ);
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        super.randomDisplayTick(world, x, y, z, random);
    }

    /**
     * Faces the drawer towards the placer, including floor and ceiling mounting.
     *
     * @param placedOn clicked support face, retained for placement integrations
     * @param placer   placing entity
     * @return the metadata to store
     */
    public int metadataForPlacement(int placedOn, @Nonnull EntityLivingBase placer) {
        Vec3 look = placer.getLookVec();
        double horizontal = Math.max(Math.abs(look.xCoord), Math.abs(look.zCoord));
        DrawerAttachment attachment = Math.abs(look.yCoord) > horizontal
            ? (look.yCoord < 0D ? DrawerAttachment.FLOOR : DrawerAttachment.CEILING)
            : DrawerAttachment.WALL;
        int quadrant = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        ForgeDirection facing = HitBoxesUtil.HORIZONTAL[quadrant];
        return getMetadata(attachment, facing);
    }

    public static boolean hasStoredContents(@Nullable ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("TileData", 10);
    }

    private static final String[] CONTENT_KEYS = { "Items", "Tanks", "Aspects", "Compacting" };

    @Nullable
    public static ItemStack cleanseContents(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ItemStack cleansed = stack.copy();
        cleansed.stackSize = 1;
        NBTTagCompound root = cleansed.getTagCompound();
        NBTTagCompound tile = getTileData(cleansed);
        if (root == null || tile == null) {
            return cleansed;
        }
        NBTTagCompound cleansedTile = (NBTTagCompound) tile.copy();
        for (String key : CONTENT_KEYS) {
            cleansedTile.removeTag(key);
        }
        cleansedTile.removeTag("Locked");
        root.setTag("TileData", cleansedTile);
        return cleansed;
    }

    public static boolean hasStoredResource(@Nullable ItemStack stack) {
        NBTTagCompound tile = getTileData(stack);
        if (tile == null) {
            return false;
        }
        for (String key : CONTENT_KEYS) {
            if (containsResource(tile.getCompoundTag(key))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsResource(@Nonnull NBTTagCompound payload) {
        if (payload.getLong("BaseAmount") > 0L) {
            return true;
        }
        NBTTagList entries = payload.getTagList("Entries", 10);
        for (int index = 0; index < entries.tagCount(); index++) {
            if (entries.getCompoundTagAt(index)
                .getLong("Amount") > 0L) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static NBTTagCompound getTileData(@Nullable ItemStack stack) {
        if (!hasStoredContents(stack)) {
            return null;
        }
        return stack.getTagCompound()
            .getCompoundTag("TileData");
    }

    @Nonnull
    public abstract List<String> getVariantNames();
}
