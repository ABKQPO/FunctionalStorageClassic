package com.hfstudio.functionalstorage.common.item;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.api.storage.TransferResult;
import com.hfstudio.functionalstorage.api.upgrade.IStorageUpgrade;
import com.hfstudio.functionalstorage.api.upgrade.StorageFeature;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeAttribute;
import com.hfstudio.functionalstorage.api.upgrade.UpgradeState;
import com.hfstudio.functionalstorage.common.block.DrawerFaceLayout;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.inventory.base.BigFluidHandler;
import com.hfstudio.functionalstorage.common.storage.DrawerLayout;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

public class FluidDrawerBlockItem extends DrawerBlockItem implements IFluidContainerItem {

    private static final String TILE_DATA = "TileData";
    private static final String STORAGE_UPGRADES = "StorageUpgrades";
    private static final String UTILITY_UPGRADES = "UtilityUpgrades";
    private static final String TANKS = "Tanks";

    public FluidDrawerBlockItem(Block block) {
        super(block);
        setMaxStackSize(1);
    }

    @Nullable
    @Override
    public FluidStack getFluid(@Nullable ItemStack container) {
        if (!isSingleContainer(container)) {
            return null;
        }
        ContainerState state = readState(container);
        int first = firstPopulated(state.handler);
        if (first < 0) {
            return null;
        }
        FluidStack fluid = state.handler.getSnapshot(first)
            .getTemplate();
        long amount = 0L;
        for (int index = first; index < state.handler.getStorageCount(); index++) {
            BigFluidStack stored = state.handler.getSnapshot(index);
            if (stored.isSameType(fluid)) {
                amount = saturatedAdd(amount, stored.getAmount());
            }
        }
        fluid.amount = clampToInt(amount);
        return fluid;
    }

    @Override
    public int getCapacity(@Nullable ItemStack container) {
        if (!isSingleContainer(container)) {
            return 0;
        }
        ContainerState state = readState(container);
        int first = firstPopulated(state.handler);
        FluidStack visibleFluid = first < 0 ? null
            : state.handler.getSnapshot(first)
                .getTemplate();
        long capacity = 0L;
        for (int index = 0; index < state.handler.getStorageCount(); index++) {
            BigFluidStack stored = state.handler.getSnapshot(index);
            if (stored.isEmpty() || visibleFluid == null || stored.isSameType(visibleFluid)) {
                capacity = saturatedAdd(capacity, state.handler.getCapacity(index));
            }
        }
        return clampToInt(capacity);
    }

    @Override
    public int fill(@Nullable ItemStack container, @Nullable FluidStack resource, boolean doFill) {
        if (!isSingleContainer(container) || resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return 0;
        }
        ContainerState state = readState(container);
        long remaining = resource.amount;
        long accepted = 0L;
        for (int index = 0; index < state.handler.getStorageCount() && remaining > 0L; index++) {
            TransferResult<BigFluidStack, FluidStorageKey> result = state.handler.insert(
                index,
                new BigFluidStack(resource, remaining),
                doFill ? StorageAction.EXECUTE : StorageAction.SIMULATE);
            long inserted = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
            accepted = saturatedAdd(accepted, inserted);
            remaining -= inserted;
        }
        if (doFill && accepted > 0L) {
            writeState(container, state);
        }
        return clampToInt(accepted);
    }

    @Nullable
    @Override
    public FluidStack drain(@Nullable ItemStack container, int maxDrain, boolean doDrain) {
        if (!isSingleContainer(container) || maxDrain <= 0) {
            return null;
        }
        ContainerState state = readState(container);
        int first = firstPopulated(state.handler);
        if (first < 0) {
            return null;
        }
        FluidStack fluid = state.handler.getSnapshot(first)
            .getTemplate();
        long remaining = maxDrain;
        long drained = 0L;
        for (int index = first; index < state.handler.getStorageCount() && remaining > 0L; index++) {
            BigFluidStack stored = state.handler.getSnapshot(index);
            if (!stored.isSameType(fluid)) {
                continue;
            }
            TransferResult<BigFluidStack, FluidStorageKey> result = state.handler
                .extract(index, remaining, doDrain ? StorageAction.EXECUTE : StorageAction.SIMULATE);
            long extracted = Math.min(remaining, Math.max(0L, result.getProcessedAmount()));
            drained = saturatedAdd(drained, extracted);
            remaining -= extracted;
        }
        if (drained <= 0L) {
            return null;
        }
        if (doDrain) {
            writeState(container, state);
        }
        fluid.amount = clampToInt(drained);
        return fluid;
    }

