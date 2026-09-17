package com.hfstudio.functionalstorage.client.render;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.common.integration.thaumcraft.EssentiaContainerRegistry;
import com.hfstudio.functionalstorage.common.interaction.FluidContainerInteraction;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DrawerContainerHintOverlay extends Gui {

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.hideGUI || minecraft.currentScreen != null
            || minecraft.thePlayer == null
            || minecraft.theWorld == null
            || !(minecraft.objectMouseOver instanceof MovingObjectPosition hit)
            || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        TileEntity tile = minecraft.theWorld.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return;
        }
        ItemStack held = minecraft.thePlayer.getHeldItem();
        String prefix = hintPrefix(drawer, held);
        if (prefix == null) {
            return;
        }
        drawHint(event.resolution, prefix);
    }

    private String hintPrefix(ControllableDrawerTile drawer, ItemStack held) {
        if (drawer.getFluidHandler() != null && FluidContainerInteraction.isFluidContainer(held)) {
            return "fluid_drawer_hint";
        }
        return drawer.getAspectHandler() != null && isEssentiaContainer(held) ? "essentia_drawer_hint" : null;
    }

    @Optional.Method(modid = "Thaumcraft")
    private boolean isEssentiaContainer(ItemStack stack) {
        return EssentiaContainerRegistry.isEssentiaContainer(stack);
    }

    private void drawHint(ScaledResolution resolution, String prefix) {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<String> lines = List.of(
            EnumChatFormatting.GOLD + StatCollector.translateToLocal("gui.functionalstorage." + prefix),
            EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                "gui.functionalstorage." + prefix + ".empty",
                keyName(minecraft.gameSettings.keyBindUseItem.getKeyCode())),
            EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                "gui.functionalstorage." + prefix + ".fill",
                keyName(minecraft.gameSettings.keyBindAttack.getKeyCode())));
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.fontRenderer.getStringWidth(line));
        }
        int height = 8 + (lines.size() - 1) * 10;
        int x = resolution.getScaledWidth() / 2 + 12;
        int y = resolution.getScaledHeight() / 2 + 12;
        if (x + width + 6 > resolution.getScaledWidth()) {
            x = resolution.getScaledWidth() - width - 6;
        }
        if (y + height + 6 > resolution.getScaledHeight()) {
            y = resolution.getScaledHeight() - height - 6;
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            drawTooltipBackground(x, y, width, height);
            GL11.glTranslatef(0F, 0F, 300F);
            for (String line : lines) {
                minecraft.fontRenderer.drawStringWithShadow(line, x, y, 0xFFFFFF);
                y += 10;
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private String keyName(int keyCode) {
        return GameSettings.getKeyDisplayString(keyCode);
    }

    private void drawTooltipBackground(int x, int y, int width, int height) {
        int backgroundStart = 0xF0100010;
        int backgroundEnd = 0xF0100010;
        int borderStart = 0x505000FF;
        int borderEnd = 0x5028007F;
        drawGradientRect(x - 3, y - 4, x + width + 3, y - 3, backgroundStart, backgroundStart);
        drawGradientRect(x - 3, y + height + 3, x + width + 3, y + height + 4, backgroundEnd, backgroundEnd);
        drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, backgroundStart, backgroundEnd);
        drawGradientRect(x - 4, y - 3, x - 3, y + height + 3, backgroundStart, backgroundEnd);
        drawGradientRect(x + width + 3, y - 3, x + width + 4, y + height + 3, backgroundStart, backgroundEnd);
        drawGradientRect(x - 3, y - 3, x + width + 3, y - 2, borderStart, borderStart);
        drawGradientRect(x - 3, y + height + 2, x + width + 3, y + height + 3, borderEnd, borderEnd);
        drawGradientRect(x - 3, y - 2, x - 2, y + height + 2, borderStart, borderEnd);
        drawGradientRect(x + width + 2, y - 2, x + width + 3, y + height + 2, borderStart, borderEnd);
    }
}
