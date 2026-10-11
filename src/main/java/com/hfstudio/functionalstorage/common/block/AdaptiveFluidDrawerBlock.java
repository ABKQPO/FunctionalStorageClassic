package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.AdaptiveFluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class AdaptiveFluidDrawerBlock extends FluidDrawerBlock {

    public AdaptiveFluidDrawerBlock() {
        super(DrawerLayout.X_1);
        setBlockName(FunctionalStorage.MOD_ID + ".adaptive_fluid_drawer");
        setBlockTextureName(FunctionalStorage.MOD_ID + ":fluid_front");
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return AdaptiveFluidDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveFluidDrawerTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("adaptive_fluid_drawer");
    }
}
