package com.olfa.b2b.lp;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.HasMessageListeners;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.MessageListenerDelegate;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Online;
import com.olfa.b2b.events.Status;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionMonitor extends DispatchListener implements HasMessageListeners {

    private final long timeout;

    private final MessageListenerDelegate delegate = new MessageListenerDelegate();
    private final Map<Subscription, Status<Subscription>> states = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(true);

    public final Set<Subscription> subscriptions;

    public SubscriptionMonitor(Set<Subscription> subscriptions, long tick, long timeout) {
        this.subscriptions = subscriptions;
        this.timeout = timeout;
        for (Subscription subscription : subscriptions) {
            states.put(subscription, new Offline<>(subscription, "Not started yet"));
        }
        timer.schedule(new TimerTask() {
            @Override public void run() {
                processMessage(new Tick(System.currentTimeMillis()));
            }
        }, 0, tick);
    }

    @Listener void $(Quote quote) {
        Subscription subscription = quote.subscription;
        Status prev = states.get(subscription);
        assert prev != null;
        if (!prev.isOnline()) {
            final Online<Subscription> online = new Online<>(subscription);
            states.put(subscription, online);
            delegate.processMessage(online);
        } else {
            @SuppressWarnings("unchecked") final Online<Subscription> online = (Online<Subscription>) prev;
            online.touch();
        }
    }

    @Listener void $(Tick tick) {
        for (Subscription subscription : states.keySet()) {
            Status<Subscription> status = states.get(subscription);
            final long current = System.currentTimeMillis();
            final Long timestamp = status.lastUpdate();
            if (status.isOnline() && current > timestamp + timeout) {
                final Offline<Subscription> offline = new Offline<>(subscription, "Subscription timeout");
                states.put(subscription, offline);
                delegate.processMessage(offline);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Listener void $(Offline offline) {
        if (offline.getSubject() instanceof Subscription) {
            Subscription subscription = (Subscription) offline.getSubject();
            final Status<Subscription> status = (Status<Subscription>) offline;
            states.put(subscription, status);
            delegate.processMessage(status);
        }
    }

    public Status<Subscription> getStatus(Subscription subscription) {
        return states.get(subscription);
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
}
