package com.hfstudio.functionalstorage.support;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.storage.FluidStorageResource;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;

/**
 * Shared fixtures for the storage harness.
 *
 * <p>
 * Items are minted without the registry being frozen, so a test can produce as many
 * unrelated types as it needs. Fluids use {@link TestFluidStack}, which exists because
 * Forge cannot construct a fluid stack outside a running game.
 * </p>
 */
public class StorageFixtures {

    private StorageFixtures() {}

    public static Item newItem() {
        return new Item();
    }

    /**
     * An item that does not stack, the shape a tool or a piece of armour has. Storage
     * admitting only unstackable items needs an item whose own limit is one to be
     * exercised at all, and the item declares that limit itself.
     */
    public static Item unstackableItem() {
        return new Item() {

            @Override
            public int getItemStackLimit() {
                return 1;
            }
        };
    }

    public static ItemStack stack(Item item, int size) {
        return new ItemStack(item, size);
    }

    public static ItemStack one(Item item) {
        return new ItemStack(item, 1);
    }

    public static Fluid fluid(String name) {
        return new Fluid(name);
    }

    public static FluidStack fluidStack(Fluid fluid, int amount) {
        return TestFluidStack.of(fluid, amount);
    }

    /**
     * One stack reused across a whole measurement run. Prefer this when a test drives
     * many operations: the storage core copies whatever template it is handed and never
     * mutates the caller's stack, so a single instance keeps the measurement on the code
     * under test.
     */
    public static TestFluidStack reusableFluidStack(Fluid fluid) {
        return TestFluidStack.of(fluid, 1);
    }

    public static Fluid[] fluids(int count) {
        Fluid[] fluids = new Fluid[Math.max(0, count)];
        for (int index = 0; index < fluids.length; index++) {
            fluids[index] = fluid("fluid_" + index);
        }
        return fluids;
    }

    public static BigItemHandler handler(int slots) {
        return new BigItemHandler(slots);
    }

    public static BigItemHandler largeHandler(int slots, double multiplier) {
        return new BigItemHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    public static BigFluidHandler fluidHandler(int slots) {
        return new BigFluidHandler(slots);
    }

    public static BigFluidHandler largeFluidHandler(int slots, double multiplier) {
        return new BigFluidHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    /**
     * A multiplier that lifts one slot's capacity past the int boundary, so a test
     * exercises the long-to-int saturation every Forge caller hits. The factor is how
     * many times the boundary the result must exceed.
     */
    public static double intOverflowingMultiplier(long baseCapacity, double factor) {
        double boundary = (double) Integer.MAX_VALUE + 1D;
        return boundary / Math.max(1L, baseCapacity) * Math.max(1D, factor);
    }

    public static long capacityOf(Item item) {
        return ItemStorageResource.INSTANCE.capacityForStack(one(item));
    }

    public static long fluidCapacity() {
        return FluidStorageResource.INSTANCE.defaultCapacity();
    }

    public static long total(IBigItemHandler handler, Item item) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (handler.getSnapshot(index)
                .getKey() != null
                && handler.getSnapshot(index)
                    .getKey()
                    .getItem() == item) {
                total += handler.getSnapshot(index)
                    .getAmount();
            }
        }
        return total;
    }

    public static long fluidTotal(IBigFluidHandler handler, Fluid fluid) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            BigFluidStack snapshot = handler.getSnapshot(index);
            if (snapshot.hasTemplate() && snapshot.getKey()
                .getFluid() == fluid) {
                total += snapshot.getAmount();
            }
        }
        return total;
    }

    public static long fluidGrandTotal(IBigFluidHandler handler) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
        }
        return total;
    }
}
