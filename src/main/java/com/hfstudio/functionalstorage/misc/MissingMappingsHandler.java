package com.hfstudio.functionalstorage.misc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.registry.GameRegistry;

public class MissingMappingsHandler {

    private static final Map<String, Item> ITEM_REMAPPINGS = new LinkedHashMap<>();
    private static final Map<String, Block> BLOCK_REMAPPINGS = new LinkedHashMap<>();

    public static void registerLegacyItems() {
        registerTieredItems("water_generation_upgrade", RegistrationHandler.waterGenerationUpgrades, true);
        registerTieredItems("stone_generation_upgrade", RegistrationHandler.stoneGenerationUpgrades, true);
        registerTieredItems("universal_item_generation", RegistrationHandler.universalGenerationUpgrades, true);
        registerTieredItems(
            "obsidian_upgrade",
            RegistrationHandler.getItemGenerationUpgradeGroups()
                .get("obsidian"),
            true);
        registerTieredItems(
            "dripping_upgrade",
            RegistrationHandler.getFluidGenerationUpgradeGroups()
                .get("lava"),
            true);
        registerItem("ore_dictionary_upgrade", RegistrationHandler.blankUpgrade);

        if (FunctionalStorageConfig.ADAPTIVE.enabled) {
            registerLegacyBlock("adaptive_controller", RegistrationHandler.adaptiveMatrix);
            registerItem("adaptive_controller", Item.getItemFromBlock(RegistrationHandler.adaptiveMatrix));
            registerLegacyBlock("adaptive_spruce_drawer", RegistrationHandler.adaptiveDrawer);
            registerItem("adaptive_spruce_drawer", Item.getItemFromBlock(RegistrationHandler.adaptiveDrawer));
            registerLegacyBlock("adaptive_fluid_drawer", RegistrationHandler.adaptiveFluidDrawers.get(0));
            registerItem(
                "adaptive_fluid_drawer",
                Item.getItemFromBlock(RegistrationHandler.adaptiveFluidDrawers.get(0)));
            if (!RegistrationHandler.adaptiveEssentiaDrawers.isEmpty()) {
                registerLegacyBlock("adaptive_essentia_drawer", RegistrationHandler.adaptiveEssentiaDrawers.get(0));
                registerItem(
                    "adaptive_essentia_drawer",
                    Item.getItemFromBlock(RegistrationHandler.adaptiveEssentiaDrawers.get(0)));
            }
        }
        registerLegacyBlock("adaptive_armory", RegistrationHandler.armoryCabinet);
        registerLegacyBlock("adaptive_extender", RegistrationHandler.controllerExtension);
        registerLegacyBlock("adaptive_range_extender", RegistrationHandler.controllerExtension);

        registerItem("adaptive_armory", Item.getItemFromBlock(RegistrationHandler.armoryCabinet));
        registerItem("adaptive_extender", Item.getItemFromBlock(RegistrationHandler.controllerExtension));
        registerItem("adaptive_range_extender", Item.getItemFromBlock(RegistrationHandler.controllerExtension));
    }

    public static void registerItem(String legacyId, Item replacement) {
        Objects.requireNonNull(legacyId, "legacyId");
        Objects.requireNonNull(replacement, "replacement");
        String name = FunctionalStorage.MOD_ID + ":" + legacyId;
        Item previous = ITEM_REMAPPINGS.putIfAbsent(name, replacement);
        if (previous != null && previous != replacement) {
            throw new IllegalArgumentException("Conflicting item replacement for " + name);
        }
    }

    private static void registerLegacyBlock(String legacyId, Block replacement) {
        Objects.requireNonNull(legacyId, "legacyId");
        Objects.requireNonNull(replacement, "replacement");
        String name = FunctionalStorage.MOD_ID + ":" + legacyId;
        Block previous = BLOCK_REMAPPINGS.putIfAbsent(name, replacement);
        if (previous != null && previous != replacement) {
            throw new IllegalArgumentException("Conflicting block replacement for " + name);
        }
    }

    public static void registerTieredItems(String legacyPrefix, List<? extends Item> replacements,
        boolean unnumberedFirstTier) {
        Objects.requireNonNull(legacyPrefix, "legacyPrefix");
        Objects.requireNonNull(replacements, "replacements");
        if (replacements.isEmpty()) {
            throw new IllegalStateException("Cannot register legacy mappings before items for " + legacyPrefix);
        }
        for (int index = 0; index < replacements.size(); index++) {
            Item replacement = replacements.get(index);
            registerItem(legacyPrefix + "_t" + (index + 1), replacement);
            registerItem(legacyPrefix + "_" + (index + 1), replacement);
            if (index == 0 && unnumberedFirstTier) registerItem(legacyPrefix, replacement);
        }
    }

    public static boolean remap(MissingMapping mapping) {
        if (mapping.type == GameRegistry.Type.ITEM) {
            Item replacement = ITEM_REMAPPINGS.get(mapping.name);
            if (replacement == null) return false;
            mapping.remap(replacement);
            FunctionalStorage.LOG
                .info("Remapped legacy item {} to {}", mapping.name, Item.itemRegistry.getNameForObject(replacement));
            return true;
        }
        if (mapping.type == GameRegistry.Type.BLOCK) {
            Block replacement = BLOCK_REMAPPINGS.get(mapping.name);
            if (replacement == null) return false;
            mapping.remap(replacement);
            FunctionalStorage.LOG.info(
                "Remapped legacy block {} to {}",
                mapping.name,
                Block.blockRegistry.getNameForObject(replacement));
            return true;
        }
        return false;
    }
}
