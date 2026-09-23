package com.hfstudio.functionalstorage.common.integration.findit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnh.findit.FindIt;
import com.gtnh.findit.IStackFilter;
import com.gtnh.findit.service.itemfinder.FindItemRequest;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;

import cpw.mods.fml.common.Optional;

@Optional.Interface(iface = "com.gtnh.findit.IStackFilter$IStackFilterProvider", modid = "findit", striprefs = true)
public class DrawerNetworkFindProvider implements IStackFilter.IStackFilterProvider {

    @Optional.Method(modid = "findit")
    public static boolean register() {
        try {
            FindIt instance = FindIt.INSTANCE;
            if (instance == null || instance.pluginsList == null) {
                return false;
            }
            instance.pluginsList.add(new DrawerNetworkFindProvider());
            return true;
        } catch (LinkageError | RuntimeException incompatible) {
            return false;
        }
    }

    @Override
    @Optional.Method(modid = "findit")
    public IStackFilter getFilter(EntityPlayer player, TileEntity tileEntity) {
        if (tileEntity instanceof DrawerControllerTile controller) {
            return new ControllerNetworkFilter(controller);
        }
        if (tileEntity instanceof ControllableDrawerTile drawer) {
            DrawerStackFilter filter = DrawerStackFilter.of(drawer);
            return filter == null ? null : new SingleDrawerFilter(filter);
        }
        return null;
    }

    @Override
    public IStackFilter getFilter(EntityPlayer player, ItemStack stack) {
        return null;
    }

    private static class SingleDrawerFilter implements IStackFilter {

        private final DrawerStackFilter filter;

        private SingleDrawerFilter(DrawerStackFilter filter) {
            this.filter = filter;
        }

        @Override
        @Optional.Method(modid = "findit")
        public boolean matches(FindItemRequest request) {
            return filter.test(FindItQueries.of(request));
        }
    }

    private static class ControllerNetworkFilter implements IStackFilter {

        private final DrawerControllerTile controller;

        private ControllerNetworkFilter(DrawerControllerTile controller) {
            this.controller = controller;
        }

        @Override
        @Optional.Method(modid = "findit")
        public boolean matches(FindItemRequest request) {
            DrawerStackFilter.Query query = FindItQueries.of(request);
            DrawerStackFilter aggregate = DrawerStackFilter.of(controller);
            if (aggregate != null && aggregate.test(query)) {
                return true;
            }
            return anyLinkedDrawerMatches(query);
        }

        private boolean anyLinkedDrawerMatches(@Nonnull DrawerStackFilter.Query query) {
            for (long position : controller.getDrawers()) {
                DrawerStackFilter filter = filterAt(position);
                if (filter != null && filter.test(query)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private DrawerStackFilter filterAt(long position) {
            World world = controller.getWorldObj();
            if (world == null) {
                return null;
            }
            int x = ConnectedDrawerScope.unpackX(position);
            int y = ConnectedDrawerScope.unpackY(position);
            int z = ConnectedDrawerScope.unpackZ(position);
            if (!world.blockExists(x, y, z)) {
                return null;
            }
            return DrawerStackFilter.of(world.getTileEntity(x, y, z));
        }
    }
}
