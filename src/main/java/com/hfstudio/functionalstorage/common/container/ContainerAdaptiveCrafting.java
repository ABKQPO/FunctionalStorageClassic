package com.hfstudio.functionalstorage.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.integration.bogosorter.BogoSorterIntegration;
import com.hfstudio.functionalstorage.common.tile.AdaptiveCraftingGridTile;

import lombok.Getter;

public class ContainerAdaptiveCrafting extends ContainerDrawer {

    private final AdaptiveCraftingGridTile craftingTile;
    private final InventoryCrafting crafting;
    private final InventoryCraftResult result = new InventoryCraftResult();
    private final int resultSlot;
    @Getter
    private final int ingredientStart;
    private final int playerStart;
    private IRecipe cachedRecipe;
    private long observedCraftingRevision = Long.MIN_VALUE;
    private int mutationDepth;
    private int refillTarget = 1;

    public ContainerAdaptiveCrafting(AdaptiveCraftingGridTile tile, EntityPlayer player) {
        super(tile, player, true);
        craftingTile = tile;
        crafting = new TerminalCraftingInventory(this, tile.getCraftingInventory());
        playerStart = getVisibleStorageSlots() + tile.getStorageUpgradeSlots() + tile.getUtilityUpgradeSlots();
        resultSlot = inventorySlots.size();
        addSlotToContainer(
            new RefillingCraftingSlot(player, DrawerGuiLayout.CRAFTING_RESULT_X, DrawerGuiLayout.CRAFTING_RESULT_Y));
        ingredientStart = inventorySlots.size();
        for (int index = 0; index < 9; index++) {
            addSlotToContainer(
                new Slot(
                    crafting,
                    index,
                    DrawerGuiLayout.CRAFTING_MATRIX_X + index % 3 * 18,
                    DrawerGuiLayout.CRAFTING_MATRIX_Y + index / 3 * 18));
        }
        onCraftMatrixChanged(crafting);
    }

    public InventoryCrafting getCraftingInventory() {
        return crafting;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        if (mutationDepth == 0) refreshCraftingResult();
    }

    private void refreshCraftingResult() {
        if (craftingTile == null || crafting == null
            || craftingTile.getWorldObj().isRemote
            || mutationDepth > 0
            || observedCraftingRevision == craftingTile.getCraftingRevision()) return;
        observedCraftingRevision = craftingTile.getCraftingRevision();
        ItemStack first = null;
        ItemStack second = null;
        int occupied = 0;
        for (int index = 0; index < 9; index++) {
            ItemStack stack = crafting.getStackInSlot(index);
            if (stack == null) continue;
            if (occupied == 0) first = stack;
            else if (occupied == 1) second = stack;
            occupied++;
        }
        // Vanilla repair precedes registered recipes and has no IRecipe instance.
        if (occupied == 2 && first.getItem() == second.getItem()
            && first.stackSize == 1
            && second.stackSize == 1
            && first.getItem()
                .isRepairable()) {
            cachedRecipe = null;
            result.setInventorySlotContents(
                0,
                CraftingManager.getInstance()
                    .findMatchingRecipe(crafting, craftingTile.getWorldObj()));
            return;
        }
        if (occupied == 0) {
            cachedRecipe = null;
            result.setInventorySlotContents(0, null);
            return;
        }
        if (cachedRecipe == null || !cachedRecipe.matches(crafting, craftingTile.getWorldObj())) {
            cachedRecipe = null;
            for (IRecipe recipe : CraftingManager.getInstance()
                .getRecipeList()) {
                if (recipe.matches(crafting, craftingTile.getWorldObj())) {
                    cachedRecipe = recipe;
                    break;
                }
            }
        }
        result.setInventorySlotContents(0, cachedRecipe == null ? null : cachedRecipe.getCraftingResult(crafting));
    }

    private void finishMutation() {
        mutationDepth--;
        if (mutationDepth == 0) refreshCraftingResult();
    }

