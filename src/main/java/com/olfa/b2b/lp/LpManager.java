package com.olfa.b2b.lp;

import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.events.LpStatusListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.events.StatusEvent;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.ValidationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LpManager implements MarketDataListener {

    private final long timeout;
    private final Map<Subscription, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    private final Timer timer = new Timer(true);
    private final Set<Subscription> subscriptions;
    private final Map<String, LiquidityProvider> liquidityProviders;

    private final Queue<LpStatusListener> statusListeners = new ConcurrentLinkedQueue<>();

    public LpManager(Set<Subscription> subscriptions, long tick, long timeout) {
        this.subscriptions = new HashSet<>(subscriptions);
        this.liquidityProviders = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            // todo check if LP configuration exists?
            liquidityProviders.put(subscription.source, null);
        }
        this.timeout = timeout;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTick(new Tick(System.currentTimeMillis()));
            }
        }, 0, tick);
    }

    public Set<String> getLpNames() {
        return liquidityProviders.keySet();
    }

    @Nullable
    public LiquidityProvider getLp(String name) {
        return liquidityProviders.get(name);
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
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

    public void subscribe(Subscription subscription) throws ValidationException {
        if (!isOnline(subscription)) {
            LiquidityProvider lp = getOrCreateLiquidityProvider(subscription.source);
            lp.subscribe(subscription);
        }
    }

    public void unsubscribe(Subscription subscription) throws ValidationException {
        if (isOnline(subscription)) {
            LiquidityProvider lp = getOrCreateLiquidityProvider(subscription.source);
            lp.unsubscribe(subscription);
        }
    }

    public void start() {
        for (Subscription subscription : subscriptions) {
            subscribe(subscription);
        }
    }

    public void stop() {
        timer.cancel();
        for (Subscription subscription : subscriptions) {
            unsubscribe(subscription);
        }
    }

    public void addStatusListener(LpStatusListener listener) {
        statusListeners.add(listener);
    }

    @Override protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    private LiquidityProvider getOrCreateLiquidityProvider(String lpName) {
        LiquidityProvider lp = liquidityProviders.get(lpName);
        if (lp == null)  {
            lp = startLiquidityProvider(lpName);
            lp.addStatusListener(new LpMonitorListener());
            liquidityProviders.put(lpName, lp);
        }
        return lp;
    }

    @SuppressWarnings("unchecked")
    private LiquidityProvider startLiquidityProvider(String lpName) {
        try {
            Config lpConfig = getLpConfig(lpName);
            Class<? extends LiquidityProvider> lpClass = (Class<? extends LiquidityProvider>) Class.forName(lpConfig.getString("implementation"));
            return lpClass.getConstructor(Config.class).newInstance(lpConfig);
        } catch (ClassNotFoundException |
                 NoSuchMethodException |
                 InvocationTargetException |
                 IllegalAccessException |
                 InstantiationException e) {
            throw new ConfigurationException(e);
        }
    }

    private Config getLpConfig(String lpName) {
        return ConfigFactory.load("/lp/" + lpName + "/lp.conf");
    }

    private void touch(Subscription subscription) {
        lastUpdateTimes.put(subscription, System.currentTimeMillis());
    }

    class LpMonitorListener implements LpStatusListener {
        @Override
        public void onStatusEvent(StatusEvent e) {
            if (e.type == StatusEvent.Type.DISCONNECTED) {
                liquidityProviders.remove(e.source);
                // todo check if we need actually restarting it...
                getOrCreateLiquidityProvider(e.source);
            }
            for (LpStatusListener listener : statusListeners) {
                listener.onStatusEvent(e);
            }
        }
    }
}
