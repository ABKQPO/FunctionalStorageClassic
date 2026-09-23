package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.client.integration.NEIGuiIntegration;
import com.hfstudio.functionalstorage.common.container.ContainerUpgrade;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.BreakerUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.RefillUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeSettings;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.client.config.GuiButtonExt;

public class GuiUpgrade extends GuiContainer {

    private static final ResourceLocation DIRECTIONS = texture("directions");
    private static final ResourceLocation FILTERS_BUTTON = texture("filters_button");
    private static final ResourceLocation FILTERS_PANEL = texture("filters_panel");
    private static final ResourceLocation FILTER_BUTTONS = texture("filter_buttons");
    private static final ResourceLocation REFILL = texture("refill_location");
    private final ContainerUpgrade container;
    private final DrawerInfoPanel drawer;
    private final boolean itemFilters;
    private final StorageTooltipPainter previews = StorageTooltipPainter.INSTANCE;
    private boolean filtersOpen;
    private int consumedMouseButtons;
    private GuiButton pressedButton;

    public GuiUpgrade(EntityPlayer player, ControllableDrawerTile tile, int upgradeSlot) {
        super(new ContainerUpgrade(tile, player, upgradeSlot));
        container = (ContainerUpgrade) inventorySlots;
        drawer = new DrawerInfoPanel(tile);
        itemFilters = tile.getItemHandler() != null;
        xSize = 176;
        ySize = 186;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(FunctionalStorage.MOD_ID, "textures/gui/" + name + ".png");
    }

    public boolean overlapsFilterPanel(int x, int y, int width, int height) {
        return filtersOpen && x < guiLeft + xSize + 63
            && x + width > guiLeft + xSize
            && y < guiTop + 94
            && y + height > guiTop + 10;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButtonExt(6, guiLeft + 116, guiTop + 62, 16, 14, ""));
        buttonList.add(new GuiButtonExt(90, guiLeft + 7, guiTop + 7, 14, 12, "<"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) filtersOpen = itemFilters && !filtersOpen;
        else send(button.id);
    }

