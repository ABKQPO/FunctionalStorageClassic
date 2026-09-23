package com.hfstudio.functionalstorage.common.integration.findit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnh.findit.service.itemfinder.FindItemRequest;

import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Optional;

@Optional.Interface(iface = "com.gtnh.findit.service.itemfinder.FindItemRequest", modid = "findit", striprefs = true)
public class FindItQueries {

    @Nonnull
    @Optional.Method(modid = "findit")
    public static DrawerStackFilter.Query of(@Nonnull FindItemRequest request) {
        ItemStack item = request.getStackToFind();
        FluidStack fluid = fluidOf(item);
        return new DrawerStackFilter.Query(item, fluid, fluid == null);
    }

    @Nullable
    @Optional.Method(modid = "NotEnoughItems")
    private static FluidStack fluidOf(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        return StackInfo.getFluid(stack);
    }
}
