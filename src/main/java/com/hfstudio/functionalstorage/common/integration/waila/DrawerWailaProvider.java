package com.hfstudio.functionalstorage.common.integration.waila;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.BigFluidStack;
import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.tile.EssentiaDrawerTile;
import com.hfstudio.functionalstorage.common.tile.FluidDrawerTile;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;

import cpw.mods.fml.common.Optional;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

/** Reports drawer contents, capacity, and lock state through Waila. */
@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila", striprefs = true)
public class DrawerWailaProvider implements IWailaDataProvider {

    @Optional.Method(modid = "Waila")
    public static void register(IWailaRegistrar registrar) {
        DrawerWailaProvider provider = new DrawerWailaProvider();
        registrar.registerBodyProvider(provider, DrawerBlock.class);
        registrar.registerNBTProvider(provider, DrawerBlock.class);
    }

    @Override
    @Optional.Method(modid = "Waila")
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    @Optional.Method(modid = "Waila")
    public List<String> getWailaHead(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        return tooltip;
    }

    @Override
    @Optional.Method(modid = "Waila")
    public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        NBTTagCompound tag = accessor.getNBTData();
        if (tag == null) {
            return tooltip;
        }
        int slots = tag.getInteger("Slots");
        for (int index = 0; index < slots; index++) {
            String name = tag.getString("Name" + index);
            if (name.isEmpty()) {
                continue;
            }
            long amount = tag.getLong("Amount" + index);
            long capacity = tag.getLong("Capacity" + index);
            tooltip.add(
                name + ": "
                    + NumberFormatUtil.formatNumberCompact(amount)
                    + " / "
                    + NumberFormatUtil.formatNumberCompact(capacity));
        }
        if (tag.getBoolean("Locked")) {
            tooltip.add(StatCollector.translateToLocal("functionalstorage.drawer.locked"));
        }
        int upgrades = tag.getInteger("Upgrades");
        if (upgrades > 0) {
            tooltip.add(
                StatCollector
                    .translateToLocalFormatted("functionalstorage.drawer.upgrades", Integer.toString(upgrades)));
        }
        return tooltip;
    }

    @Override
    @Optional.Method(modid = "Waila")
    public List<String> getWailaTail(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        return tooltip;
    }

    @Override
    @Optional.Method(modid = "Waila")
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x,
        int y, int z) {
        if (!(tile instanceof ControllableDrawerTile drawer)) {
            return tag;
        }
        tag.setBoolean("Locked", drawer.isLocked());

        int upgrades = 0;
        for (int slot = 0; slot < drawer.getStorageUpgradeSlots(); slot++) {
            if (drawer.getStorageUpgrade(slot) != null) {
                upgrades++;
            }
        }
        for (int slot = 0; slot < drawer.getUtilityUpgradeSlots(); slot++) {
            if (drawer.getUtilityUpgrade(slot) != null) {
                upgrades++;
            }
        }
        tag.setInteger("Upgrades", upgrades);

        if (tile instanceof FluidDrawerTile) {
            writeFluidTanks((FluidDrawerTile) tile, tag);
        } else if (Mods.Thaumcraft.isModLoaded() && tile instanceof EssentiaDrawerTile) {
            writeAspectSlots((EssentiaDrawerTile) tile, tag);
        } else {
            writeItemSlots(drawer, tag);
        }
        return tag;
    }

    private void writeItemSlots(ControllableDrawerTile drawer, NBTTagCompound tag) {
        if (drawer.getItemHandler() == null) {
            return;
        }
        int count = drawer.getItemHandler()
            .getStorageCount();
        tag.setInteger("Slots", count);
        for (int index = 0; index < count; index++) {
            BigItemStack snapshot = drawer.getItemHandler()
                .getSnapshot(index);
            ItemStack template = snapshot.getTemplate();
            if (template == null) {
                continue;
            }
            tag.setString("Name" + index, template.getDisplayName());
            tag.setLong("Amount" + index, snapshot.getAmount());
            tag.setLong(
                "Capacity" + index,
                drawer.getItemHandler()
                    .getCapacity(index));
        }
    }

    private void writeFluidTanks(FluidDrawerTile tile, NBTTagCompound tag) {
        int count = tile.getFluidHandler()
            .getStorageCount();
        tag.setInteger("Slots", count);
        for (int index = 0; index < count; index++) {
            BigFluidStack snapshot = tile.getFluidHandler()
                .getSnapshot(index);
            if (snapshot.getTemplate() == null) {
                continue;
            }
            tag.setString(
                "Name" + index,
                snapshot.getTemplate()
                    .getLocalizedName());
            tag.setLong("Amount" + index, snapshot.getAmount());
            tag.setLong(
                "Capacity" + index,
                tile.getFluidHandler()
                    .getCapacity(index));
        }
    }

    @Optional.Method(modid = "Thaumcraft")
    private void writeAspectSlots(EssentiaDrawerTile tile, NBTTagCompound tag) {
        int count = tile.getAspectHandler()
            .getStorageCount();
        tag.setInteger("Slots", count);
        for (int index = 0; index < count; index++) {
            BigAspectStack snapshot = tile.getAspectHandler()
                .getSnapshot(index);
            if (snapshot.getAspect() == null) {
                continue;
            }
            tag.setString(
                "Name" + index,
                snapshot.getAspect()
                    .getLocalizedDescription());
            tag.setLong("Amount" + index, snapshot.getAmount());
            tag.setLong(
                "Capacity" + index,
                tile.getAspectHandler()
                    .getCapacity(index));
        }
    }
}
