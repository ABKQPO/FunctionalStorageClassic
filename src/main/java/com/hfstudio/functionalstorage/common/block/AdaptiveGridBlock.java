package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;

public class AdaptiveGridBlock extends DrawerBlock {

    private final String gridId;

    public AdaptiveGridBlock() {
        this("adaptive_grid");
    }

    protected AdaptiveGridBlock(String gridId) {
        super(DrawerFaceLayout.X_1, FunctionalStorage.MOD_ID + "." + gridId);
        this.gridId = gridId;
        setBlockTextureName(FunctionalStorage.MOD_ID + ":controller_extension");
    }

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return AdaptiveGridTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveGridTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(gridId);
    }
}
