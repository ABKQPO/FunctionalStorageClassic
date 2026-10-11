package com.hfstudio.functionalstorage.common.block.controller;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

public class AdaptiveMatrixBlock extends DrawerControllerBlock {

    public AdaptiveMatrixBlock() {
        this("adaptive_matrix");
    }

    public AdaptiveMatrixBlock(@Nonnull String id) {
        super();
        setBlockName(FunctionalStorage.MOD_ID + "." + id);
        setBlockTextureName(FunctionalStorage.MOD_ID + ":controller_front");
        this.id = id;
    }

    private final String id;

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return AdaptiveMatrixTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveMatrixTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(id);
    }
}
