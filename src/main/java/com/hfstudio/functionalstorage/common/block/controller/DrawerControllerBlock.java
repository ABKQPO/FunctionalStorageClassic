package com.hfstudio.functionalstorage.common.block.controller;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;

/**
 * Storage controller block. Linked drawers are aggregated into one logical
 * inventory so automation can address the whole network.
 */
public class DrawerControllerBlock extends DrawerBlock {

    public DrawerControllerBlock() {
        super(DrawerFaceLayout.X_1, "functionalstorage.storage_controller");
        setBlockTextureName("functionalstorage:controller_front");
    }

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return DrawerControllerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new DrawerControllerTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("storage_controller");
    }
}
