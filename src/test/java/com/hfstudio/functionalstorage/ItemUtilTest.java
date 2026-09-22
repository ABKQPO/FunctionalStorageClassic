package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;
import com.hfstudio.functionalstorage.util.ItemUtil;

/**
 * Asserts the shared item comparison and parsing helpers.
 * <p>
 * Every storage decision about items passes through these helpers, so a comparison that
 * ignored NBT would merge a named tool with a plain one, and one that considered the
 * stack count would refuse to top up a partly filled slot. The parsing helpers read
 * configuration written by hand, where a malformed entry must be refused rather than
 * resolved to some unrelated item.
 */
public class ItemUtilTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("comparison ignores the stack count")
    void comparisonIgnoresCount() {
        Item item = StorageFixtures.newItem();

        assertTrue(
            ItemUtil.areItemStacksEqual(StorageFixtures.stack(item, 1), StorageFixtures.stack(item, 64)),
            "the same item at different counts must compare equal");
    }

    @Test
    @DisplayName("comparison separates different items and different metadata")
    void comparisonSeparatesIdentity() {
        Item first = StorageFixtures.newItem();
        Item second = StorageFixtures.newItem();

        assertFalse(
            ItemUtil.areItemStacksEqual(StorageFixtures.one(first), StorageFixtures.one(second)),
            "different items must not compare equal");
        assertFalse(
            ItemUtil.areItemStacksEqual(new ItemStack(first, 1, 0), new ItemStack(first, 1, 5)),
            "different metadata must not compare equal");
    }

    @Test
    @DisplayName("comparison separates stacks that differ only in NBT")
    void comparisonSeparatesNbt() {
        Item item = StorageFixtures.newItem();
        ItemStack plain = new ItemStack(item, 1);
        ItemStack named = new ItemStack(item, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "named");
        named.setTagCompound(tag);

        assertFalse(ItemUtil.areItemStacksEqual(plain, named), "a named stack must not compare equal to a plain one");

        ItemStack alsoNamed = new ItemStack(item, 1);
        alsoNamed.setTagCompound((NBTTagCompound) tag.copy());
        assertTrue(ItemUtil.areItemStacksEqual(named, alsoNamed), "identical NBT must compare equal");
    }

    @Test
    @DisplayName("comparison treats a null or itemless stack as equal to nothing")
    void comparisonHandlesNulls() {
        Item item = StorageFixtures.newItem();
        ItemStack real = StorageFixtures.one(item);

        assertFalse(ItemUtil.areItemStacksEqual(null, real), "a null stack must not equal a real one");
        assertFalse(ItemUtil.areItemStacksEqual(real, null), "a real stack must not equal null");
        assertFalse(ItemUtil.areItemStacksEqual(null, null), "two null stacks must not be reported as equal");
        assertFalse(
            ItemUtil.areItemStacksEqual(new ItemStack((Item) null, 1), real),
            "a stack holding no item must not equal a real one");
    }

    @Test
    @DisplayName("compatibility matches exactly regardless of widening")
    void compatibilityMatchesExactly() {
        Item item = StorageFixtures.newItem();
        ItemStack template = StorageFixtures.one(item);
        ItemStack sameItem = StorageFixtures.stack(item, 32);

        assertTrue(
            ItemUtil.areItemStacksCompatible(template, sameItem, false),
            "an exact match must be compatible even without widening");
        assertTrue(
            ItemUtil.areItemStacksCompatible(template, sameItem, true),
            "an exact match must stay compatible with widening enabled");

        // Two unrelated items share no ore entry, so widening cannot make them
        // compatible. The ore dictionary cannot be queried without a live item
        // registry, so this checks the exact-match path only.
        assertFalse(
            ItemUtil.areItemStacksCompatible(template, StorageFixtures.one(StorageFixtures.newItem()), false),
            "different items must not be compatible when widening is off");
    }

    @Test
    @DisplayName("an item description resolves by name and refuses a malformed one")
    void descriptionParsingIsStrict() {
        assertNull(ItemUtil.itemFromDescription(null), "a null description must resolve to nothing");
        assertNull(ItemUtil.itemFromDescription(""), "an empty description must resolve to nothing");
        assertNull(ItemUtil.itemFromDescription("   "), "a blank description must resolve to nothing");
        assertNull(
            ItemUtil.itemFromDescription("nocolon"),
            "a description without a namespace must resolve to nothing");
        assertNull(ItemUtil.itemFromDescription("modid:not_a_real_item"), "an unknown name must resolve to nothing");
    }

    @Test
    @DisplayName("metadata parsing reads the third field and defaults for anything else")
    void metadataParsingIsStrict() {
        assertEquals(7, ItemUtil.metadataFromDescription("modid:name:7"), "a metadata field must be read");
        assertEquals(0, ItemUtil.metadataFromDescription("modid:name"), "a missing metadata field must default");
        assertEquals(0, ItemUtil.metadataFromDescription("modid"), "a bare name must default");
        assertEquals(0, ItemUtil.metadataFromDescription(null), "a null description must default");
        assertEquals(
            0,
            ItemUtil.metadataFromDescription("modid:name:notanumber"),
            "an unparsable metadata field must default rather than resolve to something arbitrary");
    }

    @Test
    @DisplayName("a copied template is always a detached count-one stack")
    void copiedTemplateIsDetached() {
        Item item = StorageFixtures.newItem();
        ItemStack source = new ItemStack(item, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "original");
        source.setTagCompound(tag);

        ItemStack copy = ItemUtil.copyWithSizeOne(source);
        assertTrue(copy != null, "a real stack must copy");
        assertEquals(1, copy.stackSize, "a template copy must hold exactly one");

        copy.setItemDamage(9);
        copy.getTagCompound()
            .setString("display", "edited");

        assertEquals(0, source.getItemDamage(), "editing the copy must not change the source");
        assertEquals(
            "original",
            source.getTagCompound()
                .getString("display"),
            "editing the copy's NBT must not change the source");
        assertNotEquals(9, source.getItemDamage(), "the source metadata must be untouched");
    }

    @Test
    @DisplayName("copying nothing yields nothing")
    void copyingNothingYieldsNothing() {
        assertNull(ItemUtil.copyWithSizeOne(null), "a null stack must copy to nothing");
        assertNull(
            ItemUtil.copyWithSizeOne(new ItemStack((Item) null, 1)),
            "a stack holding no item must copy to nothing");
    }

    @Test
    @DisplayName("reading an absent or empty tag yields nothing")
    void readingTagsIsStrict() {
        assertNull(ItemUtil.readStack(null), "a null tag must read as nothing");
        assertNull(ItemUtil.readStack(new NBTTagCompound()), "an empty tag must read as nothing");
    }
}
