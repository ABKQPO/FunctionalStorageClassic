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

/**
 * Compacting drawer block with three visible compression tiers.
 */
public class CompactingDrawerBlock extends DrawerBlock {

    public CompactingDrawerBlock() {
        super(DrawerFaceLayout.X_4, "functionalstorage.compacting_drawer");
        setBlockTextureName("functionalstorage:compacting_drawer_front");
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return CompactingDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new CompactingDrawerTile(3);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("compacting_drawer");
    }
}
