package com.hfstudio.functionalstorage.common.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * Fluid resource kind. Fluid identity is the fluid plus its stack NBT, which
 * matches Forge's own equality semantics.
 */
public class FluidStorageResource implements StorageResource<BigFluidStack, FluidStorageKey> {

    public static final FluidStorageResource INSTANCE = new FluidStorageResource();

    @Override
    public String getId() {
        return "fluid";
    }

    @Nonnull
    @Override
    public BigFluidStack empty() {
        return BigFluidStack.empty();
    }

    @Override
    public boolean hasTemplate(@Nonnull BigFluidStack snapshot) {
        return snapshot.hasTemplate();
    }

    @Nonnull
    @Override
    public BigFluidStack templateOf(@Nonnull BigFluidStack snapshot) {
        return snapshot.withAmount(1L);
    }

    @Override
    public boolean matches(@Nonnull BigFluidStack left, @Nonnull BigFluidStack right) {
        return left.hasTemplate() && left.isSameType(right);
    }

    @Override
    public boolean accepts(@Nonnull BigFluidStack template, @Nonnull BigFluidStack candidate) {
        return matches(template, candidate);
    }

    @Override
    public long capacityFor(@Nonnull BigFluidStack template) {
        return defaultCapacity();
    }

    @Override
    public long defaultCapacity() {
        return Math.max(0L, FunctionalStorageConfig.STORAGE.baseFluidCapacity);
    }

    @Override
    public int upgradeDivisor() {
        return Math.max(1, FunctionalStorageConfig.STORAGE.fluidDivisor);
    }

    @Nullable
    @Override
    public NBTTagCompound writeTemplate(@Nonnull BigFluidStack snapshot) {
        FluidStack stack = snapshot.getTemplate();
        if (stack == null) {
            return null;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("FluidName", FluidRegistry.getFluidName(stack.getFluid()));
        if (stack.tag != null) {
            tag.setTag("Tag", stack.tag.copy());
        }
        return tag;
    }

    @Nonnull
    @Override
    public BigFluidStack readSnapshot(@Nullable NBTTagCompound tag, long amount) {
        if (tag == null || !tag.hasKey("FluidName")) {
            return BigFluidStack.empty();
        }
        String name = tag.getString("FluidName");
        if (name == null || name.isEmpty() || FluidRegistry.getFluid(name) == null) {
            return BigFluidStack.empty();
        }
        FluidStack stack = new FluidStack(FluidRegistry.getFluid(name), 1);
        if (tag.hasKey("Tag")) {
            NBTTagCompound stored = tag.getCompoundTag("Tag");
            stack.tag = stored == null ? null : (NBTTagCompound) stored.copy();
        }
        return new BigFluidStack(stack, amount);
    }
}
