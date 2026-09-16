package com.hfstudio.functionalstorage.common.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import thaumcraft.api.aspects.Aspect;

/**
 * Thaumcraft essentia resource kind. Essentia has no NBT component, so the
 * aspect is the complete identity and persistence only stores its tag.
 */
public class AspectStorageResource implements StorageResource<BigAspectStack, AspectStorageKey> {

    public static final AspectStorageResource INSTANCE = new AspectStorageResource();

    @Override
    public String getId() {
        return "aspect";
    }

    @Nonnull
    @Override
    public BigAspectStack empty() {
        return BigAspectStack.empty();
    }

    @Override
    public boolean hasTemplate(@Nonnull BigAspectStack snapshot) {
        return snapshot.hasTemplate();
    }

    @Nonnull
    @Override
    public BigAspectStack templateOf(@Nonnull BigAspectStack snapshot) {
        return snapshot.withAmount(1L);
    }

    @Override
    public boolean matches(@Nonnull BigAspectStack left, @Nonnull BigAspectStack right) {
        return left.hasTemplate() && left.isSameType(right.getAspect());
    }

    @Override
    public boolean accepts(@Nonnull BigAspectStack template, @Nonnull BigAspectStack candidate) {
        return template.getAspect() != null && template.isSameType(candidate.getAspect());
    }

    @Override
    public long capacityFor(@Nonnull BigAspectStack template) {
        return defaultCapacity();
    }

    @Override
    public long defaultCapacity() {
        return Math.max(0L, FunctionalStorageConfig.STORAGE.baseAspectCapacity);
    }

    @Override
    public int upgradeDivisor() {
        return Math.max(1, FunctionalStorageConfig.STORAGE.aspectDivisor);
    }

    @Nullable
    @Override
    public NBTTagCompound writeTemplate(@Nonnull BigAspectStack snapshot) {
        Aspect aspect = snapshot.getAspect();
        if (aspect == null) {
            return null;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Aspect", aspect.getTag());
        return tag;
    }

    @Nonnull
    @Override
    public BigAspectStack readSnapshot(@Nullable NBTTagCompound tag, long amount) {
        if (tag == null || !tag.hasKey("Aspect")) {
            return BigAspectStack.empty();
        }
        Aspect aspect = Aspect.getAspect(tag.getString("Aspect"));
        return aspect == null ? BigAspectStack.empty() : new BigAspectStack(aspect, amount);
    }
}
