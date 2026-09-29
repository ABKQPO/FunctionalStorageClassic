package com.hfstudio.functionalstorage.common.inventory.adapter;

import java.util.BitSet;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;

/** Adapts a long-capacity drawer to transfers that report the actual rejected amount. */
public class DrawerItemSink implements ItemSink {

    private final Supplier<IBigItemHandler> handlerSupplier;
    private BitSet allowedSlots;
    private int slotStackLimit = Integer.MAX_VALUE;

    public DrawerItemSink(@Nonnull Supplier<IBigItemHandler> handlerSupplier) {
        this.handlerSupplier = handlerSupplier;
    }

    @Override
    public void resetSink() {
        allowedSlots = null;
        slotStackLimit = Integer.MAX_VALUE;
    }

    @Override
    public void setAllowedSinkSlots(int[] slots) {
        if (slots == null) {
            allowedSlots = null;
            return;
        }
        BitSet allowed = new BitSet();
        IBigItemHandler handler = handlerSupplier.get();
        int count = handler == null ? 0 : handler.getStorageCount();
        for (int slot : slots) {
            if (slot >= 0 && slot < count) {
                allowed.set(slot);
            }
        }
        allowedSlots = allowed;
    }

    @Override
    public void setSlotStackLimit(int limit) {
        slotStackLimit = Math.max(0, limit);
    }

    @Override
    public int store(ImmutableItemStack stack) {
        int requested = stack.getStackSize();
        if (requested <= 0 || stack.getItem() == null) {
            return Math.max(0, requested);
        }
        IBigItemHandler handler = handlerSupplier.get();
        if (handler == null || slotStackLimit == 0) {
            return requested;
        }
        BigItemStack input = new BigItemStack(stack.toStackFast(), requested);
        if (allowedSlots == null && slotStackLimit == Integer.MAX_VALUE) {
            return (int) handler.insertRouted(input, StorageAction.EXECUTE)
                .getRemainingAmount();
        }

        int remaining = requested;
        int count = handler.getStorageCount();
        int passes = handler.allowsEquivalentResources() ? 3 : 2;
        for (int pass = 0; pass < passes && remaining > 0; pass++) {
            for (int index = 0; index < count && remaining > 0; index++) {
                if (allowedSlots != null && !allowedSlots.get(index)) {
                    continue;
                }
                BigItemStack current = handler.getSnapshot(index);
                boolean hasTemplate = current.hasTemplate();
                boolean exact = hasTemplate && current.isSameType(input);
                if (pass == 0 && !exact || pass == passes - 1 && hasTemplate) {
                    continue;
                }
                if (passes == 3 && pass == 1) {
                    if (!hasTemplate || exact
                        || handler.insert(index, input.withAmount(1L), StorageAction.SIMULATE)
                            .getProcessedAmount() == 0L) {
                        continue;
                    }
                }
                long room = Math.max(0L, (long) slotStackLimit - current.getAmount());
                if (room == 0L) {
                    continue;
                }
                long accepted = handler
                    .insert(index, input.withAmount(Math.min(remaining, room)), StorageAction.EXECUTE)
                    .getProcessedAmount();
                remaining -= (int) Math.min(remaining, accepted);
            }
        }
        return remaining;
    }

}
