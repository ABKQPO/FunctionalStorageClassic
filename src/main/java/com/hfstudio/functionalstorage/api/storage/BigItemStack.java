package com.hfstudio.functionalstorage.api.storage;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Immutable item snapshot composed of a defensively copied type template and
 * a long amount. A present template always has a count of one. A zero amount
 * may retain a template to represent a locked filter; negative amounts are
 * clamped to zero. An absent resource is normalized to {@link #empty()}.
 */
public class BigItemStack implements StorageSnapshot<BigItemStack, ItemStorageKey> {

    private static final BigItemStack EMPTY = new BigItemStack();

    private final ItemStack template;
    @Nullable
    private final ItemStorageKey key;
    private final long amount;

    private BigItemStack() {
        this.template = null;
        this.key = null;
        this.amount = 0L;
    }

    /**
     * Creates a snapshot. The supplied stack is copied immediately and is
     * never retained by reference.
     *
     * @param template item type, metadata, and NBT to represent; {@code null} is empty
     * @param amount   represented amount; negative values are clamped to zero
     */
    public BigItemStack(@Nullable ItemStack template, long amount) {
        if (template == null || template.getItem() == null) {
            this.template = null;
            this.key = null;
            this.amount = 0L;
            return;
        }
        ItemStack copy = template.copy();
        copy.stackSize = 1;
        this.template = copy;
        this.key = new ItemStorageKey(copy);
        this.amount = Math.max(0L, amount);
    }

    @Nonnull
    public static BigItemStack empty() {
        return EMPTY;
    }

    /**
     * Compares item, metadata, and stack NBT while ignoring count.
     *
     * @param left  first stack, may be null
     * @param right second stack, may be null
     * @return {@code true} when both stacks represent the same exact item type
     */
    public static boolean matches(@Nullable ItemStack left, @Nullable ItemStack right) {
        if (left == null || right == null || left.getItem() == null || right.getItem() == null) {
            return false;
        }
        if (left.getItem() != right.getItem() || left.getItemDamage() != right.getItemDamage()) {
            return false;
        }
        NBTTagCompound leftTag = left.getTagCompound();
        NBTTagCompound rightTag = right.getTagCompound();
        return Objects.equals(leftTag, rightTag);
    }

    // Only existing snapshots may share the private, never-exposed template.
    private BigItemStack(ItemStack template, ItemStorageKey key, long amount) {
        this.template = template;
        this.key = key;
        this.amount = amount;
    }

    /**
     * Returns a fresh copy of the normalized template. Mutating the returned
     * stack cannot alter this snapshot.
     *
     * @return a count-one template, or {@code null}
     */
    @Nullable
    public ItemStack getTemplate() {
        return template == null ? null : template.copy();
    }

    /**
     * Reports how large a stack of the represented item may grow.
     *
     * <p>
     * Capacity arithmetic runs this over every index of a storage, including a
     * whole controller network, so it must not copy the template. Callers that
     * need the stack itself use {@link #getTemplate()}.
     * </p>
     *
     * @return the template's maximum stack size, or sixty-four when empty
     */
    public int getTemplateStackSize() {
        return template == null ? 64 : Math.max(1, template.getMaxStackSize());
    }

    @Nullable
    @Override
    public ItemStorageKey getKey() {
        return key;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    /** Changing the amount retains the resource type, including at zero. */
    @Nonnull
    @Override
    public BigItemStack withAmount(long newAmount) {
        if (template == null) {
            return empty();
        }
        long normalized = Math.max(0L, newAmount);
        return normalized == amount ? this : new BigItemStack(template, key, normalized);
    }

    /**
     * Distinguishes an unfiltered empty snapshot from a zero-amount snapshot
     * retaining a locked type.
     *
     * @return whether this snapshot carries a resource template
     */
    @Override
    public boolean hasTemplate() {
        return key != null;
    }

    /**
     * Compares item, metadata, and stack NBT while ignoring count.
     *
     * @param other other stack
     * @return {@code true} when this snapshot and the stack represent the same item type
     */
    public boolean isSameType(@Nullable ItemStack other) {
        return matches(template, other);
    }

    /**
     * Converts this snapshot to Forge's int-count representation. Amounts
     * above {@link Integer#MAX_VALUE} are saturated at that boundary.
     *
     * @return a fresh mutable stack, or {@code null} when nothing is stored
     */
    @Nullable
    public ItemStack toItemStack() {
        if (template == null || amount == 0L) {
            return null;
        }
        ItemStack result = template.copy();
        result.stackSize = amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BigItemStack other)) {
            return false;
        }
        return amount == other.amount && Objects.equals(key, other.key);
    }

    @Override
    public int hashCode() {
        return 31 * (key == null ? 0 : key.hashCode()) + Long.hashCode(amount);
    }

    @Override
    public String toString() {
        return "BigItemStack{" + key + " x" + amount + '}';
    }
}
