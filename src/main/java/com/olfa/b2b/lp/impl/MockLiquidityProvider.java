package com.olfa.b2b.lp.impl;

import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.events.ExecutionReportListener;
import com.olfa.b2b.events.LpStatusListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.lp.LiquidityProvider;

public class MockLiquidityProvider implements LiquidityProvider {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public void subscribe(Subscription subscription) {
    }

    @Override
    public void unsubscribe(Subscription subscription) {
    }

    @Override
    public void trade(Order order) {
    }

    @Override
    public void addStatusListener(LpStatusListener listener) {
    }

    @Override
    public void addMarketDataListener(MarketDataListener listener) {
    }

    @Override
    public void addTradeListener(ExecutionReportListener listener) {
    }
}
