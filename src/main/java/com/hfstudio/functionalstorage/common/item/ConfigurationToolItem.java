package com.hfstudio.functionalstorage.common.item;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.interaction.ToolFeedback;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

public class ConfigurationToolItem extends LayeredToolItem {

    @Getter
    public enum ConfigurationAction {

        LOCKING("locking", 1, 0x2883FA, EnumChatFormatting.BLUE),
        TOGGLE_NUMBERS("numbers", 1, 0xFA9128, EnumChatFormatting.GOLD),
        TOGGLE_RENDER("renderItem", 1, 0x64FA28, EnumChatFormatting.GREEN),
        TOGGLE_UPGRADES("upgrades", 1, 0xA628FA, EnumChatFormatting.LIGHT_PURPLE),
        INDICATOR("indicator", 3, 0xFF2828, EnumChatFormatting.RED);

        private final String id;
        private final int maxValue;
        private final int color;
        private final EnumChatFormatting feedbackColor;

        ConfigurationAction(String id, int maxValue, int color, EnumChatFormatting feedbackColor) {
            this.id = id;
            this.maxValue = maxValue;
            this.color = color;
            this.feedbackColor = feedbackColor;
        }

        @Nullable
        public static ConfigurationAction byName(@Nullable String name) {
            for (ConfigurationAction action : values()) {
                if (action.name()
                    .equals(name)) {
                    return action;
                }
            }
            return null;
        }

        public String getLocalizedName() {
            return StatCollector.translateToLocal("configurationtool.configmode." + name().toLowerCase(Locale.ROOT));
        }
    }

    private static final String KEY_ACTION = "ConfigurationAction";

    public ConfigurationToolItem() {
        super("configuration_tool", "configuration_tool_base", "configuration_tool_mode");
    }

    public static ConfigurationAction getAction(ItemStack stack) {
        ConfigurationAction action = stack.hasTagCompound() ? ConfigurationAction.byName(
            stack.getTagCompound()
                .getString(KEY_ACTION))
            : null;
        return action == null ? ConfigurationAction.LOCKING : action;
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
        ConfigurationAction action = getAction(stack);
        drawer.applyConfiguration(action);
        if (action == ConfigurationAction.INDICATOR) {
            ToolFeedback.send(
                player,
                new ChatComponentTranslation(
                    "configurationtool.configmode.indicator.mode_" + drawer.getDrawerOptions()
                        .getAdvancedValue(action)));
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            ConfigurationAction[] actions = ConfigurationAction.values();
            ConfigurationAction action = actions[(getAction(stack).ordinal() + 1) % actions.length];
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound()
                .setString(KEY_ACTION, action.name());
            if (world.isRemote) {
                showActionBarFeedback(
                    "configurationtool.configmode.swapped",
                    action.getFeedbackColor(),
                    new ChatComponentTranslation(
                        "configurationtool.configmode." + action.name()
                            .toLowerCase(Locale.ROOT)));
            } else {
                world.playSoundAtEntity(player, "random.click", 0.5F, 1F);
            }
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return pass == 1 ? getAction(stack).getColor() : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("configurationtool.configmode")
                + EnumChatFormatting.WHITE
                + getAction(stack).getLocalizedName());
        tooltip.add("");
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("configurationtool.use"));
    }
}
