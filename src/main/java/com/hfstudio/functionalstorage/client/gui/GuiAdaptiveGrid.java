package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutInput;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.DrawerGuiLayout;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.common.network.StorageSearchMessage;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

import cpw.mods.fml.client.config.GuiButtonExt;

public class GuiAdaptiveGrid extends GuiStorage {

    private static final int CONTROL_HEIGHT = 14;
    private static final int CONTROL_WIDTH = 40;
    private static final int CONTROL_PITCH = CONTROL_WIDTH + 2;

    private GuiButton adaptiveModeButton;
    private GuiButton adaptiveSortButton;
    private GuiButton adaptiveLayoutButton;
    private GuiButton adaptiveOrderButton;
    private GuiTextField search;
    private boolean scrolling;
    private int requestedRow;
    private int searchRevision;
    private String lastQuery = "";
    private int[] matchedResources = new int[0];
    private Predicate<ItemStack> itemSearch;

    public GuiAdaptiveGrid(EntityPlayer player, AdaptiveGridTile tile) {
        this(tile, new ContainerDrawer(tile, player));
    }

    protected GuiAdaptiveGrid(AdaptiveGridTile tile, ContainerDrawer container) {
        super(tile, container);
        xSize = DrawerGuiLayout.TERMINAL_WIDTH;
        ySize = layout.height();
    }

