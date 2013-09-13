package com.olfa.b2b.fix;

import java.math.BigDecimal;

public class Tag {
    public final int number;
    public final String value;

    Tag(int number, String value) {
        this.number = number;
        this.value = value;
    }

    public int getInt() {
        return Integer.parseInt(value);
    }

    public double getDouble() {
        return Double.parseDouble(value);
    }

    public BigDecimal getDecimal() {
        return new BigDecimal(value);
    }
}
