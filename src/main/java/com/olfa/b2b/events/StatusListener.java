package com.olfa.b2b.events;

public interface StatusListener<T> {
    void onStatusChanged(Status<T> oldStatus, Status<T> newStatus);
}
