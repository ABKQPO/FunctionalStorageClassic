package com.hfstudio.functionalstorage.client.integration;

import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AngelicaFontBatch {

    private AngelicaFontBatch() {}

    @Optional.Method(modid = "angelica")
    public static boolean begin(FontRenderer font) {
        if (!(font instanceof FontRendererAccessor accessor)) {
            return false;
        }
        accessor.angelica$getBatcher()
            .beginBatch();
        return true;
    }

    @Optional.Method(modid = "angelica")
    public static void end(FontRenderer font) {
        ((FontRendererAccessor) font).angelica$getBatcher()
            .endBatch();
    }
}
