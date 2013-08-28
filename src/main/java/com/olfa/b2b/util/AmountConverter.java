package com.olfa.b2b.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmountConverter {

    private static final Pattern REGEX = Pattern.compile("^((\\d+)(\\w?)$)");

    public static long expand(String brief) throws IllegalArgumentException {
        Matcher matcher = REGEX.matcher(brief);
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(2));
            String multiplierString = matcher.group(3);
            if (multiplierString == null || "".equals(multiplierString)) {
                return amount;
            } else if ("k".equalsIgnoreCase(multiplierString)) {
                return amount * 1000;
            } else if ("m".equalsIgnoreCase(multiplierString)) {
                return amount * 1000000;
            } else {
                throw new IllegalArgumentException("Only K and M multipliers are supported");
            }
        } else {
            throw new IllegalArgumentException("Can't match brief: " + brief);
        }
    }

    public static String contract(long amount) throws IllegalArgumentException {
        final long d1m = amount / 1000000;
        final long d1k = amount / 1000;
        if (d1m * 1000000 == amount) {
            return d1m + "m";
        } else if (d1k * 1000 == amount) {
            return d1k + "k";
        } else {
            return "" + amount;
        }
    }
}
