package com.hfstudio.functionalstorage.client.gui;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public class StorageAmountFormatter {

    public static String formatDisplay(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluidCompact(amount) : NumberFormatUtil.formatNumberCompact(amount);
    }

    public static String formatTooltip(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluid(amount) : NumberFormatUtil.formatNumber(amount);
    }
}
