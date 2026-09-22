package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.item.FastImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSink;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Guards the cost of reading a whole drawer.
 *
 * <p>
 * A storage bus, an inventory scanner, or any caller looking for a matching slot
 * walks every slot in order. Each read commits whatever edits a previous caller
 * made, and if that commit re-examines every slot that was ever handed out, the
 * walk costs the square of the slot count. A drawer is allowed to hold hundreds of
 * slots, so that shape decides whether such a caller is usable at all.
 * </p>
 *
 * <p>
 * The check is a ratio rather than an absolute time, so it means the same thing on
 * any machine: the cost per slot on a large drawer must stay within a small factor
 * of the cost per slot on a small one. A quadratic walk makes that ratio grow with
 * the slot count, which is what the threshold below catches.
 * </p>
 */
public class SweepScalingTest {

    private static final ForgeDirection SIDE = ForgeDirection.UP;
    private static final int SMALL = 16;
    private static final int LARGE = 512;

    /** A linear walk keeps the ratio near one; a quadratic one exceeds three hundred. */
    private static final long MAX_PER_SLOT_GROWTH = 4L;

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("reading every slot stays linear in the slot count")
    void sweepCostDoesNotGrowWithSlotCount() {
        long smallPerSlot = perSlotNanos(SMALL);
        long largePerSlot = perSlotNanos(LARGE);

        System.out.println(
            "MEASURE sweep perSlotNs small=" + smallPerSlot
                + " (slots="
                + SMALL
                + ')'
                + " large="
                + largePerSlot
                + " (slots="
                + LARGE
                + ')'
                + " growth="
                + (smallPerSlot == 0L ? -1L : largePerSlot / smallPerSlot)
                + 'x');

        assertTrue(
            largePerSlot <= smallPerSlot * MAX_PER_SLOT_GROWTH,
            "per-slot read cost grew " + (smallPerSlot == 0L ? -1L : largePerSlot / smallPerSlot)
                + "x from "
                + SMALL
                + " to "
                + LARGE
                + " slots, so a full sweep has become quadratic");
    }

    private static long perSlotNanos(int slotCount) {
        BigItemHandler handler = StorageFixtures.handler(slotCount);
        IInventory view = new DrawerItemInventory(handler, "drawer", () -> {});
        InventoryItemSink sink = new InventoryItemSink(view, SIDE);

        for (int index = 0; index < slotCount; index++) {
            sink.store(new FastImmutableItemStack(StorageFixtures.stack(StorageFixtures.newItem(), 10)));
        }

        // Warm up so class loading and first-call costs are not measured.
        for (int index = 0; index < 200; index++) {
            sweep(view, slotCount);
        }

        int sweeps = 2_000;
        long best = Long.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            long start = System.nanoTime();
            for (int index = 0; index < sweeps; index++) {
                sweep(view, slotCount);
            }
            long perSweep = (System.nanoTime() - start) / sweeps;
            best = Math.min(best, perSweep);
        }
        return best / slotCount;
    }

    private static void sweep(IInventory view, int slotCount) {
        long found = 0L;
        for (int index = 0; index < slotCount; index++) {
            if (view.getStackInSlot(index) != null) {
                found++;
            }
        }
        if (found == Long.MIN_VALUE) {
            throw new IllegalStateException("unreachable, keeps the loop from being optimised away");
        }
    }
}
