package com.hfstudio.functionalstorage.common.block;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.init.BlockPropertyInit;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.misc.RegistrationHandler;

/**
 * Registers the GTNHLib block properties used by every drawer, so GTNHLib's
 * JSON model pipeline can resolve states and rotate them with
 * {@code horizontal_facing} and {@code attachment} variant selectors.
 */
public class DrawerBlockProperties {

    public static final AttachmentBlockProperty ATTACHMENT = new AttachmentBlockProperty();
    public static final HorizontalFacingBlockProperty HORIZONTAL_FACING = new HorizontalFacingBlockProperty();

    private DrawerBlockProperties() {}

    /**
     * Registers the properties for every drawer block and matching item block.
     */
    public static void register() {
        BlockPropertyInit.init();
        for (Block block : RegistrationHandler.allDrawerBlocks()) {
            BlockPropertyRegistry.registerBlockItemProperty(block, ATTACHMENT, DrawerAttachment.WALL);
            BlockPropertyRegistry.registerBlockItemProperty(block, HORIZONTAL_FACING, ForgeDirection.NORTH);
        }
    }

    /**
     * @param stack drawer item stack
     * @return the attachment encoded in the stack metadata
     */
    @Nonnull
    public static DrawerAttachment attachmentOf(@Nonnull ItemStack stack) {
        return ATTACHMENT.getValue(itemMetadata(stack));
    }

    /**
     * @param stack drawer item stack
     * @return the horizontal facing encoded in the stack metadata
     */
    @Nonnull
    public static ForgeDirection facingOf(@Nonnull ItemStack stack) {
        return HORIZONTAL_FACING.getValue(itemMetadata(stack));
    }

    private static int itemMetadata(@Nonnull ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ItemBlock ? item.getMetadata(stack.getItemDamage()) : stack.getItemDamage();
    }

    /**
     * @param block candidate block
     * @return whether the block is a drawer
     */
    public static boolean isDrawer(Block block) {
        return block instanceof DrawerBlock;
    }
}
