package com.hfstudio.functionalstorage.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import lombok.Getter;

/** Captures a possibly mutable icon while preserving cropped and flipped texture coordinates. */
@Getter
public class MaterialIcon implements IIcon {

    private final String iconName;
    private final int iconWidth;
    private final int iconHeight;
    private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
    private final TextureAtlasSprite sprite;

    public MaterialIcon(IIcon icon) {
        iconName = icon.getIconName();
        iconWidth = icon.getIconWidth();
        iconHeight = icon.getIconHeight();
        minU = icon.getInterpolatedU(0);
        maxU = icon.getInterpolatedU(16);
        minV = icon.getInterpolatedV(0);
        maxV = icon.getInterpolatedV(16);
        TextureMap atlas = Minecraft.getMinecraft()
            .getTextureMapBlocks();
        TextureAtlasSprite resolved = icon instanceof TextureAtlasSprite direct ? direct
            : atlas.getAtlasSprite(iconName);
        sprite = resolved == atlas.getAtlasSprite("missingno") ? null : resolved;
    }

    @Override
    public float getInterpolatedU(double position) {
        return minU + (maxU - minU) * (float) (position / 16);
    }

    @Override
    public float getInterpolatedV(double position) {
        return minV + (maxV - minV) * (float) (position / 16);
    }
}
