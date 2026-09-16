package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class ArmoryCabinetBlock extends DrawerBlock {

    public ArmoryCabinetBlock() {
        super(DrawerFaceLayout.X_1, "functionalstorage.armory_cabinet");
        setBlockTextureName("functionalstorage:armory_front");
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return ArmoryCabinetTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new ArmoryCabinetTile();
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList("armory_cabinet");
    }
}
