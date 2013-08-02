package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class Feed {

    public @NotNull final String source;
    public @NotNull final CurrencyPair instrument;
    public @Nullable final BigDecimal amount;
    public @Nullable final String classifier;

    private final String toString;
    private volatile long ts;

    public Feed(@NotNull String source, @NotNull CurrencyPair instrument, @Nullable BigDecimal amount, @Nullable String classifier) {
        this.source = source;
        this.instrument = instrument;
        this.amount = amount;
        this.classifier = classifier;
        this.ts = System.currentTimeMillis();
        this.toString = mkString();
    }

    public Feed(@NotNull String source, @NotNull CurrencyPair instrument) {
        this(source, instrument, null, null);
    }

    public String generateSubscriptionId() {
        return toString() + "-req-" + (ts++);
    }

    @Override public String toString() {
        return toString;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feed feed = (Feed) o;

        if (amount != null ? !amount.equals(feed.amount) : feed.amount != null) return false;
        if (classifier != null ? !classifier.equals(feed.classifier) : feed.classifier != null) return false;
        if (!instrument.equals(feed.instrument)) return false;
        if (!source.equals(feed.source)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + instrument.hashCode();
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        return result;
    }

    private String mkString() {
        StringBuilder builder = new StringBuilder();
        builder.append(source);
        builder.append("-");
        builder.append(instrument.toString());
        if (amount != null) {
            builder.append("-");
            builder.append(amount.toPlainString());
        }
        if (classifier != null) {
            builder.append("-");
            builder.append(classifier);
        }
        return builder.toString();
    }
}
