package com.hfstudio.functionalstorage.common.integration.findit;

import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageView;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;

public class DrawerStackFilter implements Predicate<DrawerStackFilter.Query> {

    private final IBigItemHandler items;
    private final IBigFluidHandler fluids;
    private final IBigAspectHandler aspects;

    private DrawerStackFilter(IBigItemHandler items, IBigFluidHandler fluids, IBigAspectHandler aspects) {
        this.items = items;
        this.fluids = fluids;
        this.aspects = aspects;
    }

    @Nullable
    public static DrawerStackFilter of(@Nonnull ControllableDrawerTile drawer) {
        Objects.requireNonNull(drawer, "drawer");
        IBigItemHandler items = drawer.getItemHandler();
        IBigFluidHandler fluids = drawer.getFluidHandler();
        IBigAspectHandler aspects = Mods.Thaumcraft.isModLoaded() ? drawer.getAspectHandler() : null;
        if (items == null && fluids == null && aspects == null) {
            return null;
        }
        return new DrawerStackFilter(items, fluids, aspects);
    }

    @Nullable
    public static DrawerStackFilter of(@Nonnull TileEntity tile) {
        return tile instanceof ControllableDrawerTile drawer ? of(drawer) : null;
    }

    @Override
    public boolean test(@Nonnull Query query) {
        if (items != null && query.item() != null && holdsItem(query.item())) {
            return true;
        }
        if (fluids != null && query.fluid() != null && holdsFluid(query.fluid())) {
            return true;
        }
        if (aspects == null || !query.matchesAspects()) {
            return false;
        }
        return Mods.Thaumcraft.isModLoaded() && holdsAspect();
    }

    private boolean holdsItem(@Nonnull ItemStack target) {
        for (ItemStorageView view : ItemStorageView.storages(items)) {
            if (view.getSnapshot()
                .isSameType(target)) {
                return true;
            }
        }
        return false;
    }

    private boolean holdsFluid(@Nonnull FluidStack target) {
        for (int index = 0; index < fluids.getStorageCount(); index++) {
            BigFluidStack snapshot = fluids.getSnapshot(index);
            FluidStack template = snapshot.getTemplate();
            if (snapshot.getAmount() > 0L && template != null && target.isFluidEqual(template)) {
                return true;
            }
        }
        return false;
    }

    @Optional.Method(modid = "Thaumcraft")
    private boolean holdsAspect() {
        for (int index = 0; index < aspects.getStorageCount(); index++) {
            BigAspectStack snapshot = aspects.getSnapshot(index);
            if (snapshot.getAmount() > 0L && snapshot.getAspect() != null) {
                return true;
            }
        }
        return false;
    }

    public record Query(@Nullable ItemStack item, @Nullable FluidStack fluid, boolean matchesAspects) {}
}
