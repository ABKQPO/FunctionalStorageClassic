package com.hfstudio.functionalstorage.support;

import java.lang.reflect.Field;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.objenesis.ObjenesisStd;

import cpw.mods.fml.common.registry.RegistryDelegate;

/**
 * A fluid stack that behaves like the real one outside a running game. Forge's
 * constructor refuses any fluid not in its registry, and that registry cannot even
 * initialize here because it reads vanilla blocks. The base class is not final and its
 * {@code copy} is overridable, so an instance is allocated without a constructor, wired
 * to a registry delegate, and taught to copy itself; identity and amount stay exactly
 * Forge's semantics, which is all the storage core asks of a fluid.
 *
 * <p>
 * Instances are meant to be reused: a caller mutates {@code amount} between operations
 * instead of allocating, so a harness that allocates per interaction would measure its
 * own allocation rather than the code under test.
 * </p>
 */
public class TestFluidStack extends FluidStack {

    private static final ObjenesisStd OBJENESIS = new ObjenesisStd();
    private static final Field DELEGATE = delegateField();

    // Never executed: every instance is allocated without a constructor. The cast
    // resolves the overloads, both of which accept a reference as their first
    // argument.
    private TestFluidStack() {
        super((Fluid) null, 0);
    }

    /**
     * Creates a reusable stack for one fluid.
     */
    public static TestFluidStack of(Fluid fluid, int amount) {
        TestFluidStack stack = OBJENESIS.newInstance(TestFluidStack.class);
        attach(stack, fluid);
        stack.amount = amount;
        return stack;
    }

    @Override
    public FluidStack copy() {
        TestFluidStack copy = OBJENESIS.newInstance(TestFluidStack.class);
        attach(copy, getFluid());
        copy.amount = amount;
        NBTTagCompound source = tag;
        copy.tag = source == null ? null : (NBTTagCompound) source.copy();
        return copy;
    }

    private static void attach(FluidStack stack, Fluid fluid) {
        try {
            DELEGATE.set(stack, new RegistryDelegate.Delegate<>(fluid, Fluid.class));
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("cannot attach a fluid delegate", failure);
        }
    }

    private static Field delegateField() {
        try {
            Field field = FluidStack.class.getDeclaredField("fluidDelegate");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("FluidStack no longer has a fluid delegate", failure);
        }
    }
}
