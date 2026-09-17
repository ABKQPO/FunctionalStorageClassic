package com.hfstudio.functionalstorage.common.integration.bogosorter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.cleanroommc.bogosorter.api.IBogoSortAPI;
import com.cleanroommc.bogosorter.common.PinnedSlots;
import com.cleanroommc.bogosorter.mixins.early.minecraft.SlotAccessor;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer.StorageSlot;

import cpw.mods.fml.common.Optional;

public class BogoSorterIntegration {

    @Optional.Method(modid = "bogosorter")
    public static void register() {
        IBogoSortAPI.getInstance()
            .addSlotGetter(StorageSlot.class, BogoSorterIntegration::displaySlot);
    }

    @Optional.Method(modid = "bogosorter")
    private static SlotAccessor displaySlot(Slot slot) {
        return (SlotAccessor) new DisplaySlot(slot);
    }

    @Optional.Method(modid = "bogosorter")
    public static boolean isPinned(EntityPlayer player, int inventorySlot) {
        return PinnedSlots.isPinned(player, inventorySlot);
    }

    public static class DisplaySlot extends Slot {

        public DisplaySlot(Slot slot) {
            super(slot.inventory, slot.getSlotIndex(), slot.xDisplayPosition, slot.yDisplayPosition);
            slotNumber = slot.slotNumber;
        }

        @Override
        public ItemStack getStack() {
            return null;
        }

        @Override
        public void putStack(ItemStack stack) {}

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }
    }
}
