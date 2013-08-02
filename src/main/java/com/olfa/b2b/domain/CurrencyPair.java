package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Currency;

public class CurrencyPair {

    public final @NotNull Currency primary;
    public final @NotNull Currency secondary;

    public CurrencyPair(@NotNull Currency primary, @NotNull Currency secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    public CurrencyPair(String primary, String secondary) {
        this(Currency.getInstance(primary), Currency.getInstance(secondary));
    }

    public CurrencyPair(String symbol) {
        String[] tokens = symbol.split("/");
        assert tokens.length == 2;
        this.primary = Currency.getInstance(tokens[0]);
        this.secondary = Currency.getInstance(tokens[1]);
    }

    @Override
    public String toString() {
        return primary.toString() + "/" + secondary.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyPair that = (CurrencyPair) o;
        return this.primary.equals(that.primary) && this.secondary.equals(that.secondary);
    }

    @Override
    public int hashCode() {
        int result = primary.hashCode();
        result = 31 * result + secondary.hashCode();
        return result;
    }
}