    @Override
    public void detectAndSendChanges() {
        refreshCraftingResult();
        if (crafting != null && !craftingTile.getWorldObj().isRemote
            && !ItemStack.areItemStacksEqual(inventoryItemStacks.get(resultSlot), result.getStackInSlot(0))) {
            for (Object listener : crafters) {
                if (listener instanceof EntityPlayerMP player && player.playerNetServerHandler != null) {
                    player.playerNetServerHandler
                        .sendPacket(new S2FPacketSetSlot(windowId, resultSlot, result.getStackInSlot(0)));
                }
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
        refreshCraftingResult();
        mutationDepth++;
        try {
            return super.slotClick(index, button, mode, player);
        } finally {
            finishMutation();
        }
    }

    @Override
    protected int playerInventoryEnd() {
        return getVisibleStorageSlots() + getTile().getStorageUpgradeSlots() + getTile().getUtilityUpgradeSlots() + 36;
    }

    @Override
    public ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index) {
        if (getTile().getWorldObj().isRemote || !canInteractWith(player) || index < 0 || index >= inventorySlots.size())
            return null;
        if (index < resultSlot) return super.transferStackInSlot(player, index);
        if (index == resultSlot) return craftIntoInventory(player);
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (!mergeItemStack(stack, playerStart, playerInventoryEnd(), true)) return null;
        if (stack.stackSize <= 0) slot.putStack(null);
        else slot.onSlotChanged();
        if (stack.stackSize == original.stackSize) return null;
        slot.onPickupFromSlot(player, stack);
        return original;
    }

    private ItemStack craftIntoInventory(EntityPlayer player) {
        int outerDepth = mutationDepth;
        mutationDepth = 0;
        try {
            refreshCraftingResult();
            ItemStack expected = result.getStackInSlot(0);
            if (expected == null || expected.stackSize <= 0) return null;
            expected = expected.copy();
            int attempts = Math.max(1, expected.getMaxStackSize() / expected.stackSize);
            boolean crafted = false;
            for (int attempt = 0; attempt < attempts; attempt++) {
                refreshCraftingResult();
                ItemStack output = result.getStackInSlot(0);
                if (output == null || !ItemStack.areItemStacksEqual(expected, output) || !fitsOutput(output)) break;
                refillTarget = attempts - attempt;
                Slot slot = inventorySlots.get(resultSlot);
                ItemStack taken = slot.decrStackSize(output.stackSize);
                ItemStack eventStack = taken.copy();
                mergeItemStack(taken, playerStart, playerInventoryEnd(), true);
                slot.onPickupFromSlot(player, eventStack);
                crafted = true;
            }
            return crafted ? expected : null;
        } finally {
            refillTarget = 1;
            mutationDepth = outerDepth;
        }
    }

    private boolean fitsOutput(ItemStack output) {
        int space = 0;
        for (int index = playerStart; index < playerInventoryEnd(); index++) {
            Slot slot = inventorySlots.get(index);
            if (!slot.isItemValid(output)) continue;
            ItemStack existing = slot.getStack();
            if (existing == null) space += Math.min(output.getMaxStackSize(), slot.getSlotStackLimit());
            else if (sameType(existing, output))
                space += Math.max(0, Math.min(output.getMaxStackSize(), slot.getSlotStackLimit()) - existing.stackSize);
            if (space >= output.stackSize) return true;
        }
        return false;
    }

    @Override
    public void applyFilter(EntityPlayer player, int slot, ItemStack template) {
        if (slot < ingredientStart || slot >= ingredientStart + 9) {
            super.applyFilter(player, slot, template);
            return;
        }
        if (!canInteractWith(player) || template == null || getTile().getWorldObj().isRemote) return;
        int index = slot - ingredientStart;
        ItemStack existing = crafting.getStackInSlot(index);
        if (existing != null && sameType(existing, template)) return;
        ItemStack ingredient = takeIngredient(player, template, 1, true);
        if (ingredient == null) return;
        mutationDepth++;
        try {
            if (existing != null) returnStack(player, existing);
            crafting.setInventorySlotContents(index, ingredient);
        } finally {
            finishMutation();
        }
        player.inventory.markDirty();
        detectAndSendChanges();
    }

    private ItemStack takeIngredient(EntityPlayer player, ItemStack template, int count, boolean usePlayerInventory) {
        if (count <= 0) return null;
        ItemStack collected = template.copy();
        collected.stackSize = 0;
        IBigItemHandler handler = getTile().getItemHandler();
        if (handler != null) {
            collected.stackSize = (int) handler.extractRouted(new BigItemStack(template, count), StorageAction.EXECUTE)
                .getProcessedAmount();
        }
        for (int index = 0; usePlayerInventory && collected.stackSize < count
            && index < player.inventory.mainInventory.length; index++) {
            if (Mods.InventoryBogoSorter.isModLoaded() && BogoSorterIntegration.isPinned(player, index)) continue;
            ItemStack available = player.inventory.getStackInSlot(index);
            if (available != null && sameType(available, template)) {
                ItemStack taken = player.inventory.decrStackSize(index, count - collected.stackSize);
                if (taken != null) collected.stackSize += taken.stackSize;
            }
        }
        return collected.stackSize > 0 ? collected : null;
    }

    public void applyRecipe(EntityPlayer player, ItemStack[] templates, boolean maxTransfer) {
        if (!canInteractWith(player) || getTile().getWorldObj().isRemote || templates.length != 9 || !isOnline())
            return;
        mutationDepth++;
        try {
            for (int index = 0; index < 9; index++) {
                ItemStack template = templates[index];
                ItemStack current = crafting.getStackInSlot(index);
                if (current != null && (template == null || !sameType(current, template))) {
                    crafting.setInventorySlotContents(index, null);
                    returnStack(player, current);
                }
            }
            boolean[] assigned = new boolean[9];
            int[] positions = new int[9];
            for (int index = 0; index < 9; index++) {
                ItemStack template = templates[index];
                if (assigned[index] || template == null || template.getItem() == null) continue;
                int count = 0;
                int available = 0;
                for (int slot = index; slot < 9; slot++) {
                    if (templates[slot] == null || !sameType(template, templates[slot])) continue;
                    assigned[slot] = true;
                    positions[count++] = slot;
                    ItemStack current = crafting.getStackInSlot(slot);
                    if (current != null) available += current.stackSize;
                }
                int limit = Math.min(crafting.getInventoryStackLimit(), template.getMaxStackSize());
                int target = maxTransfer ? limit : 1;
                ItemStack additional = takeIngredient(player, template, target * count - available, true);
                if (additional != null) available += additional.stackSize;
                // Distribute the shared pool across recipe coordinates, including scarce partial recipes.
                for (int position = 0; position < count; position++) {
                    int amount = Math
                        .min(limit, available / (count - position) + (available % (count - position) == 0 ? 0 : 1));
                    ItemStack stack = amount == 0 ? null : template.copy();
                    if (stack != null) stack.stackSize = amount;
                    crafting.setInventorySlotContents(positions[position], stack);
                    available -= amount;
                }
            }
        } finally {
            finishMutation();
        }
        player.inventory.markDirty();
        detectAndSendChanges();
    }

    private static boolean sameType(ItemStack first, ItemStack second) {
        return first.getItem() == second.getItem() && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private void returnStack(EntityPlayer player, ItemStack stack) {
        IBigItemHandler handler = getTile().getItemHandler();
        if (handler != null) stack.stackSize -= (int) handler
            .insertRouted(new BigItemStack(stack, stack.stackSize), StorageAction.EXECUTE)
            .getProcessedAmount();
        if (stack.stackSize > 0 && !player.inventory.addItemStackToInventory(stack)) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }

    @Override
    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return slot.inventory != result && super.func_94530_a(stack, slot);
    }

    public class RefillingCraftingSlot extends SlotCrafting {

        public RefillingCraftingSlot(EntityPlayer player, int x, int y) {
            super(player, crafting, result, 0, x, y);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            refreshCraftingResult();
            return canInteractWith(player) && getHasStack();
        }

        @Override
        public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
            ItemStack[] templates = new ItemStack[9];
            for (int index = 0; index < templates.length; index++) {
                ItemStack ingredient = crafting.getStackInSlot(index);
                templates[index] = ingredient == null ? null : ingredient.copy();
            }
            mutationDepth++;
            try {
                super.onPickupFromSlot(player, stack);
                for (int index = 0; index < templates.length; index++) {
                    ItemStack template = templates[index];
                    if (template == null || crafting.getStackInSlot(index) != null) continue;
                    int count = Math
                        .min(refillTarget, Math.min(template.getMaxStackSize(), crafting.getInventoryStackLimit()));
                    ItemStack replacement = takeIngredient(player, template, count, false);
                    if (replacement != null) crafting.setInventorySlotContents(index, replacement);
                }
                crafting.markDirty();
            } finally {
                finishMutation();
            }
        }
    }

    public static class TerminalCraftingInventory extends InventoryCrafting {

        private final ContainerAdaptiveCrafting menu;
        private final IInventory storage;

        public TerminalCraftingInventory(ContainerAdaptiveCrafting menu, IInventory storage) {
            super(menu, 3, 3);
            this.menu = menu;
            this.storage = storage;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return storage.getStackInSlot(index);
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            ItemStack stack = storage.decrStackSize(index, count);
            menu.onCraftMatrixChanged(this);
            return stack;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int index) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            storage.setInventorySlotContents(index, stack);
            menu.onCraftMatrixChanged(this);
        }

        @Override
        public void markDirty() {
            storage.markDirty();
            menu.onCraftMatrixChanged(this);
        }
    }
}