    private ContainerState readState(ItemStack container) {
        NBTTagCompound source = DrawerBlock.getTileData(container);
        NBTTagCompound tileData = source == null ? new NBTTagCompound() : (NBTTagCompound) source.copy();
        DrawerLayout layout = DrawerLayout.fromStorage(tileData, TANKS, layoutForBlock());
        boolean locked = tileData.getBoolean("Locked");
        UpgradeState upgrades = readUpgrades(tileData);
        BigFluidHandler handler = createHandler(layout, locked, upgrades);
        handler.deserializeNBT(tileData.hasKey(TANKS, 10) ? tileData.getCompoundTag(TANKS) : null);
        return new ContainerState(tileData, layout, handler);
    }

    private BigFluidHandler createHandler(DrawerLayout layout, boolean locked, UpgradeState upgrades) {
        return new BigFluidHandler(layout.getSlotCount()) {

            @Override
            public double getMultiplier() {
                double multiplier = upgrades.calculate(UpgradeAttribute.FLUID_CAPACITY, 1D);
                return upgrades.hasFeature(StorageFeature.IRON_DOWNGRADE)
                    ? multiplier * 1000D / Math.max(1, FunctionalStorageConfig.STORAGE.baseFluidCapacity)
                    : multiplier / layout.getSlotCount();
            }

            @Override
            public boolean isLocked() {
                return locked;
            }

            @Override
            public boolean hasMaxStorage() {
                return upgrades.hasFeature(StorageFeature.MAX_CAPACITY);
            }

            @Override
            public boolean isCreative() {
                return upgrades.hasFeature(StorageFeature.CREATIVE);
            }

            @Override
            public boolean voidsOverflow() {
                return upgrades.hasFeature(StorageFeature.VOID_OVERFLOW);
            }
        };
    }

    private UpgradeState readUpgrades(NBTTagCompound tileData) {
        UpgradeState.Builder builder = UpgradeState.builder();
        applyUpgrades(builder, tileData.getTagList(STORAGE_UPGRADES, 10));
        applyUpgrades(builder, tileData.getTagList(UTILITY_UPGRADES, 10));
        return builder.build();
    }

    private void applyUpgrades(UpgradeState.Builder builder, NBTTagList entries) {
        for (int index = 0; index < entries.tagCount(); index++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(entries.getCompoundTagAt(index));
            if (stack != null && stack.getItem() instanceof IStorageUpgrade upgrade) {
                upgrade.applyUpgrade(stack, builder);
            }
        }
    }

    private void writeState(ItemStack container, ContainerState state) {
        state.tileData.setString("DrawerLayout", state.layout.getId());
        state.tileData.setTag(TANKS, state.handler.serializeNBT());
        NBTTagCompound root = container.hasTagCompound() ? (NBTTagCompound) container.getTagCompound()
            .copy() : new NBTTagCompound();
        root.setTag(TILE_DATA, state.tileData);
        container.setTagCompound(root);
    }

    private int firstPopulated(BigFluidHandler handler) {
        for (int index = 0; index < handler.getStorageCount(); index++) {
            if (!handler.getSnapshot(index)
                .isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private DrawerLayout layoutForBlock() {
        if (!(field_150939_a instanceof DrawerBlock drawerBlock)) {
            return DrawerLayout.X_1;
        }
        DrawerFaceLayout face = drawerBlock.getFaceLayout();
        return face.getSlotCount() > 2 ? DrawerLayout.X_4
            : face.getSlotCount() > 1 ? DrawerLayout.X_2 : DrawerLayout.X_1;
    }

    private boolean isSingleContainer(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() == this && stack.stackSize == 1;
    }

    private int clampToInt(long amount) {
        return amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, amount);
    }

    private long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static class ContainerState {

        private final NBTTagCompound tileData;
        private final DrawerLayout layout;
        private final BigFluidHandler handler;

        private ContainerState(NBTTagCompound tileData, DrawerLayout layout, BigFluidHandler handler) {
            this.tileData = tileData;
            this.layout = layout;
            this.handler = handler;
        }
    }
}
