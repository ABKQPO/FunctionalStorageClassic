package com.hfstudio.functionalstorage.common.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.options.DrawerOptions;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/** Cycles display options and toggles locks through the held configuration tool. */
public class ConfigurationToolItem extends Item {

    @Getter
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

        public String getLocalizedName() {
            return StatCollector.translateToLocal("functionalstorage.configuration." + id);
        }
    }

    private static final String KEY_CYCLE = "CycleIndex";
    private static final ConfigurationAction[] CYCLE_ORDER = { ConfigurationAction.TOGGLE_NUMBERS,
        ConfigurationAction.TOGGLE_RENDER, ConfigurationAction.TOGGLE_UPGRADES, ConfigurationAction.INDICATOR };

    public ConfigurationToolItem() {
        setMaxStackSize(1);
        setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        setUnlocalizedName("functionalstorage.configuration_tool");
        setTextureName("functionalstorage:configuration_tool");
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return false;
        }

        if (player.isSneaking()) {
            drawer.toggleLocking();
            player.addChatMessage(
                new ChatComponentTranslation(
                    drawer.isLocked() ? "functionalstorage.drawer.locked" : "functionalstorage.drawer.unlocked"));
            return true;
        }

        DrawerOptions options = drawer.getDrawerOptions();
        ConfigurationAction action = nextAction(stack);
        options.cycle(action);
        drawer.markOptionsDirty();
        player.addChatMessage(
            new ChatComponentTranslation(
                "functionalstorage.configuration_tool.cycled",
                action.getLocalizedName(),
                describe(options, action)));
        return true;
    }

    @Nonnull
    private ConfigurationAction nextAction(@Nonnull ItemStack stack) {
        int index = getCycleIndex(stack) % CYCLE_ORDER.length;
        setCycleIndex(stack, (index + 1) % CYCLE_ORDER.length);
        return CYCLE_ORDER[index];
    }

    private int getCycleIndex(@Nonnull ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || !tag.hasKey(KEY_CYCLE) ? 0 : Math.max(0, tag.getInteger(KEY_CYCLE));
    }

    private void setCycleIndex(@Nonnull ItemStack stack, int index) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setInteger(KEY_CYCLE, index);
    }

    @Nonnull
    private String describe(@Nonnull DrawerOptions options, @Nonnull ConfigurationAction action) {
        if (action.getMaxValue() == 1) {
            return StatCollector.translateToLocal(
                options.isActive(action) ? "functionalstorage.configuration.state.on"
                    : "functionalstorage.configuration.state.off");
        }
        return Integer.toString(options.getAdvancedValue(action));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("functionalstorage.configuration_tool.tooltip"));
    }
}
