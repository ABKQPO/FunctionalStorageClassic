package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ResourceGenerationUpgradeItem extends AutomationUpgradeItem {

    private final ItemStack item;
    private final FluidStack fluid;
    private final LongSupplier generationAmount;

    public ResourceGenerationUpgradeItem(String id, int interval, ItemStack item, FluidStack fluid) {
        this(id, interval, item, fluid, item == null ? fluid == null ? 0L : fluid.amount : item.stackSize);
    }

    public ResourceGenerationUpgradeItem(String id, int interval, ItemStack item, FluidStack fluid,
        long generationAmount) {
        this(id, () -> interval, item, fluid, () -> generationAmount);
    }

    public ResourceGenerationUpgradeItem(String id, IntSupplier interval, ItemStack item, FluidStack fluid,
        LongSupplier generationAmount) {
        super(id, interval);
        this.item = copyItemTemplate(item);
        this.fluid = copyFluidTemplate(fluid);
        this.generationAmount = Objects.requireNonNull(generationAmount, "generationAmount");
    }

    @Override
    public boolean hasDirection() {
        return false;
    }

    @Override
    public boolean hasOwner() {
        return false;
    }

    @Override
    public void work(ControllableDrawerTile tile, ItemStack stack, int slot) {
        if (tile.getWorldObj() == null || tile.getWorldObj().isRemote) return;
        if (tile.getItemHandler() != null) {
            ItemStack item = resolveGeneratedItem(stack);
            if (item != null && item.getItem() != null) {
                IBigItemHandler storage = UpgradeSettings.itemStorage(tile.getItemHandler(), stack);
                BigItemStack request = new BigItemStack(item, scaleAmount(stack, getGenerationAmount()));
                if (storage.hasRoomInSingleSlot(request)) {
                    storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
                }
            }
        }
        if (fluid != null && tile.getFluidHandler() != null) {
            IBigFluidHandler storage = UpgradeSettings.fluidStorage(tile.getFluidHandler(), stack);
            BigFluidStack request = new BigFluidStack(fluid, scaleAmount(stack, getGenerationAmount()));
            if (storage.hasRoomInSingleSlot(request)) {
                storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
            }
        }
    }

    @Nullable
    public ItemStack getGeneratedItem() {
        return item == null ? null : item.copy();
    }

    @Nullable
    protected ItemStack resolveGeneratedItem(ItemStack upgrade) {
        return item;
    }

    @Nullable
    public FluidStack getGeneratedFluid() {
        return fluid == null ? null : fluid.copy();
    }

    public long getGenerationAmount() {
        return Math.max(0L, generationAmount.getAsLong());
    }

    @Override
    public void addDescription(List<String> tooltip) {}

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        ItemStack item = resolveGeneratedItem(stack);
        if (item == null && fluid == null) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.upgrade.filter_empty"));
            return;
        }
        tooltip.add(
            item == null
                ? StatCollector.translateToLocalFormatted(
                    "functionalupgrade.desc.generate_fluid",
                    scaleAmount(stack, getGenerationAmount()),
                    fluid.getLocalizedName())
                : StatCollector.translateToLocalFormatted(
                    "functionalupgrade.desc.generate_item",
                    scaleAmount(stack, getGenerationAmount()),
                    item.getDisplayName()));
    }

    private static ItemStack copyItemTemplate(@Nullable ItemStack source) {
        if (source == null) return null;
        ItemStack copy = source.copy();
        copy.stackSize = 1;
        return copy;
    }

    private static FluidStack copyFluidTemplate(@Nullable FluidStack source) {
        if (source == null) return null;
        FluidStack copy = source.copy();
        copy.amount = 1;
        return copy;
    }
}
