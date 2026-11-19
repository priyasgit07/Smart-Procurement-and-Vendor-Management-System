package com.procurement.procurement.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for Indian Rupee (INR - ₹) currency formatting following the Indian numbering system.
 * Example: ₹1,10,000.00 (lakhs, crores grouping instead of thousands grouping).
 */
@Component("currencyUtil")
public class CurrencyUtil {

    public static final String RUPEE_SYMBOL = "₹";

    /**
     * Formats a numeric value into INR currency display (e.g. ₹1,10,000.00).
     *
     * @param amount The numeric amount to format
     * @return Formatted Indian Rupee currency string
     */
    public String formatINR(Number amount) {
        return format(amount);
    }

    /**
     * Static helper to format a numeric value into INR currency display.
     *
     * @param amount The numeric amount to format
     * @return Formatted Indian Rupee currency string
     */
    public static String format(Number amount) {
        if (amount == null) {
            return RUPEE_SYMBOL + "0.00";
        }

        BigDecimal bd;
        if (amount instanceof BigDecimal) {
            bd = (BigDecimal) amount;
        } else {
            bd = BigDecimal.valueOf(amount.doubleValue());
        }
        bd = bd.setScale(2, RoundingMode.HALF_UP);

        boolean isNegative = bd.signum() < 0;
        BigDecimal absBd = bd.abs();

        String plainString = absBd.toPlainString();
        int dotIndex = plainString.indexOf('.');
        String integerPart = dotIndex >= 0 ? plainString.substring(0, dotIndex) : plainString;
        String fractionPart = dotIndex >= 0 ? plainString.substring(dotIndex + 1) : "00";

        String formattedInteger = formatIndianGrouping(integerPart);

        StringBuilder sb = new StringBuilder();
        if (isNegative) {
            sb.append("-");
        }
        sb.append(RUPEE_SYMBOL);
        sb.append(formattedInteger);
        sb.append(".").append(fractionPart);

        return sb.toString();
    }

    /**
     * Formats the integer part using the Indian numbering system:
     * - Last 3 digits grouped together
     * - Preceding digits grouped in pairs of 2
     */
    private static String formatIndianGrouping(String integerPart) {
        if (integerPart == null || integerPart.length() <= 3) {
            return (integerPart == null || integerPart.isEmpty()) ? "0" : integerPart;
        }

        int len = integerPart.length();
        String last3 = integerPart.substring(len - 3);
        String remaining = integerPart.substring(0, len - 3);

        StringBuilder sb = new StringBuilder();
        int remLen = remaining.length();
        int firstGroupLen = remLen % 2 == 0 ? 2 : 1;

        sb.append(remaining, 0, firstGroupLen);
        for (int i = firstGroupLen; i < remLen; i += 2) {
            sb.append(",").append(remaining, i, i + 2);
        }
        sb.append(",").append(last3);

        return sb.toString();
    }
}
