package com.olfa.b2b.events;

public class StatusEvent {

    public final String source;
    public final String message;
    public final Type type;

    public enum Type {
        DISCONNECTED,
        TRADING_SESSION_OFFLINE,
        TRADING_SESSION_ONLINE,
        SUBSCRIPTION_CANCELLED,
        SUBSCRIPTION_TIMEOUT,
        SUBSCRIPTION_ONLINE
    }

    public StatusEvent(String source, String message, Type type) {
        this.source = source;
        this.message = message;
        this.type = type;
    }

    @Override public String toString() {
        return String.format("%s: %s", source, message);
    }
}
