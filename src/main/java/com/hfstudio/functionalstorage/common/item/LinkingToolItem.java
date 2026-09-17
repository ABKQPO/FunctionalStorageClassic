package com.hfstudio.functionalstorage.common.item;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.common.interaction.ToolFeedback;
import com.hfstudio.functionalstorage.common.tile.EnderDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LinkingToolItem extends LayeredToolItem {

    public enum LinkingMode {
        SINGLE,
        MULTIPLE
    }

    public enum ActionMode {
        ADD,
        REMOVE
    }

    private static final String KEY_TARGET = "ControllerTarget";
    private static final String KEY_MODE = "Multiple";
    private static final String KEY_REMOVE = "Remove";
    private static final String KEY_FIRST = "FirstPosition";
    private static final String KEY_FREQUENCY = "EnderFrequency";
    private static final String KEY_SAFETY = "EnderSafety";

    public LinkingToolItem() {
        super(
            "linking_tool",
            "linking_tool_base",
            "linking_tool_mode",
            "linking_tool_action",
            "linking_tool_controller");
    }

    public static LinkingMode getLinkingMode(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean(KEY_MODE) ? LinkingMode.MULTIPLE : LinkingMode.SINGLE;
    }

    public static ActionMode getActionMode(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean(KEY_REMOVE) ? ActionMode.REMOVE : ActionMode.ADD;
    }

    public static String getFrequency(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getString(KEY_FREQUENCY) : "";
    }

    public static int[] getTarget(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getIntArray(KEY_TARGET) : new int[0];
    }

    public static int[] getFirstPosition(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getIntArray(KEY_FIRST) : new int[0];
    }

    public static boolean hasTarget(ItemStack stack) {
        return getTarget(stack).length == 4;
    }

    public boolean captureFrequency(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
        if (!(world.getTileEntity(x, y, z) instanceof EnderDrawerTile drawer)) {
            return false;
        }
        if (!world.isRemote) {
            tag(stack).setString(
                KEY_FREQUENCY,
                drawer.getOrCreateFrequency()
                    .toString());
            tag(stack).removeTag(KEY_SAFETY);
            message(player, "linkingtool.ender.stored");
        }
        return true;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
        return captureFrequency(stack, player, player.worldObj, x, y, z);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        NBTTagCompound data = tag(stack);
        if (tile instanceof EnderDrawerTile ender && !getFrequency(stack).isEmpty()) {
            int[] safety = data.getIntArray(KEY_SAFETY);
            boolean confirmed = player.isSneaking() && safety.length == 4
                && safety[0] == x
                && safety[1] == y
                && safety[2] == z
                && safety[3] == world.provider.dimensionId;
            if (!ender.getItemHandler()
                .getSnapshot(0)
                .isEmpty() && !confirmed) {
                data.setIntArray(KEY_SAFETY, new int[] { x, y, z, world.provider.dimensionId });
                message(player, "linkingtool.ender.warning");
                return true;
            }
            try {
                // Preserve the old network so an explicit rebind never deletes stored contents.
                ender.getOrCreateFrequency();
                ender.setFrequency(UUID.fromString(getFrequency(stack)));
                data.removeTag(KEY_SAFETY);
                message(player, "linkingtool.ender.changed");
            } catch (IllegalArgumentException exception) {
                data.removeTag(KEY_FREQUENCY);
            }
            return true;
        }
        if (tile instanceof DrawerControllerTile) {
            data.setIntArray(KEY_TARGET, new int[] { x, y, z, world.provider.dimensionId });
            data.removeTag(KEY_FREQUENCY);
            data.removeTag(KEY_FIRST);
            message(player, "linkingtool.controller.configured");
            return true;
        }
        int[] target = getTarget(stack);
        if (target.length != 4 || target[3] != world.provider.dimensionId
            || !world.blockExists(target[0], target[1], target[2])
            || !(world.getTileEntity(target[0], target[1], target[2]) instanceof DrawerControllerTile controller)) {
            return false;
        }
        boolean remove = getActionMode(stack) == ActionMode.REMOVE;
        if (getLinkingMode(stack) == LinkingMode.SINGLE) {
            if (controller.linkDrawer(x, y, z, remove)) {
                message(player, "linkingtool.single_drawer." + (remove ? "removed" : "linked"));
            } else {
                message(player, "functionalstorage.linking_tool.out_of_range");
            }
        } else {
            int[] first = data.getIntArray(KEY_FIRST);
            if (first.length != 4 || first[3] != world.provider.dimensionId) {
                data.setIntArray(KEY_FIRST, new int[] { x, y, z, world.provider.dimensionId });
                message(player, "functionalstorage.linking_tool.first_position", x, y, z);
                return true;
            }
            data.removeTag(KEY_FIRST);
            int range = controller.getLinkingRange();
            int count = 0;
            // Clip selection to controller range; never load chunks or allocate a volume-sized list.
            for (int bx = Math.max(Math.min(first[0], x), target[0] - range); bx
                <= Math.min(Math.max(first[0], x), target[0] + range); bx++) {
                for (int by = Math.max(Math.min(first[1], y), Math.max(0, target[1] - range)); by
                    <= Math.min(Math.max(first[1], y), Math.min(255, target[1] + range)); by++) {
                    for (int bz = Math.max(Math.min(first[2], z), target[2] - range); bz
                        <= Math.min(Math.max(first[2], z), target[2] + range); bz++) {
                        if (controller.linkDrawer(bx, by, bz, remove)) {
                            count++;
                        }
                    }
                }
            }
            message(
                player,
                count == 0 ? "functionalstorage.linking_tool.out_of_range"
                    : "linkingtool.multiple_drawer." + (remove ? "removed" : "linked"));
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        NBTTagCompound data = tag(stack);
        if (!getFrequency(stack).isEmpty()) {
            if (player.isSneaking()) {
                data.removeTag(KEY_FREQUENCY);
                data.removeTag(KEY_SAFETY);
                message(player, "linkingtool.drawer.clear");
            }
        } else if (player.isSneaking()) {
            data.setBoolean(KEY_MODE, !data.getBoolean(KEY_MODE));
            data.removeTag(KEY_FIRST);
            message(player, "linkingtool.linkingmode.swapped", new ChatComponentTranslation(modeKey(stack)));
        } else {
            data.setBoolean(KEY_REMOVE, !data.getBoolean(KEY_REMOVE));
            message(player, "linkingtool.linkingaction.swapped", new ChatComponentTranslation(actionKey(stack)));
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return switch (pass) {
            case 1 -> getLinkingMode(stack) == LinkingMode.SINGLE ? 0x00FFFF : 0x00FF00;
            case 2 -> getActionMode(stack) == ActionMode.ADD ? 0x2883FA : 0xFA9128;
            case 3 -> hasTarget(stack) ? 0x00FF00 : 0x555555;
            default -> 0xFFFFFF;
        };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack, int pass) {
        return !getFrequency(stack).isEmpty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        if (!getFrequency(stack).isEmpty()) {
            tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("linkingtool.ender.frequency"));
            tooltip.add(getFrequency(stack));
            tooltip.add(StatCollector.translateToLocal("linkingtool.ender.clear"));
            return;
        }
        tooltip.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("linkingtool.linkingmode")
                + EnumChatFormatting.AQUA
                + StatCollector.translateToLocal(modeKey(stack)));
        tooltip.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("linkingtool.linkingaction")
                + EnumChatFormatting.AQUA
                + StatCollector.translateToLocal(actionKey(stack)));
        int[] target = getTarget(stack);
        tooltip.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("linkingtool.controller")
                + EnumChatFormatting.DARK_AQUA
                + (target.length == 4 ? target[0] + ", " + target[1] + ", " + target[2] : "???"));
        tooltip.add("");
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(modeKey(stack) + ".desc"));
        for (String line : StatCollector.translateToLocal("linkingtool.use")
            .replace("\\n", "\n")
            .split("\n", -1)) {
            tooltip.add(EnumChatFormatting.GRAY + line);
        }
    }

    private static String modeKey(ItemStack stack) {
        return "linkingtool.linkingmode." + getLinkingMode(stack).name()
            .toLowerCase(Locale.ROOT);
    }

    private static String actionKey(ItemStack stack) {
        return "linkingtool.linkingaction." + getActionMode(stack).name()
            .toLowerCase(Locale.ROOT);
    }

    private NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private void message(EntityPlayer player, String key, Object... args) {
        ToolFeedback.send(player, new ChatComponentTranslation(key, args));
    }
}
