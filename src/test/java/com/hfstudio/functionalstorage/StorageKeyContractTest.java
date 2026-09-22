package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.AspectStorageKey;
import com.hfstudio.functionalstorage.api.storage.FluidStorageKey;
import com.hfstudio.functionalstorage.api.storage.ItemStorageKey;
import com.hfstudio.functionalstorage.api.storage.StorageKey;
import com.hfstudio.functionalstorage.support.AspectBootstrap;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

import thaumcraft.api.aspects.Aspect;

/**
 * Asserts that resource identity is stable and correct for every resource kind.
 * <p>
 * These keys are what an aggregate groups its contents by, so two keys that should be
 * equal but are not would split one pile into two, and two keys that are equal but
 * should not be would merge unrelated resources. Each check therefore pins the whole
 * contract: equality, its hash, and the fact that a key never changes after a stack it
 * was built from is edited.
 */
public class StorageKeyContractTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
        AspectBootstrap.install();
    }

    @Test
    @DisplayName("the same item produces equal item keys with equal hashes")
    void identicalItemKeysAreEqual() {
        Item item = StorageFixtures.newItem();
        ItemStorageKey first = new ItemStorageKey(StorageFixtures.stack(item, 1));
        ItemStorageKey second = new ItemStorageKey(StorageFixtures.stack(item, 64));

        assertEquals(first, second, "the stack count must not affect item identity");
        assertEquals(first.hashCode(), second.hashCode(), "equal keys must share a hash");
        assertEquals(1, setOf(first, second).size(), "a set must hold one of two equal keys");
    }

    @Test
    @DisplayName("different items produce different item keys")
    void differentItemKeysAreNotEqual() {
        ItemStorageKey first = new ItemStorageKey(StorageFixtures.stack(StorageFixtures.newItem(), 1));
        ItemStorageKey second = new ItemStorageKey(StorageFixtures.stack(StorageFixtures.newItem(), 1));

        assertNotEquals(first, second, "different items must not share identity");
        assertEquals(2, setOf(first, second).size(), "a set must hold both distinct keys");
    }

    @Test
    @DisplayName("metadata distinguishes item keys")
    void metadataDistinguishesItemKeys() {
        Item item = StorageFixtures.newItem();
        ItemStorageKey plain = new ItemStorageKey(new ItemStack(item, 1, 0));
        ItemStorageKey variant = new ItemStorageKey(new ItemStack(item, 1, 7));

        assertNotEquals(plain, variant, "different metadata must be a different item");
        assertEquals(2, setOf(plain, variant).size(), "a set must hold both damage variants");
    }

    @Test
    @DisplayName("stack NBT distinguishes item keys, and an unrelated key is not merged")
    void nbtDistinguishesItemKeys() {
        Item item = StorageFixtures.newItem();

        ItemStack plainStack = new ItemStack(item, 1);
        ItemStack namedStack = new ItemStack(item, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "named");
        namedStack.setTagCompound(tag);

        ItemStorageKey plain = new ItemStorageKey(plainStack);
        ItemStorageKey named = new ItemStorageKey(namedStack);

        assertNotEquals(plain, named, "differing NBT must be a different item");
        assertEquals(2, setOf(plain, named).size(), "a set must hold both NBT variants");
    }

    @Test
    @DisplayName("a key is unaffected by editing the stack it was built from")
    void keyIsDetachedFromItsSourceStack() {
        Item item = StorageFixtures.newItem();
        ItemStack source = new ItemStack(item, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "original");
        source.setTagCompound(tag);

        ItemStorageKey key = new ItemStorageKey(source);

        // The caller keeps its stack and edits it after handing it over.
        source.getTagCompound()
            .setString("display", "edited");
        source.setItemDamage(9);

        ItemStack rebuilt = key.toItemStack();
        assertEquals(0, rebuilt.getItemDamage(), "editing the source must not change the key's metadata");
        assertEquals(
            "original",
            rebuilt.getTagCompound()
                .getString("display"),
            "editing the source must not change the key's NBT");
    }

    @Test
    @DisplayName("a key never hands out its internal NBT for mutation")
    void exposedNbtIsACopy() {
        Item item = StorageFixtures.newItem();
        ItemStack source = new ItemStack(item, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("display", "kept");
        source.setTagCompound(tag);
        ItemStorageKey key = new ItemStorageKey(source);

        key.getTag()
            .setString("display", "tampered");
        key.toItemStack()
            .getTagCompound()
            .setString("display", "tampered");

        assertEquals(
            "kept",
            key.getTag()
                .getString("display"),
            "an exposed tag must be a copy, so the key cannot be altered from outside");
        assertEquals(key, new ItemStorageKey(source), "the key must still equal one built from the unchanged stack");
    }

    @Test
    @DisplayName("an item key refuses a stack that holds no item")
    void itemKeyRefusesEmptyStack() {
        assertThrows(NullPointerException.class, () -> new ItemStorageKey(null), "a null stack must be refused");
        assertThrows(
            IllegalArgumentException.class,
            () -> new ItemStorageKey(new ItemStack((Item) null, 1)),
            "a stack with no item must be refused");
    }

    @Test
    @DisplayName("fluid keys compare by fluid and NBT")
    void fluidKeysCompareByIdentityAndNbt() {
        var fluid = StorageFixtures.fluid("key_fluid");
        FluidStorageKey plain = new FluidStorageKey(StorageFixtures.reusableFluidStack(fluid));

        assertEquals(
            plain,
            new FluidStorageKey(StorageFixtures.reusableFluidStack(fluid)),
            "the same fluid must be equal");
        assertEquals(
            plain.hashCode(),
            new FluidStorageKey(StorageFixtures.reusableFluidStack(fluid)).hashCode(),
            "hashes must agree");

        var other = StorageFixtures.fluid("key_other");
        assertNotEquals(
            plain,
            new FluidStorageKey(StorageFixtures.reusableFluidStack(other)),
            "different fluids must differ");
        assertEquals(
            1,
            setOf(plain, new FluidStorageKey(StorageFixtures.reusableFluidStack(fluid))).size(),
            "a set must hold one");
    }

    @Test
    @DisplayName("aspect keys compare by aspect identity")
    void aspectKeysCompareByAspect() {
        Aspect first = AspectBootstrap.firstAspect();
        Aspect second = AspectBootstrap.secondAspect();

        assertEquals(new AspectStorageKey(first), new AspectStorageKey(first), "the same aspect must be equal");
        assertNotEquals(new AspectStorageKey(first), new AspectStorageKey(second), "different aspects must differ");
        assertEquals(
            1,
            setOf(new AspectStorageKey(first), new AspectStorageKey(first)).size(),
            "a set must hold one entry for a repeated aspect");
    }

    @Test
    @DisplayName("a key of one kind never equals a key of another kind")
    void keysOfDifferentKindsDiffer() {
        StorageKey itemKey = new ItemStorageKey(StorageFixtures.stack(StorageFixtures.newItem(), 1));
        StorageKey aspectKey = new AspectStorageKey(AspectBootstrap.firstAspect());
        StorageKey fluidKey = new FluidStorageKey(
            StorageFixtures.reusableFluidStack(StorageFixtures.fluid("kind_fluid")));

        assertNotEquals(itemKey, aspectKey, "an item key must not equal an aspect key");
        assertNotEquals(itemKey, fluidKey, "an item key must not equal a fluid key");
        assertNotEquals(aspectKey, fluidKey, "an aspect key must not equal a fluid key");
    }

    @Test
    @DisplayName("a key is usable as a map key across repeated lookups")
    void keysAreStableInHashedCollections() {
        Item item = StorageFixtures.newItem();
        Set<StorageKey> keys = new HashSet<>();
        for (int round = 0; round < 100; round++) {
            keys.add(new ItemStorageKey(StorageFixtures.stack(item, 1 + round)));
        }
        assertEquals(1, keys.size(), "one item must occupy one entry however often it is added");
        assertTrue(
            keys.contains(new ItemStorageKey(StorageFixtures.stack(item, 1))),
            "a freshly built equal key must be found in the set");
    }

    /**
     * Puts keys into a hashed set, so an inconsistent hash shows up as a duplicate.
     */
    private static Set<Object> setOf(Object... keys) {
        Set<Object> set = new HashSet<>();
        for (Object key : keys) {
            set.add(key);
        }
        return set;
    }
}
