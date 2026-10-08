package com.hfstudio.functionalstorage.client.gui;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

public class StorageAmountFormatter {

    private static final FormatOptions FULL_TOOLTIP_OPTIONS = new FormatOptions().disableExponentialFormatting();

    public static String formatDisplay(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluidCompact(amount) : NumberFormatUtil.formatNumberCompact(amount);
    }

    public static String formatTooltip(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluid(amount, FULL_TOOLTIP_OPTIONS)
            : NumberFormatUtil.formatNumber(amount, FULL_TOOLTIP_OPTIONS);
    }
}
