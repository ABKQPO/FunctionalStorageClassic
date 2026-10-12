package com.hfstudio.functionalstorage.client.gui;

import java.math.RoundingMode;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

public class StorageAmountFormatter {

    private static final FormatOptions FULL_TOOLTIP_OPTIONS = new FormatOptions().disableExponentialFormatting();
    private static final CompactOptions SLOT_OPTIONS = new CompactOptions().setDecimalPlaces(0)
        .setRoundingMode(RoundingMode.DOWN);

    public static String formatDisplay(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluidCompact(amount) : NumberFormatUtil.formatNumberCompact(amount);
    }

    public static String formatSlot(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluidCompact(amount, SLOT_OPTIONS)
            : NumberFormatUtil.formatNumberCompact(amount, SLOT_OPTIONS);
    }

    public static String formatTooltip(long amount, boolean fluid) {
        return fluid ? NumberFormatUtil.formatFluid(amount, FULL_TOOLTIP_OPTIONS)
            : NumberFormatUtil.formatNumber(amount, FULL_TOOLTIP_OPTIONS);
    }
}
