package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IStorageHandler;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutInput;
import com.hfstudio.functionalstorage.client.integration.StorageShortcutScreen;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.DrawerGuiLayout;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RedstoneUpgradeItem;
import com.hfstudio.functionalstorage.common.network.MenuSettingsMessage;
import com.hfstudio.functionalstorage.common.network.StorageSearchMessage;
import com.hfstudio.functionalstorage.common.network.StorageTransferMessage;
import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;
import com.hfstudio.functionalstorage.common.tile.AdaptiveGridTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;
import com.hfstudio.functionalstorage.misc.GuiHandler;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;

public class GuiDrawer extends GuiContainer implements StorageShortcutScreen {

    private static final int WINDOW_WIDTH = 176;
    private static final int LABEL_COLOUR = 0x404040;

    private GuiTextField priority;
    private final ControllableDrawerTile tile;
    private final DrawerGuiLayout layout;
    private final DrawerInfoPanel info;
    private final StorageTooltipPainter previews = StorageTooltipPainter.INSTANCE;
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
    private int consumedMouseButtons;
    private int pendingUpgradeSlot = -1;

    public GuiDrawer(@Nonnull EntityPlayer player, @Nonnull ControllableDrawerTile tile) {
        this(player, tile, new ContainerDrawer(tile, player));
    }

    protected GuiDrawer(EntityPlayer player, ControllableDrawerTile tile, ContainerDrawer container) {
        super(container);
        this.tile = tile;
        this.info = new DrawerInfoPanel(tile);
        this.layout = ((ContainerDrawer) inventorySlots).getLayout();
        this.xSize = tile instanceof AdaptiveMatrixTile || tile instanceof AdaptiveGridTile ? 193 : WINDOW_WIDTH;
        this.ySize = tile instanceof AdaptiveCraftingGridTile ? 229 : layout.height();
    }

    @Nonnull
    public ControllableDrawerTile getTile() {
        return tile;
    }

