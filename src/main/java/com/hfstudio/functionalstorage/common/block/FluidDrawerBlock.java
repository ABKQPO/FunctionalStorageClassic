package com.hfstudio.functionalstorage.common.block;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

/**
 * Fluid drawer block. One block instance per slot count; the tanks are exposed
 * through Forge's fluid handler capability by the tile.
 */
public class FluidDrawerBlock extends DrawerBlock {

    private final DrawerLayout layout;

    public FluidDrawerBlock(@Nonnull DrawerLayout layout) {
        super(
            layout == DrawerLayout.X_1 ? DrawerFaceLayout.X_1
                : layout == DrawerLayout.X_2 ? DrawerFaceLayout.X_2 : DrawerFaceLayout.X_4,
            "functionalstorage.fluid_" + layout.getSlotCount());
        this.layout = layout;
        setBlockTextureName("functionalstorage:fluid_front");
    }

    /**
     * @return the slot layout of this fluid drawer
     */
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