    private void send(int id) {
        mc.playerController.sendEnchantPacket(inventorySlots.windowId, id);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiButton object : buttonList) {
            GuiButton button = object;
            if (button.id == 6)
                button.displayString = switch (UpgradeSettings.get(container.getUpgradeStack(), "RedstoneMode")) {
                case 1 -> "-";
                case 2 -> "+";
                case 3 -> "1";
                default -> "*";
                };
        }
    }

    private boolean selected(int slot) {
        ItemStack stack = container.getUpgradeStack();
        int[] selected = ((AutomationUpgradeItem) stack.getItem()).getSelectedSlots(stack);
        if (selected == null) return true;
        for (int index : selected) if (slot == index) return true;
        return false;
    }

    private String text(String key) {
        return StatCollector.translateToLocal(key);
    }

    private void icon(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        mc.getTextureManager()
            .bindTexture(texture);
        GL11.glColor4f(1, 1, 1, 1);
        func_146110_a(guiLeft + x, guiTop + y, u, v, 14, 14, width, height);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        DrawerGuiTextures textures = DrawerGuiTextures.INSTANCE;
        textures.panel(guiLeft, guiTop, xSize, ySize);
        for (Slot slot : inventorySlots.inventorySlots) {
            if (slot.slotNumber == 0 && !(container.getUpgradeStack()
                .getItem() instanceof BreakerUpgradeItem)) continue;
            textures.slot(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition);
        }
        drawer.draw(guiLeft + 44, guiTop + 28);
        for (int slot = 0; slot < drawer.size(); slot++) {
            if (selected(slot)) drawer.outline(guiLeft + 44, guiTop + 28, slot, 0xFFFF006E);
        }
        ItemStack stack = container.getUpgradeStack();
        AutomationUpgradeItem upgrade = (AutomationUpgradeItem) stack.getItem();
        if (upgrade.hasDirection()) icon(
            DIRECTIONS,
            117,
            28,
            upgrade.getDirection(stack)
                .ordinal() * 14,
            0,
            84,
            14);
        if (itemFilters) icon(FILTERS_BUTTON, 117, 45, 0, 0, 14, 14);
        if (upgrade instanceof RefillUpgradeItem)
            icon(REFILL, 80, 80, UpgradeSettings.get(stack, "RefillTarget") * 14, 0, 42, 14);
        if (filtersOpen) {
            mc.getTextureManager()
                .bindTexture(FILTERS_PANEL);
            GL11.glColor4f(1, 1, 1, 1);
            func_146110_a(guiLeft + xSize, guiTop + 10, 0, 0, 63, 84, 63, 84);
            for (int slot = 0; slot < UpgradeSettings.FILTER_SLOTS; slot++) {
                int x = guiLeft + xSize + 6 + slot % 3 * 18;
                int y = guiTop + 18 + slot / 3 * 18;
                textures.slot(x, y);
                ItemStack filter = UpgradeSettings.getFilter(stack, slot);
                if (filter != null) previews.drawEntry(new Entry(filter, null, null, ""), x, y);
            }
            icon(FILTER_BUTTONS, xSize + 6, 74, 28, UpgradeSettings.get(stack, "Blacklist") * 14, 42, 28);
            icon(FILTER_BUTTONS, xSize + 23, 74, 14, UpgradeSettings.get(stack, "StrictMatching") * 14, 42, 28);
            icon(FILTER_BUTTONS, xSize + 40, 74, 0, UpgradeSettings.get(stack, "OreMatching") * 14, 42, 28);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(
                container.getUpgradeStack()
                    .getDisplayName(),
                142),
            25,
            8,
            0x404040);
        fontRendererObj.drawString(text("container.inventory"), 8, 92, 0x404040);
    }

    public int getFilterSlotAt(int x, int y) {
        if (!filtersOpen) return -1;
        int rx = x - guiLeft - xSize - 6;
        int ry = y - guiTop - 18;
        return rx >= 0 && rx < 54 && ry >= 0 && ry < 54 ? rx / 18 + ry / 18 * 3 : -1;
    }

    private int control(int x, int y) {
        x -= guiLeft;
        y -= guiTop;
        if (x >= 117 && x < 131) {
            if (y >= 28 && y < 42) return 0;
            if (itemFilters && y >= 45 && y < 59) return 1;
        }
        if (container.getUpgradeStack()
            .getItem() instanceof RefillUpgradeItem && x >= 80
            && x < 94
            && y >= 80
            && y < 94) return 5;
        if (filtersOpen && y >= 74 && y < 88) {
            for (int button = 0; button < 3; button++) {
                int left = xSize + 6 + button * 17;
                if (x >= left && x < left + 14) return button + 2;
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        int filter = getFilterSlotAt(x, y);
        if (filter >= 0) {
            consumeMouseButton(button);
            if (Mods.NotEnoughItems.isModLoaded() && NEIGuiIntegration.dropDraggedStack(this, x, y, button)) return;
            if (button == 0 || button == 1) send((button == 0 ? 40 : 49) + filter);
            return;
        }
        int control = control(x, y);
        if (control >= 0) {
            consumeMouseButton(button);
            if (control == 1) filtersOpen = !filtersOpen;
            else send(control == 0 && button == 1 ? 7 : control);
            return;
        }
        int slot = drawer.slotAt(x - guiLeft - 44, y - guiTop - 28);
        if (slot >= 0) {
            consumeMouseButton(button);
            if (button == 0) send(20 + slot);
            return;
        }
        if (overlapsFilterPanel(x, y, 1, 1)) {
            consumeMouseButton(button);
            return;
        }
        if (button == 0) {
            for (GuiButton guiButton : buttonList) {
                if (guiButton.mousePressed(mc, x, y)) {
                    consumeMouseButton(button);
                    pressedButton = guiButton;
                    guiButton.func_146113_a(mc.getSoundHandler());
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
    }

    private void consumeMouseButton(int button) {
        // A custom control owns the whole gesture, even when released outside its bounds.
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
            boolean consumed = (consumedMouseButtons & 1 << button) != 0;
            if (consumed || dropped || overlapsFilterPanel(x, y, 1, 1)) {
                int filter = getFilterSlotAt(x, y);
                if (!consumed && !dropped
                    && button == 0
                    && filter >= 0
                    && mc.thePlayer.inventory.getItemStack() != null) send(40 + filter);
                consumedMouseButtons &= ~(1 << button);
                field_147007_t = false;
                field_147008_s.clear();
                if (nei) NEIGuiIntegration.releaseMouse(this, x, y, button);
                if (button == 0 && pressedButton != null) {
                    GuiButton released = pressedButton;
                    pressedButton = null;
                    released.mouseReleased(x, y);
                    if (released.mousePressed(mc, x, y)) actionPerformed(released);
                }
                return;
            }
        }
        super.mouseMovedOrUp(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == 1 || key == mc.gameSettings.keyBindInventory.getKeyCode()) send(90);
        else super.keyTyped(character, key);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int slot = getFilterSlotAt(
            Mouse.getEventX() * width / mc.displayWidth,
            height - Mouse.getEventY() * height / mc.displayHeight - 1);
        if (wheel != 0 && slot >= 0) send((wheel > 0 ? 60 : 69) + slot);
    }

    @Override
    public void drawScreen(int x, int y, float partial) {
        super.drawScreen(x, y, partial);
        ItemStack stack = container.getUpgradeStack();
        List<String> lines = new ArrayList<>();
        int filter = getFilterSlotAt(x, y);
        if (filter >= 0) {
            ItemStack item = UpgradeSettings.getFilter(stack, filter);
            String ore = UpgradeSettings.filterOre(stack, filter);
            lines.add(item == null ? text("gui.functionalstorage.empty") : item.getDisplayName());
            lines.add(text("functionalstorage.gui.filter_use"));
            lines.add(text("tooltip.morefunctionalstorage.tag_selection"));
            lines.add(ore.isEmpty() ? text("tooltip.morefunctionalstorage.tag.any_of_the_above") : ore);
        }
        int control = control(x, y);
        if (control >= 0) lines.add(switch (control) {
            case 0 -> text("item.utility.direction") + ((AutomationUpgradeItem) stack.getItem()).getDirection(stack)
                .getDisplayName();
            case 1 -> text("functionalstorage.gui.filters");
            case 2 -> text(
                "tooltip.morefunctionalstorage."
                    + (UpgradeSettings.get(stack, "Blacklist") == 0 ? "whitelist" : "blacklist"));
            case 3 -> "NBT: " + text(
                "functionalstorage.configuration.state."
                    + (UpgradeSettings.get(stack, "StrictMatching") == 0 ? "off" : "on"));
            case 4 -> text("functionalstorage.gui.ore") + ": "
                + text(
                    "functionalstorage.configuration.state."
                        + (UpgradeSettings.get(stack, "OreMatching") == 0 ? "off" : "on"));
            default -> text(
                "tooltip.refill_target.morefunctionalstorage." + switch (UpgradeSettings.get(stack, "RefillTarget")) {
                case 1 -> "main_inv";
                case 2 -> "ender_chest";
                default -> "hotbar";
                });
        });
        int slot = drawer.slotAt(x - guiLeft - 44, y - guiTop - 28);
        if (slot >= 0) {
            Entry entry = drawer.entry(slot);
            if (entry != null) lines.add(entry.name());
            lines.add(
                StatCollector.translateToLocalFormatted(
                    selected(slot) ? "tooltip.morefunctionalstorage.slot_selected"
                        : "tooltip.morefunctionalstorage.lclick_select",
                    text("tooltip.morefunctionalstorage.source")));
            if (selected(slot)) lines.add(text("tooltip.morefunctionalstorage.lclick_deselect"));
        }
        if (x >= guiLeft + 116 && x < guiLeft + 132 && y >= guiTop + 62 && y < guiTop + 76)
            lines.add(text("functionalstorage.gui.redstone." + UpgradeSettings.get(stack, "RedstoneMode")));
        if (!lines.isEmpty()) {
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) wrapped.addAll(fontRendererObj.listFormattedStringToWidth(line, 200));
            drawHoveringText(wrapped, x, y, fontRendererObj);
        }
    }
}
