package com.hfstudio.functionalstorage.common;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@SuppressWarnings("unused")
public enum FSItemList {

    OakDrawer1,
    OakDrawer2,
    OakDrawer4,
    SpruceDrawer1,
    SpruceDrawer2,
    SpruceDrawer4,
    BirchDrawer1,
    BirchDrawer2,
    BirchDrawer4,
    JungleDrawer1,
    JungleDrawer2,
    JungleDrawer4,
    AcaciaDrawer1,
    AcaciaDrawer2,
    AcaciaDrawer4,
    DarkOakDrawer1,
    DarkOakDrawer2,
    DarkOakDrawer4,
    MangroveDrawer1,
    MangroveDrawer2,
    MangroveDrawer4,
    CherryDrawer1,
    CherryDrawer2,
    CherryDrawer4,
    CrimsonDrawer1,
    CrimsonDrawer2,
    CrimsonDrawer4,
    WarpedDrawer1,
    WarpedDrawer2,
    WarpedDrawer4,

    FluidDrawer1,
    FluidDrawer2,
    FluidDrawer4,

    FramedDrawer1,
    FramedDrawer2,
    FramedDrawer4,
    FramedFluidDrawer1,
    FramedFluidDrawer2,
    FramedFluidDrawer4,

    EssentiaDrawer1,
    EssentiaDrawer2,
    EssentiaDrawer4,

    CompactingDrawer,
    SimpleCompactingDrawer,
    EnderDrawer,
    ArmoryCabinet,
    StorageController,
    ControllerExtension,

    CompactingFramedDrawer,
    FramedSimpleCompactingDrawer,
    FramedStorageController,
    FramedControllerExtension,

    IronDowngrade,
    CopperUpgrade,
    GoldUpgrade,
    DiamondUpgrade,
    NetheriteUpgrade,
    MaxStorageUpgrade,
    CreativeVendingUpgrade,

    DrippingUpgrade,
    WaterGeneratorUpgrade,
    ObsidianUpgrade,

    VoidUpgrade,
    RedstoneUpgrade,
    PullingUpgrade,
    PushingUpgrade,
    CollectorUpgrade,
    OreDictionaryUpgrade,
    WirelessPullingUpgrade,
    WirelessPushingUpgrade,

    WaterGenerationUpgrade1,
    WaterGenerationUpgrade2,
    WaterGenerationUpgrade3,
    WaterGenerationUpgrade4,
    StoneGenerationUpgrade1,
    StoneGenerationUpgrade2,
    StoneGenerationUpgrade3,
    StoneGenerationUpgrade4,
    UniversalItemGeneration1,
    UniversalItemGeneration2,
    UniversalItemGeneration3,
    UniversalItemGeneration4,

    BreakerUpgrade,
    PlacerUpgrade,
    RefillUpgrade,
    DimensionalRefillUpgrade,
    SpeedUpgradeAugment,

    ConfigurationTool,
    LinkingTool,;

    public boolean mHasNotBeenSet = true;
    public ItemStack mStack;

    public Item getItem() {
        sanityCheck();
        return isStackInvalid(mStack) ? null : mStack.getItem();
    }

    public Block getBlock() {
        Item item = getItem();
        return item == null ? null : Block.getBlockFromItem(item);
    }

    public FSItemList set(Item item) {
        if (item == null) {
            return this;
        }
        return set(new ItemStack(item, 1, 0));
    }

    public FSItemList set(Block block) {
        if (block == null) {
            return this;
        }
        return set(Item.getItemFromBlock(block));
    }

    public FSItemList set(ItemStack stack) {
        if (isStackInvalid(stack)) {
            return this;
        }
        mHasNotBeenSet = false;
        mStack = copyAmount(1, stack);
        return this;
    }

    public boolean hasBeenSet() {
        return !mHasNotBeenSet;
    }

    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ItemStack getStack() {
        sanityCheck();
        return isStackInvalid(mStack) ? null : mStack.copy();
    }

    public ItemStack get() {
        return get(1);
    }

    public ItemStack get(int amount) {
        sanityCheck();
        if (isStackInvalid(mStack)) {
            throw new IllegalStateException("The Enum '" + name() + "' has not been set to an Item at this time!");
        }
        return copyAmount(amount, mStack);
    }

    public void sanityCheck() {}

    public static boolean isStackInvalid(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize < 0;
    }

    public static ItemStack copyAmount(int amount, ItemStack stack) {
        if (isStackInvalid(stack)) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = Math.max(0, amount);
        return copy;
    }

    public static FSItemList byName(String name) {
        if (name == null) {
            return null;
        }
        for (FSItemList entry : values()) {
            if (entry.getId()
                .equals(name)) {
                return entry;
            }
        }
        return null;
    }
}
