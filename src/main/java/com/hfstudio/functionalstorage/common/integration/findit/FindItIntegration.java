package com.hfstudio.functionalstorage.common.integration.findit;

import com.hfstudio.functionalstorage.client.integration.findit.DrawerNetworkHighlightOverlay;

import cpw.mods.fml.common.Optional;

public class FindItIntegration {

    @Optional.Method(modid = "findit")
    public static void register(boolean client) {
        if (!DrawerNetworkFindProvider.register()) {
            return;
        }
        if (client) {
            DrawerNetworkHighlightOverlay.register();
        }
    }
}
