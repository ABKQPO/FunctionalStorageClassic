package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.AdaptiveDrawerTile;

import lombok.Getter;

public class AdaptiveDrawerBlock extends DrawerBlock {

    private final String id;
    @Getter
    private final String woodName;

    public AdaptiveDrawerBlock() {
        this("adaptive_drawer", "spruce", "adaptive_spruce_front_4_online");
    }

    public AdaptiveDrawerBlock(@Nonnull String id, @Nonnull String woodName, @Nonnull String frontTexture) {
        super(DrawerFaceLayout.X_4, FunctionalStorage.MOD_ID + "." + id);
        setBlockTextureName(FunctionalStorage.MOD_ID + ":" + frontTexture);
        this.id = id;
        this.woodName = woodName;
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
        return Collections.singletonList(id);
    }
}
