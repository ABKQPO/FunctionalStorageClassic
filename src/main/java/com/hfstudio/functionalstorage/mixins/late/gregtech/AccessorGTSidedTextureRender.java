package com.hfstudio.functionalstorage.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import gregtech.api.interfaces.ITexture;
import gregtech.common.render.GTSidedTextureRender;

@Mixin(value = GTSidedTextureRender.class, remap = false)
public interface AccessorGTSidedTextureRender {

    @Accessor("mTextures")
    ITexture[] functionalStorage$getTextures();
}
