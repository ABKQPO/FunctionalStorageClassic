package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class EssentiaDrawerBlock extends DrawerBlock {

    private final DrawerLayout layout;

    public EssentiaDrawerBlock(@Nonnull DrawerLayout layout) {
        super(
            layout == DrawerLayout.X_1 ? DrawerFaceLayout.X_1
                : layout == DrawerLayout.X_2 ? DrawerFaceLayout.X_2 : DrawerFaceLayout.X_4,
            FunctionalStorage.MOD_ID + ".essentia_" + layout.getSlotCount());
        this.layout = layout;
        setBlockTextureName(FunctionalStorage.MOD_ID + ":fluid_front");
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return EssentiaDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new EssentiaDrawerTile(layout);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("essentia_" + layout.getSlotCount());
    }
}
