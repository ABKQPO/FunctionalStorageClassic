package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.AdaptiveRangeExtenderTile;

public class AdaptiveRangeExtenderBlock extends DrawerBlock {

    public AdaptiveRangeExtenderBlock() {
        super(DrawerFaceLayout.X_1, FunctionalStorage.MOD_ID + ".adaptive_range_extender");
        setBlockTextureName(FunctionalStorage.MOD_ID + ":controller_extension");
    }

    @Nonnull
    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return AdaptiveRangeExtenderTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveRangeExtenderTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("adaptive_range_extender");
    }
}
