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

/**
 * Essentia drawer block. Stores Thaumcraft essentia and exposes it through the
 * native {@code IAspectContainer} interface so jars, tubes, and alembics can
 * interact with it directly.
 */
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

    /**
     * @return the slot layout of this essentia drawer
     */
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
