package com.hfstudio.functionalstorage.common.options;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import com.hfstudio.functionalstorage.common.item.ConfigurationToolItem;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/** Display options persisted with the tile and synchronized to clients. */
public class DrawerOptions {

    private static final String ADVANCED_PREFIX = "Advanced_";

    private final Map<ConfigurationToolItem.ConfigurationAction, Boolean> toggles;
    private final Map<ConfigurationToolItem.ConfigurationAction, Integer> advancedValues;

    public DrawerOptions() {
        this.toggles = new EnumMap<>(ConfigurationToolItem.ConfigurationAction.class);
        this.advancedValues = new EnumMap<>(ConfigurationToolItem.ConfigurationAction.class);
        this.toggles.put(
            ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS,
            FunctionalStorageConfig.CLIENT.defaultShowItemCount);
        this.toggles.put(
            ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER,
            FunctionalStorageConfig.CLIENT.defaultShowItemRender);
        this.toggles.put(
            ConfigurationToolItem.ConfigurationAction.TOGGLE_UPGRADES,
            FunctionalStorageConfig.CLIENT.defaultShowUpgrades);
        this.advancedValues.put(ConfigurationToolItem.ConfigurationAction.INDICATOR, 0);
    }

    public boolean isActive(ConfigurationToolItem.ConfigurationAction action) {
        Boolean value = toggles.get(action);
        return value == null || value;
    }

    public boolean isShowItemRender() {
        return isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER);
    }

    public boolean isShowItemCount() {
        return isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS);
    }

    public boolean isShowUpgrades() {
        return isActive(ConfigurationToolItem.ConfigurationAction.TOGGLE_UPGRADES);
    }

    public int getAdvancedValue(ConfigurationToolItem.ConfigurationAction action) {
        Integer value = advancedValues.get(action);
        return value == null ? 0 : value;
    }

    public void setActive(ConfigurationToolItem.ConfigurationAction action, boolean active) {
        toggles.put(action, active);
    }

    public void setAdvancedValue(ConfigurationToolItem.ConfigurationAction action, int value) {
        advancedValues.put(action, value);
    }

    public void cycle(ConfigurationToolItem.ConfigurationAction action) {
        if (action.getMaxValue() == 1) {
            setActive(action, !isActive(action));
        } else {
            setAdvancedValue(action, (getAdvancedValue(action) + 1) % (action.getMaxValue() + 1));
        }
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<ConfigurationToolItem.ConfigurationAction, Boolean> entry : toggles.entrySet()) {
            tag.setBoolean(
                entry.getKey()
                    .name(),
                entry.getValue());
        }
        for (Map.Entry<ConfigurationToolItem.ConfigurationAction, Integer> entry : advancedValues.entrySet()) {
            tag.setInteger(
                ADVANCED_PREFIX + entry.getKey()
                    .name(),
                entry.getValue());
        }
        return tag;
    }

    /**
     * Restores options from persisted data, ignoring unknown action names so a
     * downgrade cannot corrupt the drawer.
     *
     * @param tag previously produced by {@link #serializeNBT()}
     */
    public void deserializeNBT(NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        for (Object keyObject : tag.func_150296_c()) {
            if (!(keyObject instanceof String key)) {
                continue;
            }
            boolean advanced = key.startsWith(ADVANCED_PREFIX);
            String actionName = advanced ? key.substring(ADVANCED_PREFIX.length()) : key;
            ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.ConfigurationAction
                .byName(actionName);
            if (action == null) {
                continue;
            }
            if (advanced) {
                advancedValues.put(action, tag.getInteger(key));
            } else {
                toggles.put(action, tag.getBoolean(key));
            }
        }
    }
}
