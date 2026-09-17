package com.hfstudio.functionalstorage.common.interaction;

import net.minecraft.util.EnumChatFormatting;

public interface ActionBarFeedback {

    default void showActionBarFeedback(String key, Object... arguments) {}

    default void showActionBarFeedback(String key, EnumChatFormatting color, Object... arguments) {}
}
