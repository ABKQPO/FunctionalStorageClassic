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
        this("adaptive_drawer", "spruce_front_4");
    }

    public AdaptiveDrawerBlock(@Nonnull String id, @Nonnull String frontTexture) {
        super(DrawerFaceLayout.X_4, FunctionalStorage.MOD_ID + "." + id);
        setBlockTextureName(FunctionalStorage.MOD_ID + ":" + frontTexture);
        this.id = id;
    }

    private final String id;

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
        return Collections.singletonList(id);
    }
}
