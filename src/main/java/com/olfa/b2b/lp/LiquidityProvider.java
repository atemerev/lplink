package com.olfa.b2b.lp;

import com.miriamlaurel.prometheus.MementoPromise;
import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.domain.Feed;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Trade;
import com.olfa.b2b.events.*;

public interface LiquidityProvider {
    Promise<Online<? extends LiquidityProvider>> connect();
    Promise<Offline<? extends LiquidityProvider>> disconnect();
    Status<? extends LiquidityProvider> getStatus();
    Promise<Online<Feed>> subscribe(Feed feed);
    Promise<Offline<Feed>> unsubscribe(Feed feed);
    Status<Feed> getSubscriptionStatus(Feed feed);
    String getName();
    void addStatusListener(StatusListener<? extends LiquidityProvider> listener);
    void addMarketDataListener(MarketDataListener listener);
    MementoPromise<Trade> trade(Order order);
}
