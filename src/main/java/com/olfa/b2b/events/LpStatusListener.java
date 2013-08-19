package com.olfa.b2b.events;

public interface LpStatusListener<T> {
    void onStatusChanged(Status<T> oldStatus, Status<T> newStatus);
}
