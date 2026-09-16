package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.EnderDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Ender drawer block. One slot whose contents are shared between every drawer
 * bound to the same frequency.
 */
public class EnderDrawerBlock extends DrawerBlock {

    public EnderDrawerBlock() {
        super(DrawerFaceLayout.X_1, "functionalstorage.ender_drawer");
        setBlockTextureName("functionalstorage:ender_front");
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return EnderDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new EnderDrawerTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("ender_drawer");
    }
}
