package com.olfa.b2b.util;

import com.olfa.b2b.domain.CurrencyPair;

public class Scaler {
    public static int pipScale(CurrencyPair instrument) {
        return instrument.secondary.getDefaultFractionDigits() + 2;
    }
}
