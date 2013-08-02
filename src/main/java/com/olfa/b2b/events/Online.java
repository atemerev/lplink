package com.olfa.b2b.events;

import org.jetbrains.annotations.NotNull;

public class Online<T> implements Status<T> {

    private final @NotNull T subject;
    private volatile long lastUpdate;

    public Online(@NotNull T subject) {
        this.subject = subject;
        touch();
    }

    public void touch() {
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override public String toString() {
        return String.format("%s is online, idle for %d seconds", subject.toString(), (System.currentTimeMillis() - lastUpdate) / 1000);
    }

    @Override public @NotNull T getSubject() {
        return subject;
    }

    @Override public boolean isOnline() {
        return true;
    }

    @Override public long lastUpdate() {
        return lastUpdate;
    }
}
