package com.olfa.b2b.lp;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.HasMessageListeners;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.MessageListenerDelegate;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.StatusEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionMonitor extends DispatchListener implements HasMessageListeners {

    private final long timeout;

    private final MessageListenerDelegate delegate = new MessageListenerDelegate();
    private final Map<Subscription, Long> lastUpdateTimes = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(true);

    public final Set<Subscription> subscriptions;

    public SubscriptionMonitor(Set<Subscription> subscriptions, long tick, long timeout) {
        this.subscriptions = subscriptions;
        this.timeout = timeout;
        timer.schedule(new TimerTask() {
            @Override public void run() {
                processMessage(new Tick(System.currentTimeMillis()));
            }
        }, 0, tick);
    }

    @Listener void $(Quote quote) {
        Subscription subscription = quote.subscription;
        if (!isOnline(subscription)) {
            delegate.processMessage(new StatusEvent(
                    quote.subscription.source,
                    "Quotes appeared on " + quote.subscription,
                    StatusEvent.Type.SUBSCRIPTION_ONLINE));
        }
        touch(subscription);
    }

    @Listener void $(Tick tick) {
        for (Subscription subscription : lastUpdateTimes.keySet()) {
            if (!isOnline(subscription)) {
                delegate.processMessage(new StatusEvent(
                        subscription.source,
                        "Feed timeout on " + subscription,
                        StatusEvent.Type.SUBSCRIPTION_TIMEOUT));
            }
        }
    }

    public boolean isOnline(Subscription subscription) {
        final Long lastUpdate = lastUpdateTimes.get(subscription);
        final long now = System.currentTimeMillis();
        return lastUpdate != null && now <= lastUpdate + timeout;
    }

    @Override public List<MessageListener> listeners() {
        return delegate.listeners();
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

    @Override protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    private void touch(Subscription subscription) {
        lastUpdateTimes.put(subscription, System.currentTimeMillis());
    }
}
