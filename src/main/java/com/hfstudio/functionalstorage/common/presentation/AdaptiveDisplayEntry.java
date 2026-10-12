package com.hfstudio.functionalstorage.common.presentation;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageResource;
import com.hfstudio.functionalstorage.api.storage.StorageSnapshot;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.storage.AspectStorageResource;
import com.hfstudio.functionalstorage.common.storage.FluidStorageResource;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;

import cpw.mods.fml.common.Optional;

public record AdaptiveDisplayEntry(@Nonnull StorageSnapshot<?, ?> content, long capacity) {

    public AdaptiveDisplayEntry {
        content = Objects.requireNonNull(content, "content");
        capacity = Math.max(0L, capacity);
    }

    public NBTTagCompound serializeNBT() {
        if (content instanceof BigItemStack item) return write(ItemStorageResource.INSTANCE, item, capacity);
        if (content instanceof BigFluidStack fluid) return write(FluidStorageResource.INSTANCE, fluid, capacity);
        if (Mods.Thaumcraft.isModLoaded()) return writeAspect();
        throw new IllegalArgumentException("Unsupported adaptive display snapshot: " + content.getClass());
    }

    public static AdaptiveDisplayEntry fromNBT(NBTTagCompound tag) {
        String resource = tag.getString("Resource");
        NBTTagCompound template = tag.getCompoundTag("Template");
        long amount = Math.max(0L, tag.getLong("Amount"));
        StorageSnapshot<?, ?> snapshot;
        if (resource.isEmpty() || resource.equals(ItemStorageResource.INSTANCE.getId())) {
            snapshot = ItemStorageResource.INSTANCE.readSnapshot(template, amount);
        } else if (resource.equals(FluidStorageResource.INSTANCE.getId())) {
            snapshot = FluidStorageResource.INSTANCE.readSnapshot(template, amount);
        } else if (resource.equals("aspect") && Mods.Thaumcraft.isModLoaded()) {
            snapshot = readAspect(template, amount);
        } else {
            snapshot = BigItemStack.empty();
        }
        return new AdaptiveDisplayEntry(snapshot, tag.getLong("Capacity"));
    }

    private static <S extends StorageSnapshot<S, K>, K extends StorageKey> NBTTagCompound write(
        StorageResource<S, K> resource, S snapshot, long capacity) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Resource", resource.getId());
        NBTTagCompound template = resource.writeTemplate(snapshot);
        if (template != null) tag.setTag("Template", template);
        tag.setLong("Amount", snapshot.getAmount());
        tag.setLong("Capacity", capacity);
        return tag;
    }

    @Optional.Method(modid = "Thaumcraft")
    private NBTTagCompound writeAspect() {
        if (content instanceof BigAspectStack aspect) return write(AspectStorageResource.INSTANCE, aspect, capacity);
        throw new IllegalArgumentException("Unsupported adaptive display snapshot: " + content.getClass());
    }

    @Optional.Method(modid = "Thaumcraft")
    private static StorageSnapshot<?, ?> readAspect(NBTTagCompound template, long amount) {
        return AspectStorageResource.INSTANCE.readSnapshot(template, amount);
    }
}
