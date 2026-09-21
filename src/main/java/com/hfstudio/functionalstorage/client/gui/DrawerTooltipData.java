package com.hfstudio.functionalstorage.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.functionalstorage.common.block.base.DrawerBlock;
import com.hfstudio.functionalstorage.common.integration.Mods;
import com.hfstudio.functionalstorage.common.item.upgrade.AutomationUpgradeItem;
import com.hfstudio.functionalstorage.common.item.upgrade.UpgradeSettings;
import com.hfstudio.functionalstorage.util.ItemUtil;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;

@SideOnly(Side.CLIENT)
public class DrawerTooltipData {

    public record AspectIcon(ResourceLocation texture, int color, String name) {

        @Optional.Method(modid = "Thaumcraft")
        public static AspectIcon of(Aspect aspect) {
            return new AspectIcon(aspect.getImage(), aspect.getColor(), aspect.getName());
        }
    }

    public record Entry(ItemStack item, FluidStack fluid, AspectIcon aspect, String amount) {

        public String name() {
            return item != null ? item.getDisplayName() : fluid != null ? fluid.getLocalizedName() : aspect.name();
        }
    }

    public record Section(String title, List<Entry> entries) {}

    private static List<Item> frequencyItems;
    private static final Cache<String, List<ItemStack>> FREQUENCY_CACHE = CacheBuilder.newBuilder()
        .maximumSize(256)
        .build();

    public static List<Section> read(ItemStack stack) {
        List<Section> sections = new ArrayList<>();
        if (stack == null) {
            return sections;
        }
        if (stack.getItem() instanceof AutomationUpgradeItem upgrade) {
            List<Entry> filters = new ArrayList<>();
            for (int slot = 0; slot < UpgradeSettings.FILTER_SLOTS; slot++) {
                ItemStack filter = UpgradeSettings.getFilter(stack, slot);
                if (filter != null) filters.add(new Entry(filter, null, null, ""));
            }
            if (!filters.isEmpty()) sections.add(new Section("functionalstorage.upgrade.filtered", filters));
            List<Entry> attachments = new ArrayList<>();
            for (String key : UpgradeSettings.NESTED_SLOT_KEYS) {
                ItemStack attachment = UpgradeSettings.getStack(stack, key);
                if (attachment != null)
                    attachments.add(new Entry(attachment, null, null, Integer.toString(attachment.stackSize)));
            }
            if (!attachments.isEmpty()) sections.add(new Section("drawer.block.upgrades", attachments));
        }
        NBTTagCompound tile = DrawerBlock.getTileData(stack);
        if (tile == null) {
            return sections;
        }
        List<StorageContentEntry> contents = new ArrayList<>();
        readEntries(
            contents,
            tile.getCompoundTag("Items")
                .getTagList("Entries", 10),
            ContentKind.ITEM);
        readEntries(
            contents,
            tile.getCompoundTag("Tanks")
                .getTagList("Entries", 10),
            ContentKind.FLUID);
        readEntries(
            contents,
            tile.getCompoundTag("Aspects")
                .getTagList("Entries", 10),
            ContentKind.ASPECT);
        readCompactingTiers(contents, tile);
        if (!contents.isEmpty()) {
            // A drawer may spread one resource over several slots, so entries are
            // merged by resource identity before display. Item and fluid identity
            // both include NBT, keeping differently configured stacks apart.
            List<Entry> merged = new ArrayList<>();
            for (StorageContentEntry entry : StorageContentEntry.merge(contents)) {
                merged.add(toEntry(entry));
            }
            sections.add(new Section("drawer.block.contents", merged));
        }
        List<Entry> upgrades = new ArrayList<>();
        for (String key : new String[] { "StorageUpgrades", "UtilityUpgrades" }) {
            NBTTagList list = tile.getTagList(key, 10);
            for (int index = 0; index < list.tagCount(); index++) {
                ItemStack item = ItemUtil.readStack(list.getCompoundTagAt(index));
                if (item != null) {
                    upgrades.add(new Entry(item, null, null, ""));
                }
            }
        }
        if (!upgrades.isEmpty()) {
            sections.add(new Section("drawer.block.upgrades", upgrades));
        }
        return sections;
    }

