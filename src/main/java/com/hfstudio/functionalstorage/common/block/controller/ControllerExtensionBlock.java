package com.hfstudio.functionalstorage.common.block.controller;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.controller.ControllerExtensionTile;

public class ControllerExtensionBlock extends DrawerBlock {

    public ControllerExtensionBlock() {
        super(DrawerFaceLayout.X_1, "functionalstorage.controller_extension");
        setBlockTextureName("functionalstorage:controller_extension");
    }

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return ControllerExtensionTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new ControllerExtensionTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("controller_extension");
    }
}
