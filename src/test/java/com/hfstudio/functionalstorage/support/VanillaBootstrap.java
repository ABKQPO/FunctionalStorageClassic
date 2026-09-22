package com.hfstudio.functionalstorage.support;

import java.lang.reflect.Field;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

import sun.misc.Unsafe;

/**
 * Supplies the single piece of vanilla state a headless harness cannot build.
 *
 * <p>
 * GTNHLib reads an item's metadata through {@code Items.feather}, a plain vanilla
 * item whose only job is to hand back the damage value stored on the stack. That field
 * is populated from the game's item registry, which is empty here, so every call through
 * GTNHLib's inventory bridge would dereference null; an ordinary item restores exactly
 * the game's behaviour, since the called method only returns the value already on the
 * stack. The field is written after its owning class has initialized, because that
 * initializer assigns every field unconditionally as its last act.
 * </p>
 */
public class VanillaBootstrap {

    private static boolean installed;

    private VanillaBootstrap() {}

    /**
     * Makes the vanilla metadata accessor usable. Safe to call repeatedly and from
     * several tests.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        if (Items.feather == null) {
            putStatic(Items.class, "feather", new Item());
        }
    }

    /**
     * Writes a static field directly.
     *
     * <p>
     * Reflection refuses to write a static final field, and this one is final. The
     * field is private to the harness and never escapes it.
     * </p>
     */
    private static void putStatic(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            Unsafe unsafe = theUnsafe();
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("cannot install the vanilla metadata accessor", failure);
        }
    }

    private static Unsafe theUnsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
