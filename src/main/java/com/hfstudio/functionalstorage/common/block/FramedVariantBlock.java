package com.hfstudio.functionalstorage.common.block;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.hfstudio.functionalstorage.client.model.FramedModelHolder;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class FramedVariantBlock extends DrawerBlock implements FramedBlock {

    private final String id;
    private final DrawerBlock delegate;

    public FramedVariantBlock(String id, DrawerBlock delegate) {
        super(delegate.getFaceLayout(), "functionalstorage." + id);
        this.id = id;
        this.delegate = delegate;
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
