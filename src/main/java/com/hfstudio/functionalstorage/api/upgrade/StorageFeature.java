package com.hfstudio.functionalstorage.api.upgrade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

@Getter
public class StorageFeature {

    private static final Map<String, StorageFeature> FEATURES = new LinkedHashMap<>();

    public static final StorageFeature MAX_CAPACITY = register("max_capacity");
    public static final StorageFeature CREATIVE = register("creative");
    public static final StorageFeature VOID_OVERFLOW = register("void_overflow");
    public static final StorageFeature EQUIVALENT_ITEMS = register("equivalent_items");
    public static final StorageFeature REDSTONE_OUTPUT = register("redstone_output");
    public static final StorageFeature IRON_DOWNGRADE = register("iron_downgrade");

    private final String id;

    private StorageFeature(String id) {
        this.id = id;
    }

    public static synchronized StorageFeature register(String id) {
        return FEATURES.computeIfAbsent(Objects.requireNonNull(id, "id"), StorageFeature::new);
    }

    public static synchronized StorageFeature[] values() {
        return FEATURES.values()
            .toArray(new StorageFeature[0]);
    }

}
