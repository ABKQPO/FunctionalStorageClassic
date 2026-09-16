package com.hfstudio.functionalstorage.common.item;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.misc.RegistrationHandler;

/**
 * Tool that toggles a drawer's rendering options in place. Sneaking applies the
 * change to every drawer linked to the clicked drawer's controller.
 */
public class ConfigurationToolItem extends Item {

    /**
     * Options the configuration tool can cycle.
     */
    public enum ConfigurationAction {

        TOGGLE_NUMBERS("numbers", 1),
        TOGGLE_RENDER("render", 1),
        TOGGLE_UPGRADES("upgrades", 1),
        INDICATOR("indicator", 3);

        private static final Map<String, ConfigurationAction> BY_NAME = new HashMap<>();

        static {
            for (ConfigurationAction action : values()) {
                BY_NAME.put(action.name(), action);
            }
        }

        private final String id;
        private final int maxValue;

        ConfigurationAction(String id, int maxValue) {
            this.id = id;
            this.maxValue = maxValue;
        }

        /**
         * Resolves an action by its enum name, tolerating unknown values.
         *
         * @param name serialized action name
         * @return the matching action, or {@code null}
         */
        @Nullable
        public static ConfigurationAction byName(@Nullable String name) {
            return name == null ? null : BY_NAME.get(name);
        }

        /**
         * @return the translation key suffix of this action
         */
        public String getId() {
            return id;
        }

        /**
         * @return number of distinct values this action cycles through minus one
         */
        public int getMaxValue() {
            return maxValue;
        }

        /**
         * @return the localized action name
         */
        public String getLocalizedName() {
            return StatCollector.translateToLocal("functionalstorage.configuration." + id);
        }
    }

    public ConfigurationToolItem() {
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName("functionalstorage.configuration_tool");
        setTextureName("functionalstorage:configuration_tool");
    }

    /**
     * Applies the next value of every configuration action to the clicked
     * drawer, or to all linked drawers while sneaking.
     *
     * @param stack  tool stack
     * @param player interacting player
     * @param world  world being modified
     * @param x      block x
     * @param y      block y
     * @param z      block z
     * @return whether the interaction was consumed
     */
    public boolean applyToDrawer(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, @Nonnull World world, int x,
        int y, int z) {
        return false;
    }
}
