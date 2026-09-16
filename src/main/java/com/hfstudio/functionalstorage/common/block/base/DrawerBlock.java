package com.hfstudio.functionalstorage.common.block.base;

import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.hfstudio.functionalstorage.common.block.DrawerAttachment;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;
import com.hfstudio.functionalstorage.util.HitBoxesUtil;

/**
 * Base class for every drawer block. A drawer is a full cube whose metadata
 * encodes the surface it is attached to and its horizontal rotation, so one
 * block instance covers all twelve orientations.
 */
public abstract class DrawerBlock extends BlockContainer {

    private static final float HARDNESS = 2.5F;
    private static final float RESISTANCE = 8.0F;

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

    /**
     * @return the arrangement of interactive regions on the front face
     */
    @Nonnull
    public DrawerFaceLayout getFaceLayout() {
        return faceLayout;
    }

    /**
     * @return the tile entity class backing this block
     */
    @Nonnull
    public abstract Class<? extends TileEntity> getTileEntityClass();

    /**
     * @param metadata block metadata
     * @return the attachment encoded in the metadata
     */
    @Nonnull
    public static DrawerAttachment getAttachment(int metadata) {
        return DrawerAttachment.byIndex((metadata & 0xF) / 4);
    }

    /**
     * @param metadata block metadata
     * @return the horizontal facing encoded in the metadata
     */
    @Nonnull
    public static ForgeDirection getHorizontalFacing(int metadata) {
        return ForgeDirection.getOrientation(2 + ((metadata & 0xF) % 4));
    }

    /**
     * Resolves the physical face the drawer opens towards.
     *
     * @param metadata block metadata
     * @return the front facing
     */
    @Nonnull
    public static ForgeDirection getFrontFacing(int metadata) {
        switch (getAttachment(metadata)) {
            case FLOOR:
                return ForgeDirection.UP;
            case CEILING:
                return ForgeDirection.DOWN;
            default:
                return getHorizontalFacing(metadata);
        }
    }

    /**
     * Encodes attachment and facing into metadata.
     *
     * @param attachment       attachment surface
     * @param horizontalFacing horizontal rotation
     * @return the metadata value
     */
    public static int getMetadata(@Nonnull DrawerAttachment attachment, @Nonnull ForgeDirection horizontalFacing) {
        return attachment.ordinal() * 4 + HitBoxesUtil.horizontalIndex(horizontalFacing);
    }

    @Override
    public int getRenderType() {
        return ModelISBRH.JSON_ISBRH_ID;
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
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile)) {
            return false;
        }
        ControllableDrawerTile drawer = (ControllableDrawerTile) tile;
        int slot = getHitSlot(world, x, y, z, player);
        return drawer.onSlotActivated(player, side, hitX, hitY, hitZ, slot);
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile)) {
            return;
        }
        int slot = getHitSlot(world, x, y, z, player);
        if (slot >= 0) {
            ((ControllableDrawerTile) tile).onSlotClicked(player, slot);
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ControllableDrawerTile) {
            ControllableDrawerTile drawer = (ControllableDrawerTile) tile;
            drawer.onBlockBroken();
            drawer.detachFromController(world);
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }

    @Override
    public java.util.ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        java.util.ArrayList<ItemStack> drops = new java.util.ArrayList<>();
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
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 end = start.addVector(
            player.getLookVec().xCoord * 6D,
            player.getLookVec().yCoord * 6D,
            player.getLookVec().zCoord * 6D);
        MovingObjectPosition hit = world.rayTraceBlocks(start, end, false);
        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z) {
            return -1;
        }
        if (hit.sideHit != getFrontFacing(metadata).ordinal()) {
            return -1;
        }
        double localX = hit.hitVec.xCoord - x;
        double localY = hit.hitVec.yCoord - y;
        double localZ = hit.hitVec.zCoord - z;
        return HitBoxesUtil
            .resolveSlot(faceLayout, getAttachment(metadata), getHorizontalFacing(metadata), localX, localY, localZ);
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        super.randomDisplayTick(world, x, y, z, random);
    }

    /**
     * Reads the safe metadata value from a placement face.
     *
     * @param placedOn face that was clicked during placement
     * @param placer   placing entity
     * @return the metadata to store
     */
    public int metadataForPlacement(int placedOn, @Nonnull EntityLivingBase placer) {
        DrawerAttachment attachment = HitBoxesUtil.attachmentForFace(placedOn);
        int quadrant = MathHelper.floor_double((placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        ForgeDirection facing = attachment == DrawerAttachment.WALL ? HitBoxesUtil.horizontalFromQuadrant(quadrant + 2)
            : HitBoxesUtil.horizontalFromQuadrant(quadrant);
        return getMetadata(attachment, facing);
    }

    /**
     * @param stack drawer item stack
     * @return whether the stack carries stored contents
     */
    public static boolean hasStoredContents(@Nullable ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("TileData", 10);
    }

    /**
     * @param stack drawer item stack
     * @return the persisted tile data, or {@code null}
     */
    @Nullable
    public static NBTTagCompound getTileData(@Nullable ItemStack stack) {
        if (!hasStoredContents(stack)) {
            return null;
        }
        return stack.getTagCompound()
            .getCompoundTag("TileData");
    }

    /**
     * @return the localized block name suffix list for this block's variants
     */
    @Nonnull
    public abstract List<String> getVariantNames();
}
