package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class Trade {
    @NotNull public final String id;
    public final long timestamp;
    @NotNull public final CurrencyPair instrument;
    @NotNull public final Side side;
    @NotNull public final BigDecimal amount;
    @NotNull public final BigDecimal price;

    public Trade(
            @NotNull String id, long timestamp, @NotNull CurrencyPair instrument,
            @NotNull Side side, @NotNull BigDecimal amount, @NotNull BigDecimal price) {
        this.id = id;
        this.timestamp = timestamp;
        this.instrument = instrument;
        this.side = side;
        this.amount = amount;
        this.price = price;
    }
}
