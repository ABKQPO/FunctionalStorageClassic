package com.hfstudio.functionalstorage.client.gui;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutInput;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutScreen;
import com.hfstudio.functionalstorage.common.container.ContainerArmory;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.network.ArmorySearchMessage;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.common.tile.ArmoryCabinetTile;

public class GuiArmory extends GuiContainer implements StorageShortcutScreen {

    private static final ResourceLocation SCROLL = new ResourceLocation(
        "minecraft",
        "textures/gui/container/creative_inventory/tabs.png");
    private static final ResourceLocation TRACK = new ResourceLocation(
        "minecraft",
        "textures/gui/container/creative_inventory/tab_items.png");
    private final ContainerArmory container;
    private GuiTextField search;
    private boolean scrolling;
    private int requestedRow;
    private int searchRevision;
    private String lastQuery = "";
    private int[] matchedSlots = new int[0];
    private Predicate<ItemStack> searchFilter;

    public GuiArmory(EntityPlayer player, ArmoryCabinetTile tile) {
        super(new ContainerArmory(tile, player));
        container = (ContainerArmory) inventorySlots;
        xSize = 176;
        ySize = 184;
    }

    @Override
    public void initGui() {
        super.initGui();
        search = new GuiTextField(fontRendererObj, guiLeft + 96, guiTop + 5, 72, 12);
        search.setMaxStringLength(50);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        search.updateCursorCounter();
        if (!search.getText()
            .isEmpty() && searchRevision != container.getSearchRevision()) update(container.getScrollRow());
    }

    private void update(int row) {
        requestedRow = Math.max(0, Math.min(row, container.getMaxScrollRow()));
        String query = search.getText()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            lastQuery = "";
            FunctionalStorage.network.sendToServer(new MenuSettingsMessage(inventorySlots.windowId, requestedRow, ""));
            return;
        }
        if (!query.equals(lastQuery) || searchRevision != container.getSearchRevision()) {
            if (!query.equals(lastQuery) || searchFilter == null) {
                searchFilter = Mods.NotEnoughItems.isModLoaded() ? NEIGuiIntegration.search(query)
                    : item -> matches(item, query);
            }
            int[] matches = new int[container.getTile()
                .getItemHandler()
                .getStorageCount()];
            int count = 0;
            for (int slot = 0; slot < matches.length; slot++) {
                BigItemStack stored = container.getTile()
                    .getItemHandler()
                    .getSnapshot(slot);
                if (stored.hasTemplate() && searchFilter.test(stored.getTemplate())) matches[count++] = slot;
            }
            matchedSlots = Arrays.copyOf(matches, count);
            lastQuery = query;
            searchRevision = container.getSearchRevision();
        }
        FunctionalStorage.network
            .sendToServer(new ArmorySearchMessage(inventorySlots.windowId, requestedRow, query, matchedSlots));
    }

    private boolean matches(ItemStack item, String query) {
        if (Item.itemRegistry.getNameForObject(item.getItem())
            .toLowerCase(Locale.ROOT)
            .contains(query)) return true;
        for (String line : item.getTooltip(mc.thePlayer, false)) {
            if (line.toLowerCase(Locale.ROOT)
                .contains(query)) return true;
        }
        return false;
    }

    @Override
    protected void keyTyped(char character, int key) {
        String previous = search.getText();
        if (key != 1 && search.textboxKeyTyped(character, key)) {
            if (!previous.equals(search.getText())) update(0);
        } else super.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        search.mouseClicked(x, y, button);
        if (search.isFocused()) return;
        if (button == 0 && x >= guiLeft + 158 && x < guiLeft + 170 && y >= guiTop + 18 && y < guiTop + 90) {
            scrolling = true;
            scroll(y);
            return;
        }
        super.mouseClicked(x, y, button);
    }

    private void scroll(int y) {
        int row = Math.round((y - guiTop - 25.5F) / 57F * container.getMaxScrollRow());
        if (row != requestedRow) update(row);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !StorageShortcutInput.handlesWheel(this)) {
            update(container.getScrollRow() + (wheel > 0 ? -1 : 1));
        }
    }

    @Override
    public boolean isTextInputFocused() {
        return search != null && search.isFocused();
    }

    @Override
    public Slot getSlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
    }

    @Override
    public void drawScreen(int x, int y, float partial) {
        if (!Mouse.isButtonDown(0)) scrolling = false;
        if (scrolling) scroll(y);
        super.drawScreen(x, y, partial);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        DrawerGuiTextures.INSTANCE.panel(guiLeft, guiTop, xSize, ySize);
        for (Slot slot : inventorySlots.inventorySlots)
            DrawerGuiTextures.INSTANCE.slot(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition);
        search.drawTextBox();
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(TRACK);
        drawTexturedModalRect(guiLeft + 157, guiTop + 17, 174, 17, 14, 1);
        func_152125_a(guiLeft + 157, guiTop + 18, 174, 18, 14, 110, 14, 72, 256, 256);
        drawTexturedModalRect(guiLeft + 157, guiTop + 90, 174, 128, 14, 1);
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(SCROLL);
        int offset = container.getMaxScrollRow() == 0 ? 0
            : Math.round(57F * container.getScrollRow() / container.getMaxScrollRow());
        drawTexturedModalRect(
            guiLeft + 158,
            guiTop + 18 + offset,
            container.getMaxScrollRow() == 0 ? 244 : 232,
            0,
            12,
            15);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRendererObj
            .drawString(StatCollector.translateToLocal("tile.functionalstorage.armory_cabinet.name"), 8, 6, 0x404040);
        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, 92, 0x404040);
    }
}
