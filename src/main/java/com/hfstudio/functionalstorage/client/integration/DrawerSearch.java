package com.hfstudio.functionalstorage.client.integration;

import java.util.function.BiPredicate;

import com.asdflj.nech.API;
import com.hfstudio.functionalstorage.common.integration.Mods;

public class DrawerSearch {

    private static final BiPredicate<String, String> MATCHER = Mods.NeverEnoughCharacters.isModLoaded()
        ? new PinyinMatcher()
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
