package com.hfstudio.functionalstorage.common.integration.holoinventory;

import net.dries007.holoInventory.compat.InventoryDecoder;
import net.dries007.holoInventory.compat.InventoryDecoderRegistry;
import net.dries007.holoInventory.util.NBTKeys;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;

public class HoloInventoryIntegration {

    @Optional.Method(modid = "holoinventory")
    public static void register() {
        InventoryDecoderRegistry.register(new InventoryDecoder(ControllableDrawerTile.class) {

            @Override
            public NBTTagList toNBT(IInventory inventory) {
                NBTTagList result = new NBTTagList();
                IBigItemHandler handler = ((ControllableDrawerTile) inventory).getItemHandler();
                if (handler == null) {
                    return result;
                }
                for (int slot = 0; slot < handler.getStorageCount(); slot++) {
                    BigItemStack snapshot = handler.getSnapshot(slot);
                    if (!snapshot.hasTemplate() || (snapshot.getAmount() == 0L && !handler.isLocked(slot))) {
                        continue;
                    }
                    ItemStack stack = snapshot.getTemplate();
                    NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
                    tag.setInteger(NBTKeys.NBT_KEY_COUNT, (int) Math.min(snapshot.getAmount(), Integer.MAX_VALUE));
                    result.appendTag(tag);
                }
                return result;
            }
        });
    }
}
