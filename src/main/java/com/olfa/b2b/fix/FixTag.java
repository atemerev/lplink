package com.olfa.b2b.fix;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class FixTag implements FixElement {
    private final int number;
    private @NotNull final String value;

    public FixTag(int number, @NotNull String value) {
        this.number = number;
        this.value = value;
    }

    public FixTag(int number, int value) {
        this.number = number;
        this.value = String.valueOf(value);
    }

    public FixTag(int number, char value) {
        this.number = number;
        this.value = String.valueOf(value);
    }

    public int getNumber() {
        return number;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    @Override
    public FixTag asTag() {
        return this;
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

    @Override
    public boolean isGroup() {
        return false;
    }
}
