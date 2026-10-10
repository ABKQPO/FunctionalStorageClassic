package com.hfstudio.functionalstorage.api.upgrade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

@Getter
public class UpgradeAttribute {

    private static final Map<String, UpgradeAttribute> ATTRIBUTES = new LinkedHashMap<>();

    public static final UpgradeAttribute ITEM_CAPACITY = register("item_capacity", "storageupgrade.obj.item_storage");
    public static final UpgradeAttribute FLUID_CAPACITY = register(
        "fluid_capacity",
        "storageupgrade.obj.fluid_storage");
    public static final UpgradeAttribute ASPECT_CAPACITY = register(
        "aspect_capacity",
        "storageupgrade.obj.aspect_storage");
    public static final UpgradeAttribute CONTROLLER_RANGE = register(
        "controller_range",
        "storageupgrade.obj.controller_range");

    private final String id;
    private final String translationKey;

    private UpgradeAttribute(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public static synchronized UpgradeAttribute register(String id, String translationKey) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        UpgradeAttribute existing = ATTRIBUTES.get(id);
        if (existing != null) {
            if (!existing.translationKey.equals(translationKey)) {
                throw new IllegalArgumentException(
                    "Attribute translation differs from its registered definition: " + id);
            }
            return existing;
        }
        UpgradeAttribute attribute = new UpgradeAttribute(id, translationKey);
        ATTRIBUTES.put(id, attribute);
        return attribute;
    }

    public static synchronized UpgradeAttribute[] values() {
        return ATTRIBUTES.values()
            .toArray(new UpgradeAttribute[0]);
    }

}
