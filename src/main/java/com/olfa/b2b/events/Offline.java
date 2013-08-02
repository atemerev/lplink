package com.olfa.b2b.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Offline<T> implements Status<T> {

    private final @NotNull T subject;
    private final @Nullable String reason;
    private final long lastUpdate;

    public Offline(@NotNull T subject, @Nullable String reason) {
        this.subject = subject;
        this.reason = reason;
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override public String toString() {
        return String.format("%s is offline (%s) for %d seconds", subject.toString(), reason != null ?
                reason : "reason unknown", ((System.currentTimeMillis() - lastUpdate) / 1000));
    }

    @NotNull
    @Override public T getSubject() {
        return subject;
    }

    @Nullable public String getReason() {
        return reason;
    }

    @Override public boolean isOnline() {
        return false;
    }

    @Override public long lastUpdate() {
        return lastUpdate;
    }
}
