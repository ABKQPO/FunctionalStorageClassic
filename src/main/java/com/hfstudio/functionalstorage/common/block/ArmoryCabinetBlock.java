package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

public class ArmoryCabinetBlock extends DrawerBlock {

    public ArmoryCabinetBlock() {
        super(DrawerFaceLayout.X_1, FunctionalStorage.MOD_ID + ".armory_cabinet");
        setBlockTextureName(FunctionalStorage.MOD_ID + ":armory_front");
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
