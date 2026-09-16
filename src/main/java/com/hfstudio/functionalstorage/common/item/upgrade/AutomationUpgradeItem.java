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

        /**
         * @return the stable identifier used in NBT and lang keys
         */
        public String getId() {
            return id;
        }

        /**
         * @return the localized display name
         */
        public String getDisplayName() {
            return StatCollector.translateToLocal("functionalstorage.direction." + id);
        }

        /**
         * @param index stored ordinal
         * @return the matching direction, or {@link #FRONT}
         */
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

    /**
     * @return whether this upgrade works on a side of the drawer
     */
    public boolean hasDirection() {
        return true;
    }

    /**
     * @return whether this upgrade remembers the player that installed it
     */
    public boolean hasOwner() {
        return true;
    }

    /**
     * @return the configured operation interval in ticks
     */
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

    /**
     * Runs one automation step.
     *
     * @param tile  owning drawer
     * @param stack installed upgrade stack
     * @param slot  utility slot index
     */
    public void work(@Nonnull ControllableDrawerTile tile, @Nonnull ItemStack stack, int slot) {}

    @Override
    public void applyUpgrade(@Nonnull ItemStack stack, @Nonnull UpgradeState.Builder builder) {}

    /**
     * @param stack upgrade stack
     * @return the configured working direction
     */
    @Nonnull
    public RelativeDirection getDirection(@Nonnull ItemStack stack) {
        return RelativeDirection.byIndex(tagOf(stack).getInteger(KEY_DIRECTION));
    }

    /**
     * @param stack     upgrade stack
     * @param direction new working direction
     */
    public void setDirection(@Nonnull ItemStack stack, @Nonnull RelativeDirection direction) {
        tagOf(stack).setInteger(KEY_DIRECTION, direction.ordinal());
    }

    /**
     * @param stack upgrade stack
     * @return the owner identifier, or {@code null} when unset
     */
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

    /**
     * @param stack upgrade stack
     * @return whether a per-upgrade filter has been configured
     */
    public boolean hasFilter(@Nonnull ItemStack stack) {
        return tagOf(stack).hasKey(KEY_FILTER);
    }

    /**
     * @param stack upgrade stack
     * @return the configured item filter, or {@code null}
     */
    @Nullable
    public ItemStack getFilter(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        if (!tag.hasKey(KEY_FILTER)) {
            return null;
        }
        return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(KEY_FILTER));
    }

    /**
     * @param stack  upgrade stack
     * @param filter new filter, or {@code null} to clear
     */
    public void setFilter(@Nonnull ItemStack stack, @Nullable ItemStack filter) {
        NBTTagCompound tag = tagOf(stack);
        if (filter == null || filter.getItem() == null) {
            tag.removeTag(KEY_FILTER);
            return;
        }
        tag.setTag(KEY_FILTER, filter.writeToNBT(new NBTTagCompound()));
    }

    /**
     * @param stack upgrade stack
     * @return the configured slot selection, or {@code null} when all slots are used
     */
    @Nullable
    public int[] getSelectedSlots(@Nonnull ItemStack stack) {
        NBTTagCompound tag = tagOf(stack);
        return tag.hasKey(KEY_SLOTS) ? tag.getIntArray(KEY_SLOTS) : null;
    }

    /**
     * @param stack upgrade stack
     * @param slots selected drawer slots, or {@code null} for all slots
     */
    public void setSelectedSlots(@Nonnull ItemStack stack, @Nullable int[] slots) {
        NBTTagCompound tag = tagOf(stack);
        if (slots == null || slots.length == 0) {
            tag.removeTag(KEY_SLOTS);
            return;
        }
        tag.setIntArray(KEY_SLOTS, slots);
    }

    /**
     * @param stack upgrade stack
     * @return the ticks remaining until the next run
     */
    public int getRemainingTicks(@Nonnull ItemStack stack) {
        return Math.max(0, tagOf(stack).getInteger(KEY_TIMER));
    }

    /**
     * Stores the remaining ticks until the next run.
     *
     * @param stack upgrade stack
     * @param ticks remaining ticks
     */
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

    /**
     * @param stack upgrade stack
     * @return the mutable upgrade tag, created on demand
     */
    @Nonnull
    protected static NBTTagCompound tagOf(@Nonnull ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