    @Override
    public void initGui() {
        super.initGui();
        priority = new GuiTextField(fontRendererObj, guiLeft + 115, guiTop + 30, 50, 16);
        priority.setMaxStringLength(9);
        priority.setText(Integer.toString(tile.getPriority()));
        priority.setVisible(!(tile instanceof StorageNetworkTile));
        priority.setEnabled(!(tile instanceof StorageNetworkTile));
        if (tile instanceof AdaptiveMatrixTile || tile instanceof AdaptiveGridTile) {
            search = new GuiTextField(fontRendererObj, guiLeft + 95, guiTop + 7, 66, 12);
            search.setMaxStringLength(50);
            search.setEnableBackgroundDrawing(false);
        }
        if ((tile instanceof AdaptiveMatrixTile || tile instanceof AdaptiveGridTile)
            && !(tile instanceof AdaptiveCraftingGridTile)) {
            int y = guiTop + 94;
            adaptiveModeButton = new GuiButton(200, guiLeft + 8, y, 78, 20, "");
            adaptiveSortButton = new GuiButton(201, guiLeft + 90, y, 78, 20, "");
            adaptiveLayoutButton = new GuiButton(202, guiLeft + 8, y + 22, 78, 20, "");
            adaptiveOrderButton = new GuiButton(203, guiLeft + 90, y + 22, 78, 20, "");
            buttonList.add(adaptiveModeButton);
            buttonList.add(adaptiveSortButton);
            buttonList.add(adaptiveLayoutButton);
            buttonList.add(adaptiveOrderButton);
            updateAdaptiveModeButton();
        }
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        if (menu.isTransferViewVirtual()) {
            updateSearch(0);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        priority.updateCursorCounter();
        if (search != null) {
            search.updateCursorCounter();
            if (!search.getText()
                .trim()
                .toLowerCase(Locale.ROOT)
                .equals(lastQuery) || searchRevision != menu().getSearchRevision()) {
                updateSearch(menu().getScrollRow());
            }
        }
        if (!priority.isFocused()) priority.setText(Integer.toString(tile.getPriority()));
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
        return priority != null && priority.isFocused() || search != null && search.isFocused();
    }

    @Override
    public Slot getSlotAt(int mouseX, int mouseY) {
        for (Slot slot : inventorySlots.inventorySlots) {
            if (func_146978_c(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY)) return slot;
        }
        return null;
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int button, int mode) {
        ContainerDrawer menu = menu();
        if (menu.isTransferViewVirtual() && slot != null
            && slotId >= 0
            && slotId < menu.getTransferSlotCount()
            && mode != 5) {
            if (!menu.hasTransferStorage()) return;
            FunctionalStorage.network.sendToServer(
                StorageTransferMessage
                    .click(inventorySlots.windowId, slotId, button, mode, menu.getSession(), menu.getViewRevision()));
            return;
        }
        super.handleMouseClick(slot, slotId, button, mode);
    }

    public int getFilterSlotAt(int mouseX, int mouseY) {
        if (!tile.isLocked() || tile.getItemHandler() == null) return -1;
        Slot slot = storageSlotAt(mouseX, mouseY);
        return slot == null ? -1 : slot.getSlotIndex();
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (search != null && search.isFocused() && key != 1) {
            String previous = search.getText();
            if (search.textboxKeyTyped(character, key)) {
                if (!previous.equals(search.getText())) updateSearch(0);
                return;
            }
        }
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
        if (key == 1 || !priority.isFocused() && (search == null || !search.isFocused()))
            super.keyTyped(character, key);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        DrawerGuiTextures textures = DrawerGuiTextures.INSTANCE;
        if (search != null) textures.adaptivePanel(
            guiLeft,
            guiTop,
            xSize,
            ySize,
            tile instanceof AdaptiveCraftingGridTile,
            ((ContainerDrawer) inventorySlots).getPlayerInventoryY());
        else textures.panel(guiLeft, guiTop, xSize, ySize);
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
            if (search != null) continue;
            textures.slot(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition);
        }
        priority.drawTextBox();
        if (search != null) search.drawTextBox();
        if (search != null) drawScrollbar();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(
                StatCollector.translateToLocal(
                    tile.getBlockType()
                        .getUnlocalizedName() + ".name"),
                search == null ? 160 : 84),
            8,
            6,
            LABEL_COLOUR);
        if (priority.getVisible()) fontRendererObj
            .drawString(StatCollector.translateToLocal("gui.functionalstorage.priority"), 114, 20, LABEL_COLOUR);
        if (tile instanceof DrawerControllerTile && search == null) fontRendererObj.drawString(
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
            ((ContainerDrawer) inventorySlots).getPlayerInventoryY() - 12,
            LABEL_COLOUR);
        if (tile instanceof StorageNetworkTile && !(tile instanceof AdaptiveMatrixTile)
            && !(tile instanceof AdaptiveGridTile)) {
            return;
        }
        if (search != null) {
            if (AdaptiveMatrixTile.usesRf()) {
                String energy = StatCollector.translateToLocalFormatted(
                    "functionalstorage.gui.adaptive.energy",
                    StorageAmountFormatter.formatDisplay(menu().getRfStored(), false),
                    StorageAmountFormatter.formatDisplay(menu().getRfCapacity(), false),
                    StorageAmountFormatter.formatDisplay(menu().getRfCost(), false));
                float scale = Math.min(1F, 160F / Math.max(1, fontRendererObj.getStringWidth(energy)));
                GL11.glPushMatrix();
                GL11.glTranslatef(8F, 130F, 0F);
                GL11.glScalef(scale, scale, 1F);
                fontRendererObj.drawString(energy, 0, 0, LABEL_COLOUR);
                GL11.glPopMatrix();
            }
            if (!menu().isOnline()) {
                String offline = StatCollector.translateToLocal("functionalstorage.gui.adaptive.offline");
                fontRendererObj.drawString(offline, 80 - fontRendererObj.getStringWidth(offline) / 2, 50, 0xAA3333);
                return;
            }
        } else if (displayStorage() == null) return;
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        for (int slot = 0; slot < menu.getVisibleStorageSlots(); slot++) {
            Entry entry = entry(slot);
            if (entry == null) {
                continue;
            }
            int x = contentX(slot);
            int y = contentY(slot);
            if (entry.fluid() != null || entry.aspect() != null || entry.item() != null && transferAmount(slot) == 0L) {
                previews.drawEntry(new Entry(entry.item(), entry.fluid(), entry.aspect(), ""), x, y);
            }
            String amount = search != null ? entry.amount()
                : entry.amount() + "/" + formatAmount(transferCapacity(slot));
            GL11.glPushMatrix();
            GL11.glTranslatef(0F, 0F, 200F);
            float scale = Math
                .min(0.72F, (search != null ? 16F : 33.12F) / Math.max(1, fontRendererObj.getStringWidth(amount)));
            GL11.glTranslatef(x + 8, y + (search != null ? 10 : 16), 0);
            GL11.glScalef(scale, scale, 1F);
            fontRendererObj.drawStringWithShadow(amount, -fontRendererObj.getStringWidth(amount) / 2, 0, 0xFFFFFF);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Mouse.isButtonDown(0)) scrolling = false;
        if (scrolling) scrollTo(mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (Mods.NotEnoughItems.isModLoaded()) {
            return;
        }
        if (mc.thePlayer.inventory.getItemStack() != null) {
            return;
        }
        Slot hovered = storageSlotAt(mouseX, mouseY);
        if (hovered == null || hovered.getHasStack()) {
            return;
        }
        List<String> lines = storageTooltipLines(mouseX, mouseY);
        if (!lines.isEmpty()) {
            drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void renderToolTip(@Nonnull ItemStack stack, int mouseX, int mouseY) {
        List<String> lines = storageTooltipLines(mouseX, mouseY);
        if (lines.isEmpty()) {
            super.renderToolTip(stack, mouseX, mouseY);
            return;
        }
        List<String> tooltip = new ArrayList<>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
        for (int index = 0; index < tooltip.size(); index++) {
            tooltip.set(
                index,
                (index == 0 ? stack.getRarity().rarityColor : EnumChatFormatting.GRAY) + tooltip.get(index));
        }
        tooltip.addAll(lines);
        FontRenderer font = stack.getItem()
            .getFontRenderer(stack);
        drawHoveringText(tooltip, mouseX, mouseY, font == null ? fontRendererObj : font);
    }

    public List<String> storageTooltipLines(int mouseX, int mouseY) {
        if (tile instanceof StorageNetworkTile && !(tile instanceof AdaptiveMatrixTile)
            && !(tile instanceof AdaptiveGridTile)) {
            return List.of();
        }
        if (search == null && displayStorage() == null) return List.of();
        Slot hovered = storageSlotAt(mouseX, mouseY);
        if (hovered == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (!hovered.getHasStack()) {
            Entry entry = entry(hovered.getSlotIndex());
            lines.add(entry == null ? StatCollector.translateToLocal("gui.functionalstorage.empty") : entry.name());
        }
        String mod = resourceModName(hovered.getSlotIndex());
        if (!mod.isEmpty()) lines.add(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.ITALIC + mod);
        lines.add(EnumChatFormatting.GRAY + amountLine(hovered.getSlotIndex()));
        if (menu().isTransferViewVirtual() && menu().getUnassignedDisplayResources() > 0) {
            lines.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                    "functionalstorage.gui.adaptive.unassigned",
                    menu().getUnassignedDisplayResources()));
        }
        if (isAspectTerminal()) {
            BigAspectStack aspect = menu().getTransferAspectSnapshot(hovered.getSlotIndex());
            if (aspect.hasTemplate()) lines.add(
                EnumChatFormatting.GRAY + aspect.getAspect()
                    .getLocalizedDescription());
        }
        if (mc.gameSettings.advancedItemTooltips) lines.add(
            EnumChatFormatting.GRAY + StatCollector.translateToLocal("gui.functionalstorage.slot")
                + hovered.getSlotIndex());
        return lines;
    }

    private String amountLine(int slot) {
        boolean fluid = isFluidTerminal();
        String amount = StorageAmountFormatter.formatTooltip(transferAmount(slot), fluid) + "/"
            + StorageAmountFormatter.formatTooltip(transferCapacity(slot), fluid);
        long capacity = transferCapacity(slot);
        String percent = capacity <= 0L ? ""
            : String.format(Locale.ROOT, " (%.1f%%)", Math.min(100D, 100D * transferAmount(slot) / capacity));
        return StatCollector.translateToLocal("gui.functionalstorage.amount") + amount + percent;
    }

    private Slot storageSlotAt(int mouseX, int mouseY) {
        Slot slot = getSlotAt(mouseX, mouseY);
        return slot instanceof ContainerDrawer.StorageSlot ? slot : null;
    }

    private String resourceModName(int slot) {
        if (isAspectTerminal()) return "Thaumcraft";
        ModContainer owner;
        if (isFluidTerminal()) {
            FluidStack fluid = menu().getTransferFluidSnapshot(slot)
                .getTemplate();
            if (fluid == null) return "";
            owner = GameData.findModOwner(FluidRegistry.getDefaultFluidName(fluid.getFluid()));
        } else {
            ItemStack item = menu().getTransferItemSnapshot(slot)
                .getTemplate();
            if (item == null) return "";
            String name = Item.itemRegistry.getNameForObject(item.getItem());
            int colon = name == null ? -1 : name.indexOf(':');
            owner = colon < 0 ? null
                : Loader.instance()
                    .getIndexedModList()
                    .get(name.substring(0, colon));
        }
        return owner == null ? "Minecraft" : owner.getName();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (priority.getVisible()) priority.mouseClicked(mouseX, mouseY, button);
        if (priority.isFocused()) {
            consumeMouseButton(button);
            return;
        }
        if (search != null) {
            search.mouseClicked(mouseX, mouseY, button);
            if (search.isFocused()) {
                consumeMouseButton(button);
                return;
            }
            if (button == 0 && mouseX >= guiLeft + 174
                && mouseX < guiLeft + 186
                && mouseY >= guiTop + 20
                && mouseY < guiTop + 92) {
                scrolling = true;
                scrollTo(mouseY);
                return;
            }
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

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        if (search == null) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !StorageShortcutInput.handlesWheel(this)) {
            updateSearch(menu().getScrollRow() + (wheel > 0 ? -1 : 1));
        }
    }

    private void scrollTo(int mouseY) {
        int max = menu().getMaxScrollRow();
        int row = max == 0 ? 0 : Math.round((mouseY - guiTop - 25.5F) / 57F * max);
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
        int offset = max == 0 ? 0 : Math.round(57F * row / max);
        drawTexturedModalRect(guiLeft + 174, guiTop + 20 + offset, 0, 0, 12, 15);
    }

    private ContainerDrawer menu() {
        return (ContainerDrawer) inventorySlots;
    }

    private void updateSearch(int row) {
        ContainerDrawer menu = menu();
        if (!menu.isSnapshotReady()) return;
        requestedRow = Math.max(0, Math.min(row, menu.getMaxScrollRow()));
        String text = search == null ? ""
            : search.getText()
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
            if (isItemTerminal()) {
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
        if (isItemTerminal()) {
            BigItemStack item = menu.getResourceItemSnapshot(index);
            return item.hasTemplate() && itemSearch != null && itemSearch.test(item.getTemplate());
        }
        if (isFluidTerminal()) {
            BigFluidStack fluid = menu.getResourceFluidSnapshot(index);
            return fluid.hasTemplate() && (fluid.getTemplate()
                .getLocalizedName() + " "
                + fluid.getTemplate()
                    .getFluid()
                    .getName()
                + " "
                + FluidRegistry.getDefaultFluidName(
                    fluid.getTemplate()
                        .getFluid())).toLowerCase(Locale.ROOT)
                            .contains(text);
        }
        BigAspectStack aspect = menu.getResourceAspectSnapshot(index);
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

    private void consumeMouseButton(int button) {
        consumedMouseButtons |= 1 << button;
        field_147007_t = false;
        field_147008_s.clear();
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long heldTime) {
        if ((consumedMouseButtons & 1 << button) == 0) super.mouseClickMove(x, y, button, heldTime);
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        if (button >= 0) {
            boolean nei = Mods.NotEnoughItems.isModLoaded();
            boolean dropped = nei && NEIGuiIntegration.dropDraggedStack(this, x, y, button);
            if ((consumedMouseButtons & 1 << button) != 0 || dropped) {
                consumedMouseButtons &= ~(1 << button);
                field_147007_t = false;
                field_147008_s.clear();
                if (nei) NEIGuiIntegration.releaseMouse(this, x, y, button);
                if (button == 1) {
                    int slot = pendingUpgradeSlot;
                    pendingUpgradeSlot = -1;
                    if (slot >= 0 && slot == upgradeSlotAt(x, y) && mc.thePlayer.inventory.getItemStack() == null) {
                        // Open after release so the new screen cannot inherit this right-click.
                        mc.playerController
                            .sendEnchantPacket(inventorySlots.windowId, GuiHandler.GUI_UPGRADE_BASE + slot);
                    }
                }
                return;
            }
        }
        super.mouseMovedOrUp(x, y, button);
    }

    private int contentX(int slot) {
        return search != null || tile.getItemHandler() != null ? layout.storageX(slot)
            : 64 + Math.round(
                ((DrawerBlock) tile.getBlockType()).getFaceLayout()
                    .getSlotX(slot) * 48F)
                - 8;
    }

    private int contentY(int slot) {
        return search != null || tile.getItemHandler() != null ? layout.storageY(slot)
            : 16 + Math.round(
                ((DrawerBlock) tile.getBlockType()).getFaceLayout()
                    .getSlotY(slot) * 48F)
                - 8;
    }

    private String formatAmount(long amount) {
        return StorageAmountFormatter.formatDisplay(amount, isFluidTerminal());
    }

    private Entry entry(int slot) {
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        if (isFluidTerminal()) {
            BigFluidStack stored = menu.getTransferFluidSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    stored.getTemplate(),
                    null,
                    StorageAmountFormatter.formatDisplay(stored.getAmount(), true))
                : null;
        }
        if (isAspectTerminal()) {
            BigAspectStack stored = menu.getTransferAspectSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    null,
                    AspectIcon.of(stored.getAspect()),
                    StorageAmountFormatter.formatDisplay(stored.getAmount(), false))
                : null;
        }
        if (isItemTerminal()) {
            BigItemStack stored = menu.getTransferItemSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    stored.getTemplate(),
                    null,
                    null,
                    StorageAmountFormatter.formatDisplay(stored.getAmount(), false))
                : null;
        }
        if (tile.getFluidHandler() != null) {
            BigFluidStack stored = menu.getTransferFluidSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    stored.getTemplate(),
                    null,
                    StorageAmountFormatter.formatDisplay(stored.getAmount(), true))
                : null;
        }
        if (tile.getAspectHandler() != null) {
            BigAspectStack stored = menu.getTransferAspectSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(
                    null,
                    null,
                    AspectIcon.of(stored.getAspect()),
                    StorageAmountFormatter.formatDisplay(stored.getAmount(), false))
                : null;
        }
        return null;
    }

    private long transferAmount(int slot) {
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        if (isFluidTerminal()) return menu.getTransferFluidSnapshot(slot)
            .getAmount();
        if (isAspectTerminal()) return menu.getTransferAspectSnapshot(slot)
            .getAmount();
        return menu.getTransferItemSnapshot(slot)
            .getAmount();
    }

    private long transferCapacity(int slot) {
        return ((ContainerDrawer) inventorySlots).getTransferCapacity(slot);
    }

    private IStorageHandler<?, ?> displayStorage() {
        if (isFluidTerminal()) return tile.getFluidHandler();
        if (isAspectTerminal()) return tile.getAspectHandler();
        if (isItemTerminal()) return tile.getItemHandler();
        return tile.getActiveStorage();
    }

    private boolean isItemTerminal() {
        if (menu().isTransferViewVirtual()) return AdaptiveMatrixTile.TERMINAL_ITEMS.equals(menu().getViewMode());
        if (tile instanceof AdaptiveMatrixTile matrix) return matrix.isTerminalItems();
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalItems();
        return tile.getItemHandler() != null;
    }

    private boolean isFluidTerminal() {
        if (menu().isTransferViewVirtual()) return AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(menu().getViewMode());
        if (tile instanceof AdaptiveMatrixTile matrix) return matrix.isTerminalFluids();
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalFluids();
        return tile.getItemHandler() == null && tile.getFluidHandler() != null;
    }

    private boolean isAspectTerminal() {
        if (menu().isTransferViewVirtual()) return AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(menu().getViewMode());
        if (tile instanceof AdaptiveMatrixTile matrix) return matrix.isTerminalAspects();
        if (tile instanceof AdaptiveGridTile grid) return grid.isTerminalAspects();
        return tile.getItemHandler() == null && tile.getFluidHandler() == null && tile.getAspectHandler() != null;
    }

    private void updateAdaptiveModeButton() {
        if (adaptiveModeButton == null) return;
        String mode = StatCollector.translateToLocal("functionalstorage.gui.adaptive.mode." + terminalMode());
        adaptiveModeButton.displayString = StatCollector
            .translateToLocalFormatted("functionalstorage.gui.adaptive.mode.button", mode);
        adaptiveSortButton.displayString = buttonLabel("sort", menu().getSortId());
        adaptiveLayoutButton.displayString = buttonLabel("layout", menu().getLayoutId());
        adaptiveOrderButton.displayString = buttonLabel("order", menu().getOrderId());
    }

    private String terminalMode() {
        return menu().isTransferViewVirtual() ? menu().getViewMode()
            : tile instanceof AdaptiveMatrixTile matrix ? matrix.getTerminalMode()
                : tile instanceof AdaptiveGridTile grid ? grid.getTerminalMode() : AdaptiveMatrixTile.TERMINAL_ITEMS;
    }

    private String buttonLabel(String group, String id) {
        String value = StatCollector
            .translateToLocal("functionalstorage.gui.adaptive.presentation." + group + "." + id);
        return fontRendererObj.trimStringToWidth(
            StatCollector.translateToLocalFormatted("functionalstorage.gui.adaptive." + group + ".button", value),
            70);
    }

    @Nonnull
    public static ResourceLocation backgroundTexture() {
        return DrawerGuiTextures.BACKGROUND;
    }
}
