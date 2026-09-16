package com.hfstudio.functionalstorage.misc;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

/**
 * In-game configuration screen backed by the GTNHLib config GUI.
 */
public class ConfigGuiConfig extends SimpleGuiConfig {

    /**
     * Creates the screen.
     *
     * @param parentScreen screen to return to
     * @throws ConfigException when the annotated config cannot be read
     */
    public ConfigGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, FunctionalStorage.MOD_ID, FunctionalStorage.MOD_NAME, true, FunctionalStorageConfig.class);
    }
}
