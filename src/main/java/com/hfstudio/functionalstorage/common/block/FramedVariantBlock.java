package com.hfstudio.functionalstorage.common.block;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.model.FramedModelHolder;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FramedVariantBlock extends DrawerBlock implements FramedBlock {

    private final String id;
    private final DrawerBlock delegate;

    public FramedVariantBlock(@Nonnull String id, @Nonnull DrawerBlock delegate, @Nonnull String defaultTexture) {
        super(delegate.getFaceLayout(), FunctionalStorage.MOD_ID + "." + id);
        this.id = id;
        this.delegate = delegate;
        setBlockTextureName(defaultTexture);
    }

    @Override
    public Class<? extends TileEntity> getTileEntityClass() {
        return delegate.getTileEntityClass();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return delegate.createNewTileEntity(world, metadata);
    }

    @Override
    public List<String> getVariantNames() {
        return List.of(id);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BakedModel getModel(BakedModelQuadContext context) {
        return FramedModelHolder.model(context);
    }
}
