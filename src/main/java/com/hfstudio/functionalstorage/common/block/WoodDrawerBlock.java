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

/**
 * Wooden drawer block. One block instance covers every layout of a single wood
 * type; the layout and wood type together form the stable identifier.
 */
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
        switch (layout) {
            case X_2:
                return DrawerFaceLayout.X_2;
            case X_4:
                return DrawerFaceLayout.X_4;
            default:
                return DrawerFaceLayout.X_1;
        }
    }

    /**
     * @return the wood variant of this block
     */
    @Nonnull
    public DrawerWoodType getWoodType() {
        return woodType;
    }

    /**
     * @return the slot layout of this block
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    /**
     * @return the stable identifier used for registration, assets, and lang keys
     */
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
