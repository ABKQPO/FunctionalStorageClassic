package com.hfstudio.functionalstorage.common.inventory.base;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.AspectSummary;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.common.storage.AspectStorageResource;

/**
 * Concrete essentia storage over the generic core.
 *
 * <p>
 * Keeps a memo of its contents because Thaumcraft polls a container for its contents
 * and suction far more often than they change. The memo is dropped whenever a slot's
 * contents or capacity change, and also when the lock state does, since a lock
 * transition alters what the storage invites without moving any essentia.
 * </p>
 */
public class BigAspectHandler extends AbstractStorageHandler<BigAspectStack, AspectStorageKey>
    implements IBigAspectHandler {

    private AspectSummary summary;

    public BigAspectHandler(int slots) {
        super(AspectStorageResource.INSTANCE, slots);
    }

    /**
     * Returns the memo, computing and storing it on first use.
     *
     * <p>
     * Computing here rather than in the caller is what makes the memo take effect: a
     * caller that receives null would summarize afresh on every request, which is the
     * cost this exists to remove.
     * </p>
     *
     * @return a memo describing the current contents
     */
    @Override
    public AspectSummary getSummary() {
        AspectSummary cached = summary;
        if (cached == null) {
            cached = AspectSummary.of(this);
            summary = cached;
        }
        return cached;
    }

    @Override
    protected void onSlotChanged() {
        summary = null;
    }

    @Override
    protected void onCapacityChanged() {
        summary = null;
    }

    @Override
    protected boolean isCompatible(@Nonnull BigAspectStack template, @Nonnull BigAspectStack candidate) {
        return template.isSameType(candidate.getAspect());
    }
}
