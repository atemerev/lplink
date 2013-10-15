package com.olfa.b2b.fix;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class FixTag {
    public final int number;
    public @NotNull final String value;

    FixTag(int number, @NotNull String value) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixTag fixTag = (FixTag) o;

        if (number != fixTag.number) return false;
        if (!value.equals(fixTag.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return number + "=" + value;
    }
}
