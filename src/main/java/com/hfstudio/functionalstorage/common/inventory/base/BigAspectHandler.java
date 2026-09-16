package com.hfstudio.functionalstorage.common.inventory.base;

import javax.annotation.Nonnull;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.common.storage.AspectStorageResource;

public class BigAspectHandler extends AbstractStorageHandler<BigAspectStack, AspectStorageKey>
    implements IBigAspectHandler {

    public BigAspectHandler(int slots) {
        super(AspectStorageResource.INSTANCE, slots);
    }

    @Override
    protected boolean isCompatible(@Nonnull BigAspectStack template, @Nonnull BigAspectStack candidate) {
        return template.isSameType(candidate.getAspect());
    }
}
