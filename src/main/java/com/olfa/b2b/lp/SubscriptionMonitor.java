package com.olfa.b2b.lp;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.HasMessageListeners;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.MessageListenerDelegate;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.olfa.b2b.domain.Feed;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Online;
import com.olfa.b2b.events.Status;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionMonitor extends DispatchListener implements HasMessageListeners {

    private final long timeout;

    private final MessageListenerDelegate delegate = new MessageListenerDelegate();
    private final Map<Feed, Status<Feed>> states = new ConcurrentHashMap<>();

    private final Timer timer = new Timer(true);

    public final Set<Feed> feeds;

    public SubscriptionMonitor(Set<Feed> feeds, long tick, long timeout) {
        this.feeds = feeds;
        this.timeout = timeout;
        for (Feed feed : feeds) {
            states.put(feed, new Offline<>(feed, "Not started yet"));
        }
        timer.schedule(new TimerTask() {
            @Override public void run() {
                processMessage(new Tick(System.currentTimeMillis()));
            }
        }, 0, tick);
    }

    @Listener void $(Quote quote) {
        Feed feed = quote.feed;
        Status prev = states.get(feed);
        assert prev != null;
        if (!prev.isOnline()) {
            final Online<Feed> online = new Online<>(feed);
            states.put(feed, online);
            delegate.processMessage(online);
        } else {
            @SuppressWarnings("unchecked") final Online<Feed> online = (Online<Feed>) prev;
            online.touch();
        }
    }

    @Listener void $(Tick tick) {
        for (Feed feed : states.keySet()) {
            Status<Feed> status = states.get(feed);
            final long current = System.currentTimeMillis();
            final Long timestamp = status.lastUpdate();
            if (status.isOnline() && current > timestamp + timeout) {
                final Offline<Feed> offline = new Offline<>(feed, "Feed timeout");
                states.put(feed, offline);
                delegate.processMessage(offline);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Listener void $(Offline offline) {
        if (offline.getSubject() instanceof Feed) {
            Feed feed = (Feed) offline.getSubject();
            final Status<Feed> status = (Status<Feed>) offline;
            states.put(feed, status);
            delegate.processMessage(status);
        }
    }

    public Status<Feed> getStatus(Feed feed) {
        return states.get(feed);
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
