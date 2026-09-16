package com.hfstudio.functionalstorage.common.block;

import java.lang.reflect.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.TransformableProperty;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

/**
 * GTNHLib block property exposing a drawer's horizontal rotation. The two low
 * metadata bits hold the rotation so {@code horizontal_facing} variants in the
 * blockstate JSON rotate the model exactly like a vanilla furnace.
 */
public class HorizontalFacingBlockProperty
    implements BlockProperty<ForgeDirection>, MetaBlockProperty<ForgeDirection>, TransformableProperty<ForgeDirection> {

    private static final ForgeDirection[] VALUES = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
        ForgeDirection.WEST };

    @Override
    public String getName() {
        return "horizontal_facing";
    }

    @Override
    public Type getType() {
        return ForgeDirection.class;
    }

    @Override
    public boolean hasTrait(BlockPropertyTrait trait) {
        switch (trait) {
            case SupportsWorld:
            case SupportsStacks:
            case OnlyNeedsMeta:
            case WorldMutable:
            case StackMutable:
            case Config:
            case Transformable:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean needsExisting() {
        return true;
    }

    /**
     * @param value candidate direction
     * @return whether the direction is horizontal
     */
    public boolean isHorizontal(@Nonnull ForgeDirection value) {
        return value.offsetY == 0;
    }

    @Override
    public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
        @Nullable TileEntity tile) {
        return block instanceof DrawerBlock;
    }

    @Override
    public int getMeta(ForgeDirection value, int existing) {
        return (existing & 0b1100) | indexOf(value);
    }

    @Override
    public ForgeDirection getValue(int meta) {
        return VALUES[meta & 0b11];
    }

    @Nonnull
    @Override
    public ForgeDirection transform(@Nonnull ForgeDirection value, @Nonnull DirectionTransform transform) {
        ForgeDirection direction = transform.apply(value);
        return isHorizontal(direction) ? direction : value;
    }

    @Override
    public String stringify(ForgeDirection value) {
        return value.name()
            .toLowerCase();
    }

    @Override
    public ForgeDirection parse(String text) {
        try {
            ForgeDirection direction = ForgeDirection.valueOf(text.toUpperCase());
            return isHorizontal(direction) ? direction : ForgeDirection.NORTH;
        } catch (IllegalArgumentException ignored) {
            return ForgeDirection.NORTH;
        }
    }

    @Override
    public void setValue(World world, int x, int y, int z, ForgeDirection value) {
        int meta = world.getBlockMetadata(x, y, z);
        int updated = getMeta(value, meta);
        if (updated != meta) {
            world.setBlockMetadataWithNotify(x, y, z, updated, 2);
        }
    }

    private int indexOf(@Nonnull ForgeDirection direction) {
        for (int index = 0; index < VALUES.length; index++) {
            if (VALUES[index] == direction) {
                return index;
            }
        }
        return 0;
    }
}
