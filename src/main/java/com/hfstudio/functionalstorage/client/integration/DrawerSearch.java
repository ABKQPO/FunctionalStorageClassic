package com.hfstudio.functionalstorage.client.integration;

import java.util.function.BiPredicate;

import com.asdflj.nech.API;

import cpw.mods.fml.common.Loader;

public class DrawerSearch {

    private static final BiPredicate<String, String> MATCHER = Loader.isModLoaded("nech") ? new PinyinMatcher()
        : String::contains;

    public static boolean contains(String text, String query) {
        return MATCHER.test(text, query);
    }

    public static class PinyinMatcher implements BiPredicate<String, String> {

        @Override
        public boolean test(String text, String query) {
            return API.INSTANCE.contains(text, query);
        }
    }
}
