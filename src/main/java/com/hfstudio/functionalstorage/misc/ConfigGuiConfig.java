package com.hfstudio.functionalstorage.misc;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class ConfigGuiConfig extends SimpleGuiConfig {

    public ConfigGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, FunctionalStorage.MOD_ID, FunctionalStorage.MOD_NAME, true, FunctionalStorageConfig.class);
    }
}
