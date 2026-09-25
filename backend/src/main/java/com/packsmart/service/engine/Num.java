package com.packsmart.service.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Number formatting for reasons and API output (display only - never used inside calculations). */
public final class Num {

    private static final int SIGNIFICANT_DIGITS = 4;

    private Num() {
    }

    /** Rounds to 4 significant digits (keeps tiny OTR values like 0.01389 readable). */
    public static double sig(double v) {
        if (v == 0 || Double.isNaN(v) || Double.isInfinite(v)) {
            return v;
        }
        return new BigDecimal(v).round(new MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)).doubleValue();
    }

    public static Double sig(Double v) {
        return v == null ? null : sig(v.doubleValue());
    }

    /** Rounds to a fixed number of decimals. */
    public static double dp(double v, int decimals) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return v;
        }
        return BigDecimal.valueOf(v).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }

    /** Human-friendly string: 4 significant digits, no trailing zeros. */
    public static String fmt(double v) {
        if (Double.isInfinite(v)) {
            return "∞";
        }
        return BigDecimal.valueOf(sig(v)).stripTrailingZeros().toPlainString();
    }
}
