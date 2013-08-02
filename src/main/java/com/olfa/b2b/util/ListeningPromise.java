package com.olfa.b2b.util;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.miriamlaurel.prometheus.AbstractPromise;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ListeningPromise<T> extends AbstractPromise<T> implements MessageListener {

    private final MessageListener listener = new DispatchListener(this);
    private final CountDownLatch latch = new CountDownLatch(1);
    private T result;

    @Override public T get() {
        try {
            latch.await();
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted!", e);
        }
    }

    @Override public T get(long l) throws TimeoutException {
        try {
            latch.await(l, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new TimeoutException();
            } else {
                return result;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted!", e);
        }
    }

    @Override public void processMessage(Object o) {
        listener.processMessage(o);
    }

    @Listener void $(T message) {
        this.result = message;
        markDone();
        latch.countDown();
    }
}
