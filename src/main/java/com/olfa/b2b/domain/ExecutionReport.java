package com.olfa.b2b.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecutionReport {

    public enum ExecutionStatus {
        NEW, PARTIAL, FILLED, REJECTED
    }

    private static volatile long nextId = System.currentTimeMillis();

    public @NotNull final String id;
    public @NotNull final String lpId;
    public @NotNull final Order order;
    public @NotNull final ExecutionStatus status;
    public @Nullable final Trade trade;
    public @Nullable final String text;

    public ExecutionReport(@NotNull String lpId, @NotNull Order order, @NotNull ExecutionStatus status, @Nullable Trade trade, @Nullable String text) {
        this.id = "exex-" + (nextId++);
        this.lpId = lpId;
        this.order = order;
        this.status = status;
        this.trade = trade;
        this.text = text;
    }
}
