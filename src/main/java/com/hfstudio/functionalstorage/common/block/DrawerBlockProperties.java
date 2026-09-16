package com.hfstudio.functionalstorage.common.block;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

/** Shared GTNHLib state properties for block and item model selection. */
public class DrawerBlockProperties {

    public static final AttachmentBlockProperty ATTACHMENT = new AttachmentBlockProperty();
    public static final HorizontalFacingBlockProperty HORIZONTAL_FACING = new HorizontalFacingBlockProperty();

    private DrawerBlockProperties() {}

    public static void register() {
        for (Block block : RegistrationHandler.allDrawerBlocks()) {
            BlockPropertyRegistry.registerBlockItemProperty(block, ATTACHMENT, DrawerAttachment.WALL);
            BlockPropertyRegistry.registerBlockItemProperty(block, HORIZONTAL_FACING, ForgeDirection.NORTH);
        }
    }

    @Nonnull
    public static DrawerAttachment attachmentOf(@Nonnull ItemStack stack) {
        return ATTACHMENT.getValue(itemMetadata(stack));
    }

    @Nonnull
    public static ForgeDirection facingOf(@Nonnull ItemStack stack) {
        return HORIZONTAL_FACING.getValue(itemMetadata(stack));
    }

    private static int itemMetadata(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ItemBlock ? item.getMetadata(stack.getItemDamage()) : stack.getItemDamage();
    }

    public static boolean isDrawer(Block block) {
        return block instanceof DrawerBlock;
    }
}