    protected boolean isCraftingGrid() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        search = new GuiTextField(
            fontRendererObj,
            guiLeft + DrawerGuiLayout.TERMINAL_SEARCH_X,
            guiTop + DrawerGuiLayout.TERMINAL_SEARCH_Y,
            DrawerGuiLayout.TERMINAL_SEARCH_WIDTH,
            DrawerGuiLayout.TERMINAL_SEARCH_HEIGHT);
        search.setMaxStringLength(50);
        search.setEnableBackgroundDrawing(false);
        if (!isCraftingGrid()) {
            // Buttons sit in the strip that adaptivePanel stretches open between
            // the resource grid and the player inventory.
            int y = guiTop + DrawerGuiLayout.TERMINAL_STRIP_Y + 2;
            adaptiveModeButton = new GuiButtonExt(200, guiLeft + 6, y, CONTROL_WIDTH, CONTROL_HEIGHT, "");
            adaptiveSortButton = new GuiButtonExt(
                201,
                guiLeft + 6 + CONTROL_PITCH,
                y,
                CONTROL_WIDTH,
                CONTROL_HEIGHT,
                "");
            adaptiveLayoutButton = new GuiButtonExt(
                202,
                guiLeft + 6 + CONTROL_PITCH * 2,
                y,
                CONTROL_WIDTH,
                CONTROL_HEIGHT,
                "");
            adaptiveOrderButton = new GuiButtonExt(
                203,
                guiLeft + 6 + CONTROL_PITCH * 3,
                y,
                CONTROL_WIDTH,
                CONTROL_HEIGHT,
                "");
            buttonList.add(adaptiveModeButton);
            buttonList.add(adaptiveSortButton);
            buttonList.add(adaptiveLayoutButton);
            buttonList.add(adaptiveOrderButton);
            updateAdaptiveModeButton();
        }
        updateSearch(0);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        search.updateCursorCounter();
        if (!search.getText()
            .trim()
            .toLowerCase(Locale.ROOT)
            .equals(lastQuery) || searchRevision != menu().getSearchRevision()) updateSearch(menu().getScrollRow());
        updateAdaptiveModeButton();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == adaptiveModeButton) {
            FunctionalStorage.network
                .sendToServer(new MenuSettingsMessage(inventorySlots.windowId, 0, "adaptive_mode_next"));
            return;
        }
        if (button == adaptiveSortButton || button == adaptiveLayoutButton || button == adaptiveOrderButton) {
            String setting = button == adaptiveSortButton ? "adaptive_sort_next"
                : button == adaptiveLayoutButton ? "adaptive_layout_next" : "adaptive_order_next";
            FunctionalStorage.network.sendToServer(new MenuSettingsMessage(inventorySlots.windowId, 0, setting));
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    public boolean isTextInputFocused() {
        return search != null && search.isFocused();
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (search.isFocused() && key != 1) {
            String previous = search.getText();
            if (search.textboxKeyTyped(character, key)) {
                if (!previous.equals(search.getText())) updateSearch(0);
                return;
            }
        }
        if (key == 1 || !search.isFocused()) super.keyTyped(character, key);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        DrawerGuiTextures textures = DrawerGuiTextures.INSTANCE;
        textures.adaptivePanel(guiLeft, guiTop, xSize, ySize, isCraftingGrid());
        for (int row = 0; row < DrawerGuiLayout.TERMINAL_ROWS; row++) {
            textures.adaptiveRow(
                guiLeft + DrawerGuiLayout.TERMINAL_GRID_X - 1,
                guiTop + DrawerGuiLayout.TERMINAL_GRID_Y - 1 + row * 18);
        }
        if (search.isFocused()) {
            int searchLeft = guiLeft + DrawerGuiLayout.TERMINAL_SEARCH_X;
            int searchTop = guiTop + DrawerGuiLayout.TERMINAL_SEARCH_Y;
            drawRect(
                searchLeft,
                searchTop,
                searchLeft + DrawerGuiLayout.TERMINAL_SEARCH_WIDTH,
                searchTop + DrawerGuiLayout.TERMINAL_SEARCH_HEIGHT,
                0x55333333);
        }
        search.drawTextBox();
        drawScrollbar();

    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawLabels(DrawerGuiLayout.TERMINAL_SEARCH_X - 10);
        drawTerminalForeground();
    }

    private void drawTerminalForeground() {
        if (!menu().isOnline()) {
            String offline = StatCollector.translateToLocal("functionalstorage.gui.adaptive.offline");
            int centreX = DrawerGuiLayout.TERMINAL_GRID_X + DrawerGuiLayout.TERMINAL_COLUMNS * 9;
            int centreY = DrawerGuiLayout.TERMINAL_GRID_Y + DrawerGuiLayout.TERMINAL_ROWS * 9 - 4;
            fontRendererObj
                .drawString(offline, centreX - fontRendererObj.getStringWidth(offline) / 2, centreY, 0xAA3333);
            return;
        }
        drawEntries();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Mouse.isButtonDown(0)) scrolling = false;
        if (scrolling) scrollTo(mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawControlTooltip(mouseX, mouseY);
    }

    private void drawControlTooltip(int mouseX, int mouseY) {
        if (adaptiveModeButton == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (adaptiveModeButton.func_146115_a()) {
            lines.add(StatCollector.translateToLocal("functionalstorage.gui.adaptive.mode.button"));
            lines.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocal(
                    menu().isOnline() ? "functionalstorage.gui.adaptive.online"
                        : "functionalstorage.gui.adaptive.offline"));
            if (AdaptiveMatrixTile.usesRf()) {
                lines.add(
                    EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                        "functionalstorage.gui.adaptive.energy",
                        StorageAmountFormatter.formatDisplay(menu().getRfStored(), false),
                        StorageAmountFormatter.formatDisplay(menu().getRfCapacity(), false),
                        StorageAmountFormatter.formatDisplay(menu().getRfCost(), false)));
            }
            if (menu().getUnassignedDisplayResources() > 0) {
                lines.add(
                    EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                        "functionalstorage.gui.adaptive.unassigned",
                        menu().getUnassignedDisplayResources()));
            }
        } else if (adaptiveSortButton.func_146115_a()) {
            lines.add(StatCollector.translateToLocal("functionalstorage.gui.adaptive.sort.button"));
        } else if (adaptiveLayoutButton.func_146115_a()) {
            lines.add(StatCollector.translateToLocal("functionalstorage.gui.adaptive.layout.button"));
        } else if (adaptiveOrderButton.func_146115_a()) {
            lines.add(StatCollector.translateToLocal("functionalstorage.gui.adaptive.order.button"));
        }
        if (!lines.isEmpty()) {
            drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        search.mouseClicked(mouseX, mouseY, button);
        if (search.isFocused()) {
            consumeMouseButton(button);
            return;
        }
        if (button == 0 && mouseX >= guiLeft + DrawerGuiLayout.TERMINAL_SCROLLBAR_X
            && mouseX < guiLeft + DrawerGuiLayout.TERMINAL_SCROLLBAR_X + DrawerGuiLayout.TERMINAL_SCROLLBAR_WIDTH
            && mouseY >= guiTop + DrawerGuiLayout.TERMINAL_SCROLLBAR_Y
            && mouseY < guiTop + DrawerGuiLayout.TERMINAL_SCROLLBAR_Y
                + DrawerGuiLayout.TERMINAL_SCROLLBAR_TRAVEL
                + DrawerGuiLayout.TERMINAL_SCROLLBAR_HEIGHT) {
            scrolling = true;
            scrollTo(mouseY);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !StorageShortcutInput.handlesWheel(this)) {
            updateSearch(menu().getScrollRow() + (wheel > 0 ? -1 : 1));
        }
    }

    private void scrollTo(int mouseY) {
        int max = menu().getMaxScrollRow();
        // The handle centre tracks the cursor, so the usable track starts half a
        // handle below the top of the bar.
        float top = guiTop + DrawerGuiLayout.TERMINAL_SCROLLBAR_Y + DrawerGuiLayout.TERMINAL_SCROLLBAR_HEIGHT / 2F;
        int row = max == 0 ? 0 : Math.round((mouseY - top) / DrawerGuiLayout.TERMINAL_SCROLLBAR_TRAVEL * max);
        row = Math.max(0, Math.min(max, row));
        if (row != requestedRow) updateSearch(row);
    }

    private void drawScrollbar() {
        int max = menu().getMaxScrollRow();
        int row = menu().getScrollRow();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        ResourceLocation scrollTexture = max == 0 ? DrawerGuiTextures.ADAPTIVE_SCROLLBAR_DISABLED
            : scrolling ? DrawerGuiTextures.ADAPTIVE_SCROLLBAR_CLICKED : DrawerGuiTextures.ADAPTIVE_SCROLLBAR;
        mc.getTextureManager()
            .bindTexture(scrollTexture);
        int offset = max == 0 ? 0 : Math.round((float) DrawerGuiLayout.TERMINAL_SCROLLBAR_TRAVEL * row / max);
        func_152125_a(
            guiLeft + DrawerGuiLayout.TERMINAL_SCROLLBAR_X,
            guiTop + DrawerGuiLayout.TERMINAL_SCROLLBAR_Y + offset,
            0,
            0,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_WIDTH,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_HEIGHT,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_WIDTH,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_HEIGHT,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_WIDTH,
            DrawerGuiLayout.TERMINAL_SCROLLBAR_HEIGHT);
    }

    private void updateSearch(int row) {
        ContainerDrawer menu = menu();
        if (!menu.isSnapshotReady()) return;
        requestedRow = Math.max(0, Math.min(row, menu.getMaxScrollRow()));
        String text = search.getText()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            lastQuery = "";
            matchedResources = new int[0];
            searchRevision = menu.getSearchRevision();
            FunctionalStorage.network.sendToServer(
                new StorageSearchMessage(
                    inventorySlots.windowId,
                    requestedRow,
                    "",
                    matchedResources,
                    menu.getSession(),
                    menu.getViewRevision()));
            return;
        }
        if (!text.equals(lastQuery) || searchRevision != menu.getSearchRevision()) {
            int count = menu.getTransferResourceCount();
            int[] matches = new int[count];
            int matched = 0;
            if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(menu.getViewMode())
                || AdaptiveMatrixTile.TERMINAL_ALL.equals(menu.getViewMode())) {
                itemSearch = Mods.NotEnoughItems.isModLoaded() ? NEIGuiIntegration.search(text)
                    : item -> matchesItem(item, text);
            }
            for (int index = 0; index < count; index++) {
                if (matchesResource(menu, index, text)) matches[matched++] = index;
            }
            matchedResources = Arrays.copyOf(matches, matched);
            lastQuery = text;
            searchRevision = menu.getSearchRevision();
        }
        FunctionalStorage.network.sendToServer(
            new StorageSearchMessage(
                inventorySlots.windowId,
                requestedRow,
                text,
                matchedResources,
                menu.getSession(),
                menu.getViewRevision()));
    }

    private boolean matchesResource(ContainerDrawer menu, int index, String text) {
        BigItemStack item = menu.getViewItemSnapshot(index);
        if (item.hasTemplate()) return itemSearch != null && itemSearch.test(item.getTemplate());
        BigFluidStack fluid = menu.getViewFluidSnapshot(index);
        if (fluid.hasTemplate()) return (fluid.getTemplate()
            .getLocalizedName() + " "
            + fluid.getTemplate()
                .getFluid()
                .getName()
            + " "
            + FluidRegistry.getDefaultFluidName(
                fluid.getTemplate()
                    .getFluid())).toLowerCase(Locale.ROOT)
                        .contains(text);
        BigAspectStack aspect = menu.getViewAspectSnapshot(index);
        return aspect.hasTemplate() && (aspect.getAspect()
            .getName() + " "
            + aspect.getAspect()
                .getLocalizedDescription()
            + " "
            + aspect.getAspect()
                .getTag()
            + " thaumcraft").toLowerCase(Locale.ROOT)
                .contains(text);
    }

    private boolean matchesItem(ItemStack item, String text) {
        if (item.getDisplayName()
            .toLowerCase(Locale.ROOT)
            .contains(text)
            || Item.itemRegistry.getNameForObject(item.getItem())
                .toLowerCase(Locale.ROOT)
                .contains(text))
            return true;
        for (String line : item.getTooltip(mc.thePlayer, false)) {
            if (line.toLowerCase(Locale.ROOT)
                .contains(text)) return true;
        }
        return false;
    }

    private void updateAdaptiveModeButton() {
        if (adaptiveModeButton == null) return;
        // The strip is narrow, so a button shows only its current value; the
        // category it belongs to is named in its hover tooltip.
        adaptiveModeButton.displayString = buttonLabel("mode", menu().getViewMode());
        adaptiveSortButton.displayString = buttonLabel("presentation.sort", menu().getSortId());
        adaptiveLayoutButton.displayString = buttonLabel("presentation.layout", menu().getLayoutId());
        adaptiveOrderButton.displayString = buttonLabel("presentation.order", menu().getOrderId());
    }

    private String buttonLabel(String group, String id) {
        return fontRendererObj.trimStringToWidth(
            StatCollector.translateToLocal("functionalstorage.gui.adaptive." + group + "." + id),
            CONTROL_WIDTH - 6);
    }

}
