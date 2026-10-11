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

    private final String id;

    public AdaptiveFluidDrawerBlock(@Nonnull DrawerLayout layout) {
        super(layout);
        this.id = "adaptive_fluid_" + layout.getSlotCount();
        setBlockName(FunctionalStorage.MOD_ID + "." + id);
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return AdaptiveFluidDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveFluidDrawerTile(getDrawerLayout());
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(id);
    }
}
