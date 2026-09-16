package com.hfstudio.functionalstorage.common.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.hfstudio.functionalstorage.client.model.FramedModelHolder;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.common.storage.FramedDrawerStyle;
import com.hfstudio.functionalstorage.common.tile.FramedDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Framed drawer block. An item drawer whose exterior, front, and divider take
 * their texture from any block the player applies, either by crafting in a 2x2
 * grid or by applying the material in the world.
 *
 * <p>
 * The block supplies its own baked model so the marker quads of the framed
 * model can be replaced with the applied material at render time.
 * </p>
 */
public class FramedDrawerBlock extends DrawerBlock implements IBlockModelProvider {

    /**
     * Number of cells in the framed drawer crafting grid.
     */
    public static final int RECIPE_GRID_SIZE = 4;

    private final DrawerLayout layout;

    public FramedDrawerBlock(@Nonnull DrawerLayout layout) {
        super(faceLayoutOf(layout), "functionalstorage.framed_" + layout.getSlotCount());
        this.layout = layout;
    }

    private static DrawerFaceLayout faceLayoutOf(@Nonnull DrawerLayout layout) {
        return switch (layout) {
            case X_2 -> DrawerFaceLayout.X_2;
            case X_4 -> DrawerFaceLayout.X_4;
            default -> DrawerFaceLayout.X_1;
        };
    }

    /**
     * @return the slot layout of this framed drawer
     */
    @Nonnull
    public DrawerLayout getDrawerLayout() {
        return layout;
    }

    /**
     * @return the stable identifier used for registration and lang keys
     */
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
        if (!(tile instanceof FramedDrawerTile) || !(material.getItem() instanceof ItemBlock)) {
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
        for (ItemStack cell : grid) {
            if (cell == null || cell.getItem() == null || !(cell.getItem() instanceof ItemBlock)) {
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
}
