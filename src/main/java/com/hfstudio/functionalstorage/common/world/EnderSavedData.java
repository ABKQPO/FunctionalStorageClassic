package com.hfstudio.functionalstorage.common.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;

/**
 * Per-save shared storage for ender drawers. Every ender drawer using the same
 * frequency reads and writes the same handler, so contents survive breaking and
 * are shared across dimensions.
 */
public class EnderSavedData extends WorldSavedData {

    public static final String DATA_NAME = "functionalstorage_ender";

    private final Map<UUID, BigItemHandler> frequencies = new HashMap<>();

    public EnderSavedData() {
        super(DATA_NAME);
    }

    public EnderSavedData(String name) {
        super(name);
    }

    /**
     * Returns the shared handler for a frequency, creating it on first use.
     *
     * @param frequency drawer frequency
     * @param slots     slot count used when the handler is created
     * @return the shared handler
     */
    @Nonnull
    public BigItemHandler handlerFor(@Nonnull UUID frequency, int slots) {
        BigItemHandler handler = frequencies.get(frequency);
        if (handler == null) {
            handler = new BigItemHandler(Math.max(1, slots));
            frequencies.put(frequency, handler);
            markDirty();
        }
        return handler;
    }

    /**
     * @param frequency drawer frequency
     * @return the shared handler, or {@code null} when the frequency is unused
     */
    @Nullable
    public BigItemHandler peek(@Nonnull UUID frequency) {
        return frequencies.get(frequency);
    }

    /**
     * Loads or creates the ender storage data for a world.
     *
     * @param world world to load data for
     * @return the shared save data
     */
    @Nonnull
    public static EnderSavedData dataFor(@Nonnull World world) {
        EnderSavedData data = (EnderSavedData) world.mapStorage.loadData(EnderSavedData.class, DATA_NAME);
        if (data == null) {
            data = new EnderSavedData();
            world.mapStorage.setData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        frequencies.clear();
        NBTTagCompound all = tag.getCompoundTag("Frequencies");
        for (Object keyObject : all.func_150296_c()) {
            if (!(keyObject instanceof String key)) {
                continue;
            }
            UUID frequency;
            try {
                frequency = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            NBTTagCompound entry = all.getCompoundTag(key);
            BigItemHandler handler = new BigItemHandler(Math.max(1, entry.getInteger("Slots")));
            handler.deserializeNBT(entry.hasKey("Storage", 10) ? entry.getCompoundTag("Storage") : null);
            frequencies.put(frequency, handler);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound all = new NBTTagCompound();
        for (Map.Entry<UUID, BigItemHandler> entry : frequencies.entrySet()) {
            NBTTagCompound stored = new NBTTagCompound();
            stored.setInteger(
                "Slots",
                entry.getValue()
                    .getStorageCount());
            stored.setTag(
                "Storage",
                entry.getValue()
                    .serializeNBT());
            all.setTag(
                entry.getKey()
                    .toString(),
                stored);
        }
        tag.setTag("Frequencies", all);
    }
}
