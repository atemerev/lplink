package com.olfa.b2b.events;

import org.jetbrains.annotations.NotNull;

public interface Status<T> {
    @NotNull T getSubject();
    boolean isOnline();
    long lastUpdate(); // timestamp
}
