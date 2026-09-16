package com.hfstudio.functionalstorage.common.inventory.base;

import java.util.Arrays;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.IBigFluidHandler;
import com.hfstudio.functionalstorage.common.storage.FluidStorageResource;

/**
 * Concrete fluid storage over the generic core. Each tank carries an explicit
 * fill and drain permission so a read-only tank can be modelled without a
 * subclass per combination.
 */
public class BigFluidHandler extends AbstractStorageHandler<BigFluidStack, FluidStorageKey>
    implements IBigFluidHandler {

    private final boolean[] fillable;
    private final boolean[] drainable;

    public BigFluidHandler(int slots) {
        this(slots, true, true);
    }

    public BigFluidHandler(int slots, boolean fillable, boolean drainable) {
        super(FluidStorageResource.INSTANCE, slots);
        int count = Math.max(0, slots);
        this.fillable = new boolean[count];
        this.drainable = new boolean[count];
        Arrays.fill(this.fillable, fillable);
        Arrays.fill(this.drainable, drainable);
    }

    @Override
    public boolean supportsFill(int index) {
        return index >= 0 && index < fillable.length && fillable[index];
    }

    @Override
    public boolean supportsDrain(int index) {
        return index >= 0 && index < drainable.length && drainable[index];
    }

    @Override
    protected boolean isCompatible(@Nonnull BigFluidStack template, @Nonnull BigFluidStack candidate) {
        return template.isSameType(candidate);
    }

    public void setAccess(int index, boolean fillable, boolean drainable) {
        if (index < 0 || index >= this.fillable.length) {
            return;
        }
        this.fillable[index] = fillable;
        this.drainable[index] = drainable;
    }
}