    /**
     * Converts a merged content entry into a display entry, formatting the amount
     * with the unit that matches the resource kind.
     *
     * @param entry merged content entry
     * @return the display entry
     */
    private static Entry toEntry(StorageContentEntry entry) {
        if (entry.item() != null) {
            return new Entry(entry.item(), null, null, NumberFormatUtil.formatNumberCompact(entry.amount()));
        }
        if (entry.fluid() != null) {
            return new Entry(null, entry.fluid(), null, NumberFormatUtil.formatFluid(entry.amount()));
        }
        return new Entry(null, null, entry.aspect(), NumberFormatUtil.formatNumberCompact(entry.amount()));
    }

    /**
     * Reads the compacted tiers a compacting drawer exposes as stored content.
     *
     * @param contents accumulator
     * @param tile     drawer tile data
     */
    private static void readCompactingTiers(List<StorageContentEntry> contents, NBTTagCompound tile) {
        NBTTagCompound compacting = tile.getCompoundTag("Compacting");
        NBTTagList tiers = compacting.getTagList("Tiers", 10);
        for (int index = 0; index < tiers.tagCount(); index++) {
            NBTTagCompound tier = tiers.getCompoundTagAt(index);
            ItemStack item = ItemUtil.readStack(tier.getCompoundTag("Template"));
            if (item != null) {
                long count = compacting.getLong("BaseAmount") / Math.max(1L, tier.getLong("BaseUnits"));
                contents.add(StorageContentEntry.ofItem(item, count));
            }
        }
    }

    private static void readEntries(List<StorageContentEntry> entries, NBTTagList list, ContentKind kind) {
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            NBTTagCompound template = entry.getCompoundTag("Template");
            long count = entry.getLong("Amount");
            if (count <= 0L) {
                continue;
            }
            if (kind == ContentKind.ITEM) {
                ItemStack item = ItemUtil.readStack(template);
                if (item != null) {
                    entries.add(StorageContentEntry.ofItem(item, count));
                }
            } else if (kind == ContentKind.FLUID) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(template);
                if (fluid != null) {
                    entries.add(StorageContentEntry.ofFluid(fluid, count));
                }
            } else if (Mods.Thaumcraft.isModLoaded()) {
                readAspectEntry(entries, template, count);
            }
        }
    }

    @Optional.Method(modid = "Thaumcraft")
    private static void readAspectEntry(List<StorageContentEntry> entries, NBTTagCompound template, long count) {
        Aspect aspect = Aspect.getAspect(template.getString("Aspect"));
        if (aspect != null) {
            entries.add(StorageContentEntry.ofAspect(AspectIcon.of(aspect), count));
        }
    }

    /**
     * Which accumulator a storage list feeds, so amounts are formatted with the
     * right unit once entries are merged.
     */
    private enum ContentKind {
        ITEM,
        FLUID,
        ASPECT
    }

    public static List<ItemStack> frequencyDisplay(String frequency) {
        List<ItemStack> cached = FREQUENCY_CACHE.getIfPresent(frequency);
        if (cached != null) return cached;
        if (frequencyItems == null) {
            frequencyItems = new ArrayList<>();
            for (Object object : Item.itemRegistry) {
                if (object instanceof Item item && !(item instanceof ItemBlock)
                    && Item.itemRegistry.getNameForObject(item)
                        .startsWith("minecraft:")) {
                    frequencyItems.add(item);
                }
            }
            frequencyItems.sort(Comparator.comparing(Item.itemRegistry::getNameForObject));
        }
        List<ItemStack> result = new ArrayList<>();
        if (!frequencyItems.isEmpty() && !frequency.isEmpty()) {
            for (String part : frequency.split("-")) {
                result.add(new ItemStack(frequencyItems.get(Math.floorMod(part.hashCode(), frequencyItems.size()))));
            }
        }
        List<ItemStack> symbols = List.copyOf(result);
        FREQUENCY_CACHE.put(frequency, symbols);
        return symbols;
    }

}
