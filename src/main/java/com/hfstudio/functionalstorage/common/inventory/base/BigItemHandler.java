package com.hfstudio.functionalstorage.common.inventory.base;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.StorageViewCache;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;
import com.hfstudio.functionalstorage.util.ItemUtil;

/**
 * Concrete item storage over the generic core. Adds item-specific slot
 * compatibility so the ore dictionary upgrade can widen matching.
 */
public class BigItemHandler extends AbstractStorageHandler<BigItemStack, ItemStorageKey> implements IBigItemHandler {

    private final StorageViewCache viewCache = new StorageViewCache();

    public BigItemHandler(int slots) {
        super(ItemStorageResource.INSTANCE, slots);
    }

    @Override
    public StorageViewCache getStorageViewCache() {
        return viewCache;
    }

    @Override
    protected void onSlotChanged() {
        viewCache.invalidate();
    }

    /**
     * Reports whether a non-exact resource may still share a slot.
     *
     * <p>
     * Routing relies on this to skip a compatibility probe that could never succeed,
     * so a subclass that widens matching must answer {@code true} here as well as in
     * {@link #isCompatible}.
     * </p>
     *
     * @return whether equivalent resources are interchangeable
     */
    @Override
    public boolean allowsEquivalentResources() {
        return allowsEquivalentItems();
    }

    @Override
    protected boolean isCompatible(@Nonnull BigItemStack template, @Nonnull BigItemStack candidate) {
        if (template.isSameType(candidate)) {
            return true;
        }
        if (!allowsEquivalentItems()) {
            return false;
        }
        ItemStack templateStack = template.getTemplate();
        ItemStack candidateStack = candidate.getTemplate();
        if (templateStack == null || candidateStack == null) {
            return false;
        }
        return ItemUtil.sharesOreDictionary(templateStack, candidateStack);
    }

    protected boolean allowsEquivalentItems() {
        return false;
    }

    public long insertStack(int index, @Nonnull ItemStack stack) {
        return insert(index, new BigItemStack(stack, stack.stackSize), StorageAction.EXECUTE).getProcessedAmount();
    }

    public boolean acceptsStack(int index, @Nullable ItemStack stack) {
        return stack != null
            && insert(index, new BigItemStack(stack, 1L), StorageAction.SIMULATE).getProcessedAmount() > 0L;
    }
}
