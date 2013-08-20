package com.olfa.b2b.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmountConverter {

    private static final Pattern REGEX = Pattern.compile("^((\\d+)(\\w?)$)");
    private static final BigDecimal BD_1K = new BigDecimal(1000);
    private static final BigDecimal BD_1M = new BigDecimal(1000000);


    public static BigDecimal expand(String brief) throws IllegalArgumentException {
        Matcher matcher = REGEX.matcher(brief);
        if (matcher.matches()) {
            BigDecimal amount = new BigDecimal(matcher.group(1));
            String multiplierString = matcher.group(2);
            if (multiplierString == null) {
                return amount;
            } else if ("k".equalsIgnoreCase(multiplierString)) {
                return amount.multiply(new BigDecimal(1000));
            } else if ("m".equalsIgnoreCase(multiplierString)) {
                return amount.multiply(new BigDecimal(1000000));
            } else {
                throw new IllegalArgumentException("Only K and M multiplers are supported");
            }
        } else {
            throw new IllegalArgumentException("Can't match brief: " + brief);
        }
    }

    public static String contract(BigDecimal amount) throws IllegalArgumentException {
        final BigDecimal d1m = amount.divideToIntegralValue(BD_1M);
        final BigDecimal d1k = amount.divideToIntegralValue(BD_1K);
        if (d1m.multiply(BD_1M).equals(amount)) {
            return d1m.toPlainString() + "m";
        } else if (d1k.multiply(BD_1K).equals(amount)) {
            return d1k.toPlainString() + "k";
        } else {
            return amount.toPlainString();
        }
    }
}
