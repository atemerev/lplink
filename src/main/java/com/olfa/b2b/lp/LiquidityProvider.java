package com.olfa.b2b.lp;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.events.*;

public interface LiquidityProvider {
    String getName();
    void subscribe(Subscription subscription);
    void unsubscribe(Subscription subscription);
    void trade(Order order);
    void addStatusListener(StatusListener<? extends LiquidityProvider> listener);
    void addMarketDataListener(MarketDataListener listener);
    void addTradeListener(ExecutionReportListener listener);
}
