package com.olfa.b2b.lp;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.LpStatusListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.events.StatusEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SubscriptionMonitor implements MarketDataListener {

    private final long timeout;

    private final Map<Subscription, Long> lastUpdateTimes = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(true);

    public final Set<Subscription> subscriptions;
    private final Queue<LpStatusListener> statusListeners = new ConcurrentLinkedQueue<>();

    public SubscriptionMonitor(Set<Subscription> subscriptions, long tick, long timeout) {
        this.subscriptions = subscriptions;
        this.timeout = timeout;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTick(new Tick(System.currentTimeMillis()));
            }
        }, 0, tick);
    }

    public void onQuote(Quote quote) {
        Subscription subscription = quote.subscription;
        if (!isOnline(subscription)) {
            for (LpStatusListener listener : statusListeners) {
                listener.onStatusEvent(new StatusEvent(
                        quote.subscription.source,
                        "Quotes appeared on " + quote.subscription,
                        StatusEvent.Type.SUBSCRIPTION_ONLINE));
            }
        }
        touch(subscription);
    }

    public void onTick(Tick tick) {
        for (Subscription subscription : lastUpdateTimes.keySet()) {
            if (!isOnline(subscription)) {
                for (LpStatusListener listener : statusListeners) {
                    listener.onStatusEvent(new StatusEvent(
                            subscription.source,
                            "Feed timeout on " + subscription,
                            StatusEvent.Type.SUBSCRIPTION_TIMEOUT));
                }
            }
        }
    }

    public boolean isOnline(Subscription subscription) {
        final Long lastUpdate = lastUpdateTimes.get(subscription);
        final long now = System.currentTimeMillis();
        return lastUpdate != null && now <= lastUpdate + timeout;
    }

    class Tick {
        public final long timestamp;

        Tick(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public void stop() {
        timer.cancel();
    }

    public void addStatusListener(LpStatusListener listener) {
        statusListeners.add(listener);
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    private void touch(Subscription subscription) {
        lastUpdateTimes.put(subscription, System.currentTimeMillis());
    }
}
