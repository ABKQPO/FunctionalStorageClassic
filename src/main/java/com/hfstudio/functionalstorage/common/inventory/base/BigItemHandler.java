package com.hfstudio.functionalstorage.common.inventory.base;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.storage.ItemStorageResource;
import com.hfstudio.functionalstorage.util.ItemUtil;

/**
 * Concrete item storage over the generic core. Adds item-specific slot
 * compatibility so the ore dictionary upgrade can widen matching.
 */
public class BigItemHandler extends AbstractStorageHandler<BigItemStack, ItemStorageKey> implements IBigItemHandler {

    public BigItemHandler(int slots) {
        super(ItemStorageResource.INSTANCE, slots);
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
