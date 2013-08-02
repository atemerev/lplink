package com.olfa.b2b.events;

public class Diagnostic {

    public final String source;
    public final String message;

    public Diagnostic(String source, String message) {
        this.source = source;
        this.message = message;
    }

    @Override public String toString() {
        return String.format("%s: %s", source, message);
    }
}
