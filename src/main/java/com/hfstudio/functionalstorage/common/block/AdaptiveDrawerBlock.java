package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.AdaptiveDrawerTile;

public class AdaptiveDrawerBlock extends DrawerBlock {

    public AdaptiveDrawerBlock() {
        super(DrawerFaceLayout.X_4, FunctionalStorage.MOD_ID + ".adaptive_drawer");
        setBlockTextureName(FunctionalStorage.MOD_ID + ":oak_front_4");
    }

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return AdaptiveDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveDrawerTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("adaptive_drawer");
    }
}
