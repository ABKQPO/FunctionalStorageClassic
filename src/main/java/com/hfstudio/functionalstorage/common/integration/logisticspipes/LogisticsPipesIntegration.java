package com.hfstudio.functionalstorage.common.integration.logisticspipes;

import com.hfstudio.functionalstorage.FunctionalStorage;

import cpw.mods.fml.common.Optional;
import logisticspipes.proxy.SimpleServiceLocator;

public class LogisticsPipesIntegration {

    private LogisticsPipesIntegration() {}

    @Optional.Method(modid = "LogisticsPipes")
    public static void register() {
        SimpleServiceLocator.inventoryUtilFactory.registerHandler(new LogisticsPipesInventoryHandler());
        FunctionalStorage.LOG.info("Registered the LogisticsPipes drawer storage bridge");
    }
}
