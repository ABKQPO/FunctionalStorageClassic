package com.hfstudio.functionalstorage.support;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.common.storage.FluidStorageResource;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;

/**
 * Shared fixtures for the storage harness.
 *
 * <p>
 * Items are minted without the registry being frozen, so a test can produce as
 * many unrelated types as it needs. Fluids use {@link TestFluidStack}, which
 * exists because Forge cannot construct a fluid stack outside a running game.
 * </p>
 */
public class StorageFixtures {

    private StorageFixtures() {}

    /**
     * Mints an item free of any registry or name binding.
     *
     * @return a fresh item usable as an unrelated storage type
     */
    public static Item newItem() {
        return new Item();
    }

    /**
     * @param item item to wrap
     * @param size stack count
     * @return a stack of the given item
     */
    public static ItemStack stack(Item item, int size) {
        return new ItemStack(item, size);
    }

    /**
     * @param item item to wrap
     * @return a count-one stack of the given item
     */
    public static ItemStack one(Item item) {
        return new ItemStack(item, 1);
    }

    /**
     * Creates a fluid that never reaches Forge's registry, which is enough for
     * identity comparison because that only compares fluid references.
     *
     * @param name unique fluid name
     * @return a distinct fluid
     */
    public static Fluid fluid(String name) {
        return new Fluid(name);
    }

    /**
     * Creates a fluid stack that behaves like Forge's own outside a running game.
     *
     * <p>
     * Prefer {@link #reusableFluidStack(Fluid)} when a test drives many operations:
     * the storage core copies whatever template it is handed and never mutates the
     * caller's stack, so one instance can serve a whole run and keep the measurement
     * on the code under test.
     * </p>
     *
     * @param fluid  fluid identity
     * @param amount amount in millibuckets
     * @return a stack usable as a storage template
     */
    public static FluidStack fluidStack(Fluid fluid, int amount) {
        return TestFluidStack.of(fluid, amount);
    }

    /**
     * Creates one stack to be reused across an entire measurement run.
     *
     * @param fluid fluid identity
     * @return a mutable stack whose amount the caller may set per operation
     */
    public static TestFluidStack reusableFluidStack(Fluid fluid) {
        return TestFluidStack.of(fluid, 1);
    }

    /**
     * Creates a distinct fluid for each index.
     *
     * @param count how many fluids to mint
     * @return an array of distinct fluids
     */
    public static Fluid[] fluids(int count) {
        Fluid[] fluids = new Fluid[Math.max(0, count)];
        for (int index = 0; index < fluids.length; index++) {
            fluids[index] = fluid("fluid_" + index);
        }
        return fluids;
    }

    /**
     * @param slots slot count
     * @return a handler granting one default item stack of room per slot
     */
    public static BigItemHandler handler(int slots) {
        return new BigItemHandler(slots);
    }

    /**
     * Builds an item handler whose slots hold far more than an int can express.
     *
     * @param slots      slot count
     * @param multiplier capacity multiplier
     * @return a handler with scaled capacity
     */
    public static BigItemHandler largeHandler(int slots, double multiplier) {
        return new BigItemHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    /**
     * @param slots slot count
     * @return a fluid handler granting one default tank per slot
     */
    public static BigFluidHandler fluidHandler(int slots) {
        return new BigFluidHandler(slots);
    }

    /**
     * Builds a fluid handler whose tanks hold far more than an int can express.
     *
     * @param slots      tank count
     * @param multiplier capacity multiplier
     * @return a handler with scaled capacity
     */
    public static BigFluidHandler largeFluidHandler(int slots, double multiplier) {
        return new BigFluidHandler(slots) {

            @Override
            public double getMultiplier() {
                return multiplier;
            }
        };
    }

    /**
     * Builds a multiplier that lifts one slot's capacity past the int boundary,
     * so a test exercises the long-to-int saturation every Forge caller hits.
     *
     * @param baseCapacity capacity one slot grants before scaling
     * @param factor       multiple of the int boundary the result must exceed
     * @return a multiplier exceeding the int boundary
     */
    public static double intOverflowingMultiplier(long baseCapacity, double factor) {
        double boundary = (double) Integer.MAX_VALUE + 1D;
        return boundary / Math.max(1L, baseCapacity) * Math.max(1D, factor);
    }

    /**
     * @param item item to measure
     * @return the room one slot grants that item
     */
    public static long capacityOf(Item item) {
        return ItemStorageResource.INSTANCE.capacityForStack(one(item));
    }

    /**
     * @return the room one tank grants any fluid
     */
    public static long fluidCapacity() {
        return FluidStorageResource.INSTANCE.defaultCapacity();
    }

    /**
     * Sums the stored amount of one item across every index.
     *
     * @param handler handler to read
     * @param item    item to total
     * @return the summed stored amount
     */
    public static long total(BigItemHandler handler, Item item) {
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

    /**
     * Sums the stored amount of one fluid across every tank.
     *
     * @param handler handler to read
     * @param fluid   fluid to total
     * @return the summed stored amount
     */
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

    /**
     * Sums every stored amount in a fluid handler.
     *
     * @param handler handler to read
     * @return the grand total
     */
    public static long fluidGrandTotal(IBigFluidHandler handler) {
        long total = 0L;
        for (int index = 0; index < handler.getStorageCount(); index++) {
            total += handler.getSnapshot(index)
                .getAmount();
        }
        return total;
    }
}
