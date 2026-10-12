package com.hfstudio.functionalstorage.common.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;

public class AdaptiveCraftingGridBlock extends AdaptiveGridBlock {

    public AdaptiveCraftingGridBlock() {
        super("adaptive_crafting_grid");
    }

    @Override
    public @NotNull Class<? extends TileEntity> getTileEntityClass() {
        return AdaptiveCraftingGridTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new AdaptiveCraftingGridTile();
    }
}
