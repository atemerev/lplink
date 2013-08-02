package com.olfa.b2b.domain;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.miriamlaurel.prometheus.MementoAbstractPromise;
import com.olfa.b2b.exception.RejectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TradePromise extends MementoAbstractPromise<Trade> implements MessageListener {

    public final Order order;
    public final CountDownLatch latch;
    public final List<ExecutionReport> reports;
    public Trade trade;

    private final MessageListener listener = new DispatchListener(this);

    public TradePromise(Order order) {
        this.order = order;
        this.latch = new CountDownLatch(1);
        this.reports = new ArrayList<>();
    }

    @Listener void $(ExecutionReport report) {
        reports.add(report);
        if (report.status == ExecutionReport.ExecutionStatus.FILLED) {
            this.trade = report.trade;
            markDone();
        } else if (report.status == ExecutionReport.ExecutionStatus.REJECTED) {
            markCancelled();
        }
    }

    @Override public Trade get() throws RejectedException {
        try {
            latch.await();
            if (trade == null) {

            }
            return trade;
        } catch (InterruptedException e) {
            throw new IllegalThreadStateException();
        }
    }

    @Override public Trade get(long timeout) throws TimeoutException {
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
            return trade;
        } catch (InterruptedException e) {
            throw new IllegalThreadStateException();
        }
    }

    @Override public void processMessage(Object o) {
        listener.processMessage(o);
    }

    @Override public List<ExecutionReport> getMemories() {
        return reports;
    }
}
