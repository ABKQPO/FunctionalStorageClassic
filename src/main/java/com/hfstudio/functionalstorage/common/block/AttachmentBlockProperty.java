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

/** Attachment occupies the metadata bits above the two horizontal rotation bits. */
public class AttachmentBlockProperty implements BlockProperty<DrawerAttachment>, MetaBlockProperty<DrawerAttachment>,
    TransformableProperty<DrawerAttachment> {

    private static final int SHIFT = 2;

    @Override
    public String getName() {
        return "attachment";
    }

    @Override
    public Type getType() {
        return DrawerAttachment.class;
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

    @Override
    public boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta,
        @Nullable TileEntity tile) {
        return block instanceof DrawerBlock;
    }

    @Override
    public int getMeta(DrawerAttachment value, int existing) {
        return (existing & 0b11) | (value.ordinal() << SHIFT);
    }

    @Override
    public DrawerAttachment getValue(int meta) {
        return DrawerAttachment.byIndex((meta & 0b1100) >> SHIFT);
    }

    @Nonnull
    @Override
    public DrawerAttachment transform(@Nonnull DrawerAttachment value, @Nonnull DirectionTransform transform) {
        ForgeDirection direction = transform.apply(value.asDirection());
        return switch (direction) {
            case UP -> DrawerAttachment.FLOOR;
            case DOWN -> DrawerAttachment.CEILING;
            default -> DrawerAttachment.WALL;
        };
    }

    @Override
    public String stringify(DrawerAttachment value) {
        return value.getId();
    }

    @Override
    public DrawerAttachment parse(String text) {
        for (DrawerAttachment attachment : DrawerAttachment.values()) {
            if (attachment.getId()
                .equals(text)) {
                return attachment;
            }
        }
        return DrawerAttachment.WALL;
    }

    @Override
    public void setValue(World world, int x, int y, int z, DrawerAttachment value) {
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, getMeta(value, meta), 2);
    }
}
