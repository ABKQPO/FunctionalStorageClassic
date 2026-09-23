package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.model.FramedModelHolder;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Retextures exterior, front, and divider marker quads with the selected materials. */
public class FramedDrawerBlock extends DrawerBlock implements IBlockModelProvider, FramedBlock {

    public static final int RECIPE_GRID_SIZE = 4;

    private final DrawerLayout layout;

    public FramedDrawerBlock(@Nonnull DrawerLayout layout) {
        super(faceLayoutOf(layout), FunctionalStorage.MOD_ID + ".framed_" + layout.getSlotCount());
        this.layout = layout;
        setBlockTextureName(FunctionalStorage.MOD_ID + ":framed_side");
    }

    private static DrawerFaceLayout faceLayoutOf(@Nonnull DrawerLayout layout) {
        return switch (layout) {
            case X_2 -> DrawerFaceLayout.X_2;
            case X_4 -> DrawerFaceLayout.X_4;
            default -> DrawerFaceLayout.X_1;
        };
    }

    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    @Nonnull
    public String getDrawerId() {
        return "framed_" + layout.getSlotCount();
    }

    @Nonnull
    @Override
    public Class<? extends ControllableDrawerTile> getTileEntityClass() {
        return FramedDrawerTile.class;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new FramedDrawerTile(layout);
    }

    @Nonnull
    @Override
    public List<String> getVariantNames() {
        return Collections.singletonList(getDrawerId());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BakedModel getModel(BakedModelQuadContext context) {
        return FramedModelHolder.model(context);
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    /**
     * Applies a material to a placed framed drawer.
     *
     * @param world    world containing the drawer
     * @param x        block x
     * @param y        block y
     * @param z        block z
     * @param material block item to copy the texture from
     * @param front    whether the front and divider are being set
     * @return whether the drawer accepted the material
     */
    public boolean applyMaterial(@Nonnull World world, int x, int y, int z, @Nonnull ItemStack material,
        boolean front) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof FramedDrawerTile) || FramedDrawerStyle.materialBlock(material) == null) {
            return false;
        }
        return ((FramedDrawerTile) tile).applyMaterial(material, front);
    }

    /**
     * Builds a styled framed drawer from a 2x2 crafting grid. The grid reads as
     * two exterior cells on the top row, then the front and an optional divider,
     * matching the modern implementation's layout.
     *
     * @param grid   exactly four stacks in row-major order
     * @param drawer target drawer stack
     * @return the styled drawer stack, or {@code null} when the grid is invalid
     */
    @Nullable
    public static ItemStack craft(@Nullable ItemStack[] grid, @Nullable ItemStack drawer) {
        if (grid == null || grid.length != RECIPE_GRID_SIZE || drawer == null || drawer.getItem() == null) {
            return null;
        }
        for (int index = 0; index < grid.length; index++) {
            if ((index != 3 || grid[index] != null) && !isStyleMaterial(grid[index])) {
                return null;
            }
        }
        if (!ItemUtil.areItemStacksEqual(grid[0], grid[1])) {
            return null;
        }
        FramedDrawerStyle style = new FramedDrawerStyle(grid[0], grid[2], grid[3]);
        if (!style.isConfigured()) {
            return null;
        }
        ItemStack result = drawer.copy();
        result.stackSize = 1;
        style.applyDrawerStyle(result);
        return result;
    }

    private static boolean isStyleMaterial(@Nullable ItemStack stack) {
        return FramedDrawerStyle.materialBlock(stack) != null;
    }
}
