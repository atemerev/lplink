package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class Subscription {

    public @NotNull final String source;
    public @NotNull final CurrencyPair instrument;
    public @Nullable final BigDecimal amount;
    public @Nullable final String requestId;
    public @Nullable final String classifier;

    private final String toString;
    private volatile long ts;

    public Subscription(@NotNull String source, @NotNull CurrencyPair instrument, @Nullable BigDecimal amount, @Nullable String classifier) {
        this.source = source;
        this.instrument = instrument;
        this.amount = amount;
        this.classifier = classifier;
        this.toString = mkString();
        this.requestId = toString() + "req" + System.currentTimeMillis();
    }

    public Subscription(@NotNull String source, @NotNull CurrencyPair instrument) {
        this(source, instrument, null, null);
    }

    @Override public String toString() {
        return toString;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription subscription = (Subscription) o;

        if (amount != null ? !amount.equals(subscription.amount) : subscription.amount != null) return false;
        if (classifier != null ? !classifier.equals(subscription.classifier) : subscription.classifier != null) return false;
        if (!instrument.equals(subscription.instrument)) return false;
        if (!source.equals(subscription.source)) return false;

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
