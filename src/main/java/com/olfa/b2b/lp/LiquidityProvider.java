package com.olfa.b2b.lp;

import com.miriamlaurel.prometheus.MementoPromise;
import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Trade;
import com.olfa.b2b.events.*;

public interface LiquidityProvider {
    Promise<Online<? extends LiquidityProvider>> connect();
    Promise<Offline<? extends LiquidityProvider>> disconnect();
    Status<? extends LiquidityProvider> getStatus();
    Promise<Online<Subscription>> subscribe(Subscription subscription);
    Promise<Offline<Subscription>> unsubscribe(Subscription subscription);
    Status<Subscription> getSubscriptionStatus(Subscription subscription);
    String getName();
    void addStatusListener(StatusListener<? extends LiquidityProvider> listener);
    void addMarketDataListener(MarketDataListener listener);
    MementoPromise<Trade> trade(Order order);
}
