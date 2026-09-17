package com.hfstudio.functionalstorage.api.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Registry of wood variants a wooden drawer can use. The mod's built-in woods
 * are added at startup; other mods may add theirs before registration runs.
 *
 * <p>
 * Adding a wood only makes it available for registration. Woods whose blocks
 * are missing are ignored when drawers are built, which keeps a partially
 * enabled pack working.
 * </p>
 */
public class WoodTypeRegistry {

    private static final Map<String, IWoodType> WOOD_TYPES = new LinkedHashMap<>();
    private static boolean frozen;

    /**
     * Adds a wood variant. Adding the same identifier twice replaces the earlier
     * entry, so a mod may override a built-in wood if it wants to.
     *
     * @param woodType wood to add
     */
    public static synchronized void add(@Nonnull IWoodType woodType) {
        WOOD_TYPES.put(woodType.getName(), woodType);
    }

    /**
     * Prevents further additions. Called once content registration has finished,
     * so a late addition cannot produce a drawer with no block behind it.
     */
    public static synchronized void freeze() {
        frozen = true;
    }

    /**
     * @return whether the registry no longer accepts additions
     */
    public static synchronized boolean isFrozen() {
        return frozen;
    }

    /**
     * @return every registered wood, in registration order
     */
    @Nonnull
    public static synchronized List<IWoodType> get() {
        return List.copyOf(WOOD_TYPES.values());
    }

    /**
     * @return every registered wood whose blocks are present
     */
    @Nonnull
    public static synchronized List<IWoodType> available() {
        List<IWoodType> woodTypes = new ArrayList<>();
        for (IWoodType woodType : WOOD_TYPES.values()) {
            if (woodType.isAvailable()) {
                woodTypes.add(woodType);
            }
        }
        return woodTypes;
    }

    /**
     * @param name stable wood identifier
     * @return the matching wood, or {@code null}
     */
    @Nullable
    public static synchronized IWoodType byName(@Nullable String name) {
        return name == null ? null : WOOD_TYPES.get(name);
    }
}
