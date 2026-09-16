package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import net.minecraft.item.Item;

import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry.ContainerDefinition;

import cpw.mods.fml.common.registry.GameRegistry;

/** Connects registered Thaumcraft content through its public API and registry names. */
public class ThaumcraftIntegration {

    private ThaumcraftIntegration() {}

    public static void register() {
        Item phial = GameRegistry.findItem("Thaumcraft", "ItemEssence");
        if (phial != null) {
            EssentiaContainerRegistry.register(phial, new ContainerDefinition(8, 0, 1));
        }
    }
}
