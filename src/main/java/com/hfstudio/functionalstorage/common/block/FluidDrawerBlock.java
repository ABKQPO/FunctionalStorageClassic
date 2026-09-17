package com.hfstudio.functionalstorage.common.block;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class FluidDrawerBlock extends DrawerBlock {

    private final DrawerLayout layout;

    public FluidDrawerBlock(@Nonnull DrawerLayout layout) {
        super(
            layout == DrawerLayout.X_1 ? DrawerFaceLayout.X_1
                : layout == DrawerLayout.X_2 ? DrawerFaceLayout.X_2 : DrawerFaceLayout.X_4,
            FunctionalStorage.MOD_ID + ".fluid_" + layout.getSlotCount());
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
        return FluidDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new FluidDrawerTile(layout);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return List.of("fluid_" + layout.getSlotCount());
    }
}
