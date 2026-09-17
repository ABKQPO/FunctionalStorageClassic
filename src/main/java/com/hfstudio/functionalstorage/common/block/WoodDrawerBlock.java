package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.IWoodType;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.WoodDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * One block per wood type and layout, with a stable registry and asset
 * identifier. The wood is an {@link IWoodType} rather than a concrete enum, so a
 * wood contributed by another mod registers exactly like a built-in one.
 */
public class WoodDrawerBlock extends DrawerBlock {

    private final IWoodType woodType;
    private final DrawerLayout layout;

    public WoodDrawerBlock(@Nonnull IWoodType woodType, @Nonnull DrawerLayout layout) {
        super(faceLayoutOf(layout), FunctionalStorage.MOD_ID + "." + woodType.getName() + "_" + layout.getSlotCount());
        this.woodType = woodType;
        this.layout = layout;
        setBlockTextureName(FunctionalStorage.MOD_ID + ":" + woodType.getName() + "_front_" + layout.getSlotCount());
    }

    private static DrawerFaceLayout faceLayoutOf(@Nonnull DrawerLayout layout) {
        return switch (layout) {
            case X_2 -> DrawerFaceLayout.X_2;
            case X_4 -> DrawerFaceLayout.X_4;
            default -> DrawerFaceLayout.X_1;
        };
    }

    @Nonnull
    public IWoodType getWoodType() {
        return woodType;
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    public String getDrawerId() {
        return woodType.getName() + "_" + layout.getSlotCount();
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
