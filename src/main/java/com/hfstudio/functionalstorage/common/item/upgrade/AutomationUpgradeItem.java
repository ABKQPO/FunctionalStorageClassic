package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/**
 * Base for the automation upgrades merged in from More Functional Storage.
 * Adds owner tracking, a relative working direction, a redstone mode, and a
 * configurable operation interval on top of the functional upgrade contract.
 */
public class AutomationUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    private static final String KEY_OWNER = "Owner";
    private static final String KEY_DIRECTION = "RelativeDirection";
    private static final String KEY_TIMER = "Timer";
    private static final String KEY_FILTER = "Filter";
    private static final String KEY_SLOTS = "SelectedSlots";

    /**
     * Side of the drawer an automation upgrade works on, relative to its face.
     */
    @Getter
    public enum RelativeDirection {

        FRONT("front"),
        BACK("back"),
        LEFT("left"),
        RIGHT("right"),
        UP("up"),
        DOWN("down");

        private final String id;

        RelativeDirection(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return StatCollector.translateToLocal("functionalstorage.direction." + id);
        }

        @Nonnull
        public static RelativeDirection byIndex(int index) {
            RelativeDirection[] values = values();
            return index < 0 || index >= values.length ? FRONT : values[index];
        }
    }

    private final int baseTickInterval;

    protected AutomationUpgradeItem(@Nonnull String id, int baseTickInterval) {
        super(id);
        this.baseTickInterval = Math.max(1, baseTickInterval);
    }

    public boolean hasDirection() {
        return true;
    }

    public boolean hasOwner() {
        return true;
    }

    public int getTickInterval() {
        return baseTickInterval;
    }

    /**
     * Records the installing player when the upgrade first enters an inventory.
     *
     * @param stack  upgrade stack
     * @param world  world holding the player
     * @param player owning player
     */
    public void onInventoryTick(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull EntityPlayer player) {
        if (!hasOwner()) {
            return;
        }
        NBTTagCompound tag = tagOf(stack);
        if (tag.hasKey(KEY_OWNER)) {
            return;
        }
        tag.setString(
            KEY_OWNER,
            player.getUniqueID()
                .toString());
        if (hasDirection() && !tag.hasKey(KEY_DIRECTION)) {
            tag.setInteger(KEY_DIRECTION, RelativeDirection.FRONT.ordinal());
        }
    }

    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {}

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {}

    @Nonnull
    public RelativeDirection getDirection(@Nonnull ItemStack stack) {
        return RelativeDirection.byIndex(tagOf(stack).getInteger(KEY_DIRECTION));
    }

    public void setDirection(@Nonnull ItemStack stack, @Nonnull RelativeDirection direction) {
        tagOf(stack).setInteger(KEY_DIRECTION, direction.ordinal());
    }

    @Nullable
    public UUID getOwner(@Nonnull ItemStack stack) {
        String value = tagOf(stack).getString(KEY_OWNER);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean hasFilter(@Nonnull ItemStack stack) {
        return tagOf(stack).hasKey(KEY_FILTER);
    }

    @Nullable
    public ItemStack getFilter(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        if (!tag.hasKey(KEY_FILTER)) {
            return null;
        }
        return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(KEY_FILTER));
    }

    public void setFilter(@Nonnull ItemStack stack, @Nullable ItemStack filter) {
        NBTTagCompound tag = tagOf(stack);
        if (filter == null || filter.getItem() == null) {
            tag.removeTag(KEY_FILTER);
            return;
        }
        tag.setTag(KEY_FILTER, filter.writeToNBT(new NBTTagCompound()));
    }

    @Nullable
    public int[] getSelectedSlots(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        return tag.hasKey(KEY_SLOTS) ? tag.getIntArray(KEY_SLOTS) : null;
    }

    public void setSelectedSlots(@Nonnull ItemStack stack, @Nullable int[] slots) {
        NBTTagCompound tag = tagOf(stack);
        if (slots == null || slots.length == 0) {
            tag.removeTag(KEY_SLOTS);
            return;
        }
        tag.setIntArray(KEY_SLOTS, slots);
    }

    public int getRemainingTicks(@Nonnull ItemStack stack) {
        return Math.max(0, tagOf(stack).getInteger(KEY_TIMER));
    }

    public void setRemainingTicks(@Nonnull ItemStack stack, int ticks) {
        tagOf(stack).setInteger(KEY_TIMER, Math.max(0, ticks));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (hasDirection()) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "functionalstorage.upgrade.direction",
                    getDirection(stack).getDisplayName()));
        }
        if (hasFilter(stack)) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.upgrade.filtered"));
        }
        if (getSelectedSlots(stack) != null) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.upgrade.slot_selection"));
        }
    }

    @Nonnull
    protected static NBTTagCompound tagOf(@Nonnull ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
