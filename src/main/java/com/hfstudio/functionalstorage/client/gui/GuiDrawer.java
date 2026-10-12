package com.hfstudio.functionalstorage.client.gui;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;

public class GuiDrawer extends GuiStorage {

    private GuiTextField priority;
    private final DrawerInfoPanel info;
    private int pendingUpgradeSlot = -1;

    public GuiDrawer(EntityPlayer player, ControllableDrawerTile tile) {
        super(tile, new ContainerDrawer(tile, player));
        info = new DrawerInfoPanel(tile);
        xSize = 176;
        ySize = layout.height();
    }

    protected boolean showsDrawerFace() {
        return tile.getActiveStorage() != null && tile.getActiveStorage()
            .getStorageCount() <= 4;
    }

    protected boolean showsPriority() {
        return true;
    }

    protected String storageUpgradeLabel() {
        return "key.categories.storage";
    }

    @Override
    public void initGui() {
        super.initGui();
        priority = new GuiTextField(fontRendererObj, guiLeft + 115, guiTop + 30, 50, 16);
        priority.setMaxStringLength(9);
        priority.setText(Integer.toString(tile.getPriority()));
        priority.setVisible(showsPriority());
        priority.setEnabled(showsPriority());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        priority.updateCursorCounter();
        if (!priority.isFocused()) priority.setText(Integer.toString(tile.getPriority()));
    }

    @Override
    public boolean isTextInputFocused() {
        return priority != null && priority.isFocused();
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (priority.isFocused()
            && (Character.isDigit(character) || character < 32 || key == 211 || key == 203 || key == 205)) {
            String previous = priority.getText();
            if (priority.textboxKeyTyped(character, key)) {
                String value = priority.getText();
                if (!value.chars()
                    .allMatch(Character::isDigit)) priority.setText(previous);
                else FunctionalStorage.network.sendToServer(
                    new MenuSettingsMessage(
                        inventorySlots.windowId,
                        value.isEmpty() ? 0 : Integer.parseInt(value),
                        ""));
                return;
            }
        }
        if (key == 1 || !priority.isFocused()) super.keyTyped(character, key);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        DrawerGuiTextures textures = DrawerGuiTextures.INSTANCE;
        textures.panel(guiLeft, guiTop, xSize, ySize);
        boolean drawerFace = showsDrawerFace();
        if (drawerFace) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            info.drawFluid(guiLeft + 64, guiTop + 16);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            textures.front(tile, guiLeft + 64, guiTop + 16);
            GL11.glPopAttrib();
        }
        int storageSlots = ((ContainerDrawer) inventorySlots).getVisibleStorageSlots();
        for (int index = 0; index < inventorySlots.inventorySlots.size(); index++) {
            Slot slot = inventorySlots.inventorySlots.get(index);
            if (drawerFace && index < storageSlots) continue;
            textures.slot(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition);
        }
        priority.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawLabels(160);
        if (priority.getVisible()) fontRendererObj
            .drawString(StatCollector.translateToLocal("gui.functionalstorage.priority"), 114, 20, LABEL_COLOUR);
        if (tile.getStorageUpgradeSlots() > 0) fontRendererObj.drawString(
            StatCollector.translateToLocal(storageUpgradeLabel()),
            10,
            layout.upgradeY() - 11,
            LABEL_COLOUR);
        if (tile.getUtilityUpgradeSlots() > 0) fontRendererObj.drawString(
            StatCollector.translateToLocal("key.categories.utility"),
            114,
            layout.upgradeY() - 11,
            LABEL_COLOUR);
        if (showsDrawerFace()) drawEntries();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (priority.getVisible()) priority.mouseClicked(mouseX, mouseY, button);
        if (priority.isFocused()) {
            consumeMouseButton(button);
            return;
        }
        if (button == 1 && mc.thePlayer.inventory.getItemStack() == null
            && !(Mods.NotEnoughItems.isModLoaded() && NEIGuiIntegration.isDragging())) {
            int slot = upgradeSlotAt(mouseX, mouseY);
            if (slot >= 0) {
                consumeMouseButton(button);
                pendingUpgradeSlot = slot;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    private int upgradeSlotAt(int mouseX, int mouseY) {
        for (int slot = 0; slot < tile.getUtilityUpgradeSlots(); slot++) {
            int x = guiLeft + 114 + slot * 18;
            int y = guiTop + layout.upgradeY();
            ItemStack stack = tile.getUtilityUpgrade(slot);
            if (mouseX >= x && mouseX < x + 16
                && mouseY >= y
                && mouseY < y + 16
                && stack != null
                && (stack.getItem() instanceof AutomationUpgradeItem
                    || stack.getItem() instanceof RedstoneUpgradeItem)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void onConsumedMouseReleased(int x, int y, int button) {
        if (button != 1) return;
        int slot = pendingUpgradeSlot;
        pendingUpgradeSlot = -1;
        if (slot >= 0 && slot == upgradeSlotAt(x, y) && mc.thePlayer.inventory.getItemStack() == null) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, GuiHandler.GUI_UPGRADE_BASE + slot);
        }
    }

    @Nonnull
    public static ResourceLocation backgroundTexture() {
        return DrawerGuiTextures.BACKGROUND;
    }

}
