package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class Quote {

    public enum Action {
        PUT, REMOVE
    }

    public enum Side {
        BID, ASK
    }

    public @NotNull final String quoteId;

    public @NotNull final Action action;
    public @NotNull final Feed feed;
    public @NotNull final Side side;
    public @NotNull final String lpQuoteId;
    public @Nullable final String bandId;
    public @Nullable final BigDecimal amount;
    public @Nullable final BigDecimal price;
    public @Nullable final String quoteReqId;

    private static volatile long nextId = System.currentTimeMillis();

    public Quote(
            @NotNull Action action,
            @NotNull String lpQuoteId,
            @NotNull Feed feed,
            @NotNull Side side,
            @Nullable String quoteReqId,
            @Nullable String bandId,
            @Nullable BigDecimal amount,
            @Nullable BigDecimal price) {
        assert bandId != null ^ amount != null;
        this.feed = feed;
        this.lpQuoteId = lpQuoteId;
        this.quoteReqId = quoteReqId;
        this.action = action;
        this.side = side;
        this.bandId = bandId;
        this.amount = amount;
        this.price = price;
        this.quoteId = feed.toString() + "-q-" + (nextId++);
    }

    public Quote(@NotNull Action action, @NotNull String lpQuoteId, @NotNull Feed feed, @NotNull Side side, @Nullable String quoteReqId, @Nullable String bandId, double price) {
        this(action, lpQuoteId, feed, side, quoteReqId, bandId, feed.amount, BigDecimal.valueOf(price));
    }

    public Quote(@NotNull Action action, @NotNull String lpQuoteId, @NotNull Feed feed, @NotNull Side side, @Nullable String quoteReqId) {
        this(action, lpQuoteId, feed, side, quoteReqId, null, null, null);
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(action).append(" ");
        builder.append(side).append(" ");
        builder.append(feed).append(" ");
        if (amount != null && feed.amount != null && !amount.equals(feed.amount)) {
            builder.append(amount).append(" ");
        }
        if (bandId != null) {
            builder.append(bandId).append(" ");
        }
        if (price != null) {
            builder.append(price.toPlainString());
        }
        return builder.toString();
    }
}
