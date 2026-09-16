package com.hfstudio.functionalstorage.common.block.compact;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.compact.CompactingDrawerTile;

public class SimpleCompactingDrawerBlock extends DrawerBlock {

    public static final int TIER_COUNT = 2;

    public SimpleCompactingDrawerBlock() {
        super(DrawerFaceLayout.X_2, "functionalstorage.simple_compacting_drawer");
        setBlockTextureName("functionalstorage:simple_compacting_drawer_front");
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return CompactingDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new CompactingDrawerTile(TIER_COUNT);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("simple_compacting_drawer");
    }
}
