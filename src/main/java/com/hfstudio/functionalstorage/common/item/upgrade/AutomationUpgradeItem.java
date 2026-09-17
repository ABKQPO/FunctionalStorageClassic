package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.common.interaction.ToolFeedback;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;
import com.hfstudio.functionalstorage.util.UpgradeTargeting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

public class AutomationUpgradeItem extends UpgradeItem implements IStorageUpgrade {

    private static final String KEY_OWNER = "Owner";
    private static final String KEY_DIRECTION = "RelativeDirection";
    private static final String KEY_TIMER = "Timer";
    private static final String KEY_FILTER = "Filter";
    private static final String KEY_SLOTS = "SelectedSlots";

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

    public boolean isWireless() {
        return false;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || !hasDirection()) return false;
        if (world.isRemote) return false;
        if (!isWireless()) {
            ForgeDirection target = ForgeDirection.getOrientation(side);
            ControllableDrawerTile drawer = world.getTileEntity(x, y, z) instanceof ControllableDrawerTile tile ? tile
                : null;
            for (RelativeDirection direction : RelativeDirection.values()) {
                setDirection(stack, direction);
                if ((drawer == null ? UpgradeTargeting.resolve(ForgeDirection.NORTH, direction)
                    : UpgradeTargeting.targetDirection(drawer, stack)) == target) break;
            }
            return true;
        }
        NBTTagCompound tag = tagOf(stack);
        tag.setIntArray("WirelessTarget", new int[] { x, y, z });
        tag.setInteger("WirelessDimension", world.provider.dimensionId);
        tag.setInteger("WirelessSide", side);
        ToolFeedback.send(player, new ChatComponentTranslation("functionalstorage.upgrade.wireless_target", x, y, z));
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && hasDirection() && !isWireless())
            setDirection(stack, RelativeDirection.byIndex((getDirection(stack).ordinal() + 1) % 6));
        return stack;
    }

    public int getTickInterval() {
        return Math.max(1, switch (getId()) {
            case "breaker_upgrade" -> FunctionalStorageConfig.UPGRADES.breakerTick;
            case "placer_upgrade" -> FunctionalStorageConfig.UPGRADES.placerTick;
            case "refill_upgrade", "dimensional_refill_upgrade" -> FunctionalStorageConfig.UPGRADES.refillTick;
            case "pulling_upgrade", "pushing_upgrade", "wireless_pulling_upgrade", "wireless_pushing_upgrade", "collector_upgrade" -> FunctionalStorageConfig.UPGRADES.upgradeTick;
            default -> baseTickInterval;
        });
    }

    public int getTickInterval(ItemStack stack) {
        ItemStack augments = UpgradeSettings.getStack(stack, "SpeedAugments");
        return Math.max(
            1,
            getTickInterval()
                - (augments == null ? 0 : augments.stackSize) * FunctionalStorageConfig.UPGRADES.speedAugmentReduction);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isRemote && entity instanceof EntityPlayer player) onInventoryTick(stack, world, player);
    }

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
        return RelativeDirection.byIndex(UpgradeSettings.get(stack, KEY_DIRECTION));
    }

    public void setDirection(@Nonnull ItemStack stack, @Nonnull RelativeDirection direction) {
        tagOf(stack).setInteger(KEY_DIRECTION, direction.ordinal());
    }

    @Nullable
    public UUID getOwner(@Nonnull ItemStack stack) {
        String value = stack.hasTagCompound() ? stack.getTagCompound()
            .getString(KEY_OWNER) : "";
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
        return UpgradeSettings.hasFilters(stack);
    }

    @Nullable
    public ItemStack getFilter(@Nonnull ItemStack stack) {
        return UpgradeSettings.getStack(stack, KEY_FILTER);
    }

    public void setFilter(@Nonnull ItemStack stack, @Nullable ItemStack filter) {
        UpgradeSettings.setFilter(stack, 0, filter);
    }

    @Nullable
    public int[] getSelectedSlots(@Nonnull ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(KEY_SLOTS) ? stack.getTagCompound()
                .getIntArray(KEY_SLOTS) : null;
    }

    public void setSelectedSlots(@Nonnull ItemStack stack, @Nullable int[] slots) {
        NBTTagCompound tag = tagOf(stack);
        if (slots == null) {
            tag.removeTag(KEY_SLOTS);
            return;
        }
        tag.setIntArray(KEY_SLOTS, slots);
    }

    public int getRemainingTicks(@Nonnull ItemStack stack) {
        return Math.max(0, UpgradeSettings.get(stack, KEY_TIMER));
    }

    public void setRemainingTicks(@Nonnull ItemStack stack, int ticks) {
        tagOf(stack).setInteger(KEY_TIMER, Math.max(0, ticks));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("functionalupgrade.desc.execute_every_tick", getTickInterval(stack)));
        if (this instanceof PullingUpgradeItem || this instanceof PushingUpgradeItem) {
            boolean pull = this instanceof PullingUpgradeItem;
            String operation = pull ? "pull" : "push";
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "drawer_upgrade.functionalstorage." + operation + ".item",
                    pull ? FunctionalStorageConfig.UPGRADES.upgradePullItems
                        : FunctionalStorageConfig.UPGRADES.upgradePushItems));
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "drawer_upgrade.functionalstorage." + operation + ".fluid",
                    NumberFormatUtil.formatFluid(
                        pull ? FunctionalStorageConfig.UPGRADES.upgradePullFluid
                            : FunctionalStorageConfig.UPGRADES.upgradePushFluid)));
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "drawer_upgrade.functionalstorage." + operation + ".aspect",
                    pull ? FunctionalStorageConfig.UPGRADES.upgradePullAspect
                        : FunctionalStorageConfig.UPGRADES.upgradePushAspect));
        }
        if (hasDirection()) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "functionalstorage.upgrade.direction",
                    getDirection(stack).getDisplayName()));
        }
        ItemStack filter = getFilter(stack);
        if (filter != null) {
            tooltip.add(
                StatCollector.translateToLocal("functionalstorage.upgrade.filtered") + ": " + filter.getDisplayName());
        }
        if (getSelectedSlots(stack) != null) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.upgrade.slot_selection"));
        }
        if (isWireless()) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.upgrade.wireless_use"));
            int[] target = stack.hasTagCompound() ? stack.getTagCompound()
                .getIntArray("WirelessTarget") : new int[0];
            if (target.length == 3) tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "functionalstorage.upgrade.wireless_target",
                    target[0],
                    target[1],
                    target[2]));
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
