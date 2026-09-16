package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.DrawerWoodType;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/** One block per wood type and layout, with a stable registry and asset identifier. */
public class WoodDrawerBlock extends DrawerBlock {

    private final DrawerWoodType woodType;
    private final DrawerLayout layout;

    public WoodDrawerBlock(@Nonnull DrawerWoodType woodType, @Nonnull DrawerLayout layout) {
        super(faceLayoutOf(layout), "functionalstorage." + woodType.getId() + "_" + layout.getSlotCount());
        this.woodType = woodType;
        this.layout = layout;
        setBlockTextureName("functionalstorage:" + woodType.getId() + "_front_" + layout.getSlotCount());
    }

    private static DrawerFaceLayout faceLayoutOf(@Nonnull DrawerLayout layout) {
        return switch (layout) {
            case X_2 -> DrawerFaceLayout.X_2;
            case X_4 -> DrawerFaceLayout.X_4;
            default -> DrawerFaceLayout.X_1;
        };
    }

    @Nonnull
    public DrawerWoodType getWoodType() {
        return woodType;
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    public String getDrawerId() {
        return woodType.getId() + "_" + layout.getSlotCount();
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return WoodDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new WoodDrawerTile(layout);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(getDrawerId());
    }
}
