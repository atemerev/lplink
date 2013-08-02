package com.olfa.b2b.lp;

import com.miriamlaurel.pms.listeners.HasMessageListeners;
import com.miriamlaurel.prometheus.MementoPromise;
import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.domain.Feed;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Trade;
import com.olfa.b2b.events.Status;

import java.util.Set;

public interface LiquidityProvider extends HasMessageListeners {
    Promise<Status<? extends LiquidityProvider>> connect();
    Promise<Status<? extends LiquidityProvider>> disconnect();
    Status<? extends LiquidityProvider> getStatus();
    String getName();
    Set<Feed> getAvailableFeeds();
    MementoPromise<Trade> trade(Order order);
}
