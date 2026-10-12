package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.hfstudio.functionalstorage.FunctionalStorage;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.AspectIcon;
import com.hfstudio.functionalstorage.client.gui.DrawerTooltipData.Entry;
import com.hfstudio.functionalstorage.common.container.ContainerDrawer;
import com.hfstudio.functionalstorage.common.container.DrawerGuiLayout;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.network.StorageTransferMessage;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.AdaptiveMatrixTile;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;

public abstract class GuiStorage extends BaseGUI {

    protected final ControllableDrawerTile tile;
    protected final DrawerGuiLayout layout;
    private final StorageTooltipPainter previews = StorageTooltipPainter.INSTANCE;

    protected GuiStorage(ControllableDrawerTile tile, ContainerDrawer container) {
        super(container);
        this.tile = tile;
        this.layout = container.getLayout();
    }

    @Nonnull
    public ControllableDrawerTile getTile() {
        return tile;
    }

    protected ContainerDrawer menu() {
        return (ContainerDrawer) inventorySlots;
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
        Slot hovered = storageSlotAt(mouseX, mouseY);
        if (hovered == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        Entry entry = entry(hovered.getSlotIndex());
        if (!hovered.getHasStack() && entry == null) return List.of();
        if (!hovered.getHasStack()) lines.add(entry.name());
        lines.add(EnumChatFormatting.GRAY + amountLine(hovered.getSlotIndex()));
        if (menu().isTransferViewVirtual() && menu().getUnassignedDisplayResources() > 0) {
            lines.add(
                EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                    "functionalstorage.gui.adaptive.unassigned",
                    menu().getUnassignedDisplayResources()));
        }
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(menu().getTransferResourceKind(hovered.getSlotIndex()))) {
            BigAspectStack aspect = menu().getTransferAspectSnapshot(hovered.getSlotIndex());
            if (aspect.hasTemplate()) lines.add(
                EnumChatFormatting.GRAY + aspect.getAspect()
                    .getLocalizedDescription());
        }
        if (mc.gameSettings.advancedItemTooltips) lines.add(
            EnumChatFormatting.GRAY + StatCollector.translateToLocal("gui.functionalstorage.slot")
                + hovered.getSlotIndex());
        if (!hovered.getHasStack()) {
            String mod = resourceModName(hovered.getSlotIndex());
            if (!mod.isEmpty()) lines.add(EnumChatFormatting.BLUE.toString() + EnumChatFormatting.ITALIC + mod);
        }
        return lines;
    }

    private String amountLine(int slot) {
        boolean fluid = AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(menu().getTransferResourceKind(slot));
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
        String kind = menu().getTransferResourceKind(slot);
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind)) return "Thaumcraft";
        ModContainer owner;
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) {
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
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

    protected void drawEntries() {
        GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
        try {
            // Resource overlays must not occlude later count, cursor or tooltip drawing.
            GL11.glDepthMask(false);
            for (int slot = 0; slot < menu().getVisibleStorageSlots(); slot++) {
                Entry entry = entry(slot);
                if (entry == null) continue;
                int x = contentX(slot);
                int y = contentY(slot);
                if (entry.fluid() != null || entry.aspect() != null
                    || entry.item() != null && transferAmount(slot) == 0L) {
                    previews.drawEntry(new Entry(entry.item(), entry.fluid(), entry.aspect(), ""), x, y);
                }
                drawSlotAmount(entry.amount(), x, y);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    protected int contentX(int slot) {
        return layout.storageX(slot);
    }

    protected int contentY(int slot) {
        return layout.storageY(slot);
    }

    protected void drawLabels(int titleWidth) {
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(
                StatCollector.translateToLocal(
                    tile.getBlockType()
                        .getUnlocalizedName() + ".name"),
                titleWidth),
            8,
            6,
            LABEL_COLOUR);
        fontRendererObj.drawString(
            StatCollector.translateToLocal("container.inventory"),
            8,
            layout.inventoryY() - 12,
            LABEL_COLOUR);
    }

    protected String formatAmount(long amount, int slot) {
        return StorageAmountFormatter
            .formatSlot(amount, AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(menu().getTransferResourceKind(slot)));
    }

    private Entry entry(int slot) {
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        String kind = menu.getTransferResourceKind(slot);
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) {
            BigFluidStack stored = menu.getTransferFluidSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, stored.getTemplate(), null, formatAmount(stored.getAmount(), slot))
                : null;
        }
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind)) {
            BigAspectStack stored = menu.getTransferAspectSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, null, AspectIcon.of(stored.getAspect()), formatAmount(stored.getAmount(), slot))
                : null;
        }
        if (AdaptiveMatrixTile.TERMINAL_ITEMS.equals(kind)) {
            BigItemStack stored = menu.getTransferItemSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(stored.getTemplate(), null, null, formatAmount(stored.getAmount(), slot))
                : null;
        }
        if (tile.getFluidHandler() != null) {
            BigFluidStack stored = menu.getTransferFluidSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, stored.getTemplate(), null, formatAmount(stored.getAmount(), slot))
                : null;
        }
        if (tile.getAspectHandler() != null) {
            BigAspectStack stored = menu.getTransferAspectSnapshot(slot);
            return stored.hasTemplate()
                ? new Entry(null, null, AspectIcon.of(stored.getAspect()), formatAmount(stored.getAmount(), slot))
                : null;
        }
        return null;
    }

    private long transferAmount(int slot) {
        ContainerDrawer menu = (ContainerDrawer) inventorySlots;
        String kind = menu.getTransferResourceKind(slot);
        if (AdaptiveMatrixTile.TERMINAL_FLUIDS.equals(kind)) return menu.getTransferFluidSnapshot(slot)
            .getAmount();
        if (AdaptiveMatrixTile.TERMINAL_ASPECTS.equals(kind)) return menu.getTransferAspectSnapshot(slot)
            .getAmount();
        return menu.getTransferItemSnapshot(slot)
            .getAmount();
    }

    protected long transferCapacity(int slot) {
        return ((ContainerDrawer) inventorySlots).getTransferCapacity(slot);
    }

}
