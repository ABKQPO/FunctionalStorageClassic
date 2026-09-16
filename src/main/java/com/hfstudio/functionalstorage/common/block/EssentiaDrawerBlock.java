package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/** Exposes essentia storage through Thaumcraft container and tube interfaces. */
public class EssentiaDrawerBlock extends DrawerBlock {

    private final DrawerLayout layout;

    public EssentiaDrawerBlock(@Nonnull DrawerLayout layout) {
        super(
            layout == DrawerLayout.X_1 ? DrawerFaceLayout.X_1
                : layout == DrawerLayout.X_2 ? DrawerFaceLayout.X_2 : DrawerFaceLayout.X_4,
            "functionalstorage.essentia_" + layout.getSlotCount());
        this.layout = layout;
        setBlockTextureName("functionalstorage:fluid_front");
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
