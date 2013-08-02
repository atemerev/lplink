package com.olfa.b2b.domain;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class Order {
    public final long timestamp;
    public final @NotNull String id;
    public final @NotNull String destination;
    public final @Nullable Quote quote;
    public final @NotNull CurrencyPair instrument;
    public final @NotNull Side side;
    public final @NotNull BigDecimal amount;
    public final @NotNull BigDecimal price;

    public Order(
            @NotNull String id,
            @NotNull String destination,
            @Nullable Quote quote,
            @NotNull CurrencyPair instrument,
            @NotNull Side side,
            @NotNull BigDecimal amount,
            @Nullable BigDecimal price) {
        assert quote != null || price != null;
        assert price == null || price.compareTo(BigDecimal.ZERO) > 0;
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.destination = destination;
        this.quote = quote;
        this.amount = amount;
        this.instrument = instrument;
        this.side = side;
        //noinspection ConstantConditions
        this.price = price != null ? price : quote.price;
    }
}
