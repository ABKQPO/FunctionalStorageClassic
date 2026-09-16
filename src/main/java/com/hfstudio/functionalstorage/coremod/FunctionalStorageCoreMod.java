package com.hfstudio.functionalstorage.coremod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.mixins.Mixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

/**
 * Core mod. Its only job is registering the GTNHLib configuration before any
 * mixin applies and exposing the early mixin set.
 */
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class FunctionalStorageCoreMod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static {
        try {
            FunctionalStorageConfig.registerConfig();
        } catch (ConfigException exception) {
            throw new IllegalStateException("Unable to register the Functional Storage config", exception);
        }
    }

    public FunctionalStorageCoreMod() {}

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.functionalstorage.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
