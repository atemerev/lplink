package com.olfa.b2b.exception;

public class RejectedException extends RuntimeException {
    public RejectedException() {
    }

    public RejectedException(String message) {
        super(message);
    }
}
