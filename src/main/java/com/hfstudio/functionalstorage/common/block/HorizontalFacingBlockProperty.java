package com.hfstudio.functionalstorage.common.block;

import java.lang.reflect.Type;
import java.util.Locale;

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

/** The low two metadata bits select the horizontal model rotation. */
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
        return switch (trait) {
            case SupportsWorld, SupportsStacks, OnlyNeedsMeta, WorldMutable, StackMutable, Config, Transformable -> true;
            default -> false;
        };
    }

    @Override
    public boolean needsExisting() {
        return true;
    }

    public boolean isHorizontal(@Nonnull ForgeDirection value) {
        return value != ForgeDirection.UNKNOWN && value.offsetY == 0;
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
        return DrawerBlock.getHorizontalFacing(meta);
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
            .toLowerCase(Locale.ROOT);
    }

    @Override
    public ForgeDirection parse(String text) {
        try {
            ForgeDirection direction = ForgeDirection.valueOf(text.toUpperCase(Locale.ROOT));
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
