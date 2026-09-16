package com.hfstudio.functionalstorage.util;

import java.util.Locale;

/** Compact numeric formatting used by drawer face labels. */
public class NumberUtils {

    private static final String[] UNITS = { "", "K", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q" };

    private NumberUtils() {}

    /**
     * Formats an amount with at most three significant digits and a unit
     * suffix. Amounts below one thousand are printed in full.
     *
     * @param amount amount to format
     * @return the formatted text
     */
    public static String formatCompact(long amount) {
        if (amount == 0L) {
            return "0";
        }
        boolean negative = amount < 0L;
        double value = negative ? -(double) amount : amount;
        int unitIndex = 0;
        while (value >= 1000D && unitIndex < UNITS.length - 1) {
            value /= 1000D;
            unitIndex++;
        }
        String text = unitIndex == 0 ? Long.toString(negative ? -amount : amount) : formatScaled(value, unitIndex);
        return negative ? "-" + text : text;
    }

    public static String formatFluid(long amount) {
        return formatCompact(amount) + "B";
    }

    public static String formatAspect(long amount) {
        return formatCompact(amount) + " essentia";
    }

    public static String formatPercent(double ratio) {
        double clamped = Math.max(0D, Math.min(1D, ratio));
        return String.format(Locale.ROOT, "%.0f%%", clamped * 100D);
    }

    private static String formatScaled(double value, int unitIndex) {
        if (value >= 100D) {
            return String.format(Locale.ROOT, "%.0f%s", value, UNITS[unitIndex]);
        }
        if (value >= 10D) {
            return String.format(Locale.ROOT, "%.1f%s", value, UNITS[unitIndex]);
        }
        return String.format(Locale.ROOT, "%.2f%s", value, UNITS[unitIndex]);
    }
}
