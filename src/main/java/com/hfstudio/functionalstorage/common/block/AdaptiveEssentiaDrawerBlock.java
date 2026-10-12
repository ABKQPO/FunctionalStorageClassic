package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.AdaptiveEssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/** Adaptive source-matter drawer using the same cavity geometry as the fluid drawer. */
public class AdaptiveEssentiaDrawerBlock extends EssentiaDrawerBlock {

    private static final String ID = "adaptive_essentia_1";

    public AdaptiveEssentiaDrawerBlock() {
        super(DrawerLayout.X_4);
        setBlockName(FunctionalStorage.MOD_ID + "." + ID);
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return AdaptiveEssentiaDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveEssentiaDrawerTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(ID);
    }
}
