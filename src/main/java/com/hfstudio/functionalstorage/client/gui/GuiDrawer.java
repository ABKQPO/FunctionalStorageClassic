package com.hfstudio.functionalstorage.client.gui;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutScreen;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.DrawerGuiLayout;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;

public class GuiDrawer extends GuiContainer implements StorageShortcutScreen {

    private static final int WINDOW_WIDTH = 176;
    private static final int LABEL_COLOUR = 0x404040;

    private GuiTextField priority;
    private final ControllableDrawerTile tile;
    private final DrawerGuiLayout layout;
    private final DrawerInfoPanel info;
    private final DrawerTooltipRenderer previews = new DrawerTooltipRenderer();

    public GuiDrawer(@Nonnull EntityPlayer player, @Nonnull ControllableDrawerTile tile) {
        super(new ContainerDrawer(tile, player));
        this.tile = tile;
        this.info = new DrawerInfoPanel(tile);
        this.layout = ((ContainerDrawer) inventorySlots).getLayout();
        this.xSize = WINDOW_WIDTH;
        this.ySize = layout.height();
    }

    @Override
    public void initGui() {
        super.initGui();
        priority = new GuiTextField(fontRendererObj, guiLeft + 115, guiTop + 30, 50, 16);
        priority.setMaxStringLength(9);
        priority.setText(Integer.toString(tile.getPriority()));
        priority.setVisible(!(tile instanceof StorageNetworkTile));
        priority.setEnabled(!(tile instanceof StorageNetworkTile));
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
    public Slot getSlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
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
        priority.drawTextBox();
        boolean drawerFace = !(tile instanceof StorageNetworkTile) && tile.getActiveStorage() != null
            && tile.getActiveStorage()
                .getStorageCount() <= 4;
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
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(
                StatCollector.translateToLocal(
                    tile.getBlockType()
                        .getUnlocalizedName() + ".name"),
                160),
            8,
            6,
            LABEL_COLOUR);
        if (priority.getVisible()) fontRendererObj
            .drawString(StatCollector.translateToLocal("gui.functionalstorage.priority"), 114, 20, LABEL_COLOUR);
        if (tile instanceof DrawerControllerTile) fontRendererObj.drawString(
            StatCollector.translateToLocal("gui.functionalstorage.storage_range"),
            10,
            layout.upgradeY() - 11,
            LABEL_COLOUR);
        else if (tile.getStorageUpgradeSlots() > 0) fontRendererObj.drawString(
            StatCollector.translateToLocal("key.categories.storage"),
            10,
            layout.upgradeY() - 11,
            LABEL_COLOUR);
        if (tile.getUtilityUpgradeSlots() > 0) fontRendererObj.drawString(
            StatCollector.translateToLocal("key.categories.utility"),
            114,
            layout.upgradeY() - 11,
            LABEL_COLOUR);
        fontRendererObj.drawString(
            StatCollector.translateToLocal("container.inventory"),
            8,
            layout.inventoryY() - 12,
            LABEL_COLOUR);
        if (tile instanceof StorageNetworkTile || tile.getActiveStorage() == null) {
            return;
        }
        for (int slot = 0; slot < Math.min(
            36,
            tile.getActiveStorage()
                .getStorageCount()); slot++) {
            Entry entry = entry(slot);
            if (entry == null) {
                continue;
            }
            int x = contentX(slot);
            int y = contentY(slot);
            if (entry.aspect() != null || entry.item() != null && tile.getActiveStorage()
                .getSnapshot(slot)
                .getAmount() == 0) {
                previews.drawEntry(new Entry(entry.item(), entry.fluid(), entry.aspect(), ""), x, y);
            }
            String amount = entry.amount() + "/"
                + formatAmount(
                    tile.getActiveStorage()
                        .getCapacity(slot));
            GL11.glPushMatrix();
            GL11.glTranslatef(0F, 0F, 200F);
            float scale = Math.min(0.72F, 33.12F / Math.max(1, fontRendererObj.getStringWidth(amount)));
            GL11.glTranslatef(x + 8, y + 16, 0);
            GL11.glScalef(scale, scale, 1F);
            fontRendererObj.drawStringWithShadow(amount, -fontRendererObj.getStringWidth(amount) / 2, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tile instanceof StorageNetworkTile || tile.getActiveStorage() == null) {
            return;
        }
        for (int slot = 0; slot < Math.min(
            36,
            tile.getActiveStorage()
                .getStorageCount()); slot++) {
            int x = guiLeft + contentX(slot);
            int y = guiTop + contentY(slot);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                Entry entry = entry(slot);
                String name = entry == null ? StatCollector.translateToLocal("gui.functionalstorage.empty")
                    : entry.name();
                String amount = NumberFormatUtil.formatNumber(
                    tile.getActiveStorage()
                        .getSnapshot(slot)
                        .getAmount())
                    + "/"
                    + NumberFormatUtil.formatNumber(
                        tile.getActiveStorage()
                            .getCapacity(slot))
                    + (tile.getFluidHandler() == null ? "" : " " + NumberFormatUtil.getFluidUnit());
                drawHoveringText(
                    List.of(
                        name,
                        StatCollector.translateToLocal("gui.functionalstorage.amount") + amount,
                        StatCollector.translateToLocal("gui.functionalstorage.slot") + slot),
                    mouseX,
                    mouseY,
                    fontRendererObj);
                break;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (priority.getVisible()) priority.mouseClicked(mouseX, mouseY, button);
        if (priority.isFocused()) return;
        if (button == 1 && mc.thePlayer.inventory.getItemStack() == null) {
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
                    mc.playerController.sendEnchantPacket(inventorySlots.windowId, GuiHandler.GUI_UPGRADE_BASE + slot);
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    private int contentX(int slot) {
        return tile.getItemHandler() != null ? layout.storageX(slot)
            : 64 + Math.round(
                ((DrawerBlock) tile.getBlockType()).getFaceLayout()
                    .getSlotX(slot) * 48F)
                - 8;
    }

    private int contentY(int slot) {
        return tile.getItemHandler() != null ? layout.storageY(slot)
            : 16 + Math.round(
                ((DrawerBlock) tile.getBlockType()).getFaceLayout()
                    .getSlotY(slot) * 48F)
                - 8;
    }

    private String formatAmount(long amount) {
        return tile.getFluidHandler() == null ? NumberFormatUtil.formatNumberCompact(amount)
            : NumberFormatUtil.formatFluid(amount);
    }

    private Entry entry(int slot) {
        if (tile.getItemHandler() != null) {
            BigItemStack stored = tile.getItemHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(stored.getTemplate(), null, null, NumberFormatUtil.formatNumberCompact(stored.getAmount()))
                : null;
        }
        if (tile.getFluidHandler() != null) {
            BigFluidStack stored = tile.getFluidHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, stored.getTemplate(), null, NumberFormatUtil.formatFluid(stored.getAmount()))
                : null;
        }
        if (tile.getAspectHandler() != null) {
            BigAspectStack stored = tile.getAspectHandler()
                .getSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    null,
                    AspectIcon.of(stored.getAspect()),
                    NumberFormatUtil.formatNumberCompact(stored.getAmount()))
                : null;
        }
        return null;
    }

    @Nonnull
    public static ResourceLocation backgroundTexture() {
        return DrawerGuiTextures.BACKGROUND;
    }
}
