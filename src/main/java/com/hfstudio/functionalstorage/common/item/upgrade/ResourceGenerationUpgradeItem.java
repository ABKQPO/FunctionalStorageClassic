package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;

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

    public ResourceGenerationUpgradeItem(String id, int interval, ItemStack item, FluidStack fluid) {
        super(id, interval);
        this.item = item == null ? null : item.copy();
        this.fluid = fluid == null ? null : fluid.copy();
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
        if (item != null && tile.getItemHandler() != null) {
            IBigItemHandler storage = UpgradeSettings.itemStorage(tile.getItemHandler(), stack);
            BigItemStack request = new BigItemStack(item, item.stackSize);
            if (storage.hasRoomInSingleSlot(request)) {
                storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
            }
        }
        if (fluid != null && tile.getFluidHandler() != null) {
            IBigFluidHandler storage = UpgradeSettings.fluidStorage(tile.getFluidHandler(), stack);
            BigFluidStack request = new BigFluidStack(fluid, fluid.amount);
            if (storage.hasRoomInSingleSlot(request)) {
                storage.insertIntoSingleSlot(request, StorageAction.EXECUTE);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(
            item == null
                ? StatCollector.translateToLocalFormatted(
                    "functionalupgrade.desc.generate_fluid",
                    fluid.amount,
                    fluid.getLocalizedName())
                : StatCollector.translateToLocalFormatted(
                    "functionalupgrade.desc.generate_item",
                    item.stackSize,
                    item.getDisplayName()));
    }
}
