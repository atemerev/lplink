package com.olfa.b2b.lp.impl;

import com.olfa.b2b.domain.*;
import com.olfa.b2b.lp.AbstractLiquidityProvider;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MockLiquidityProvider extends AbstractLiquidityProvider {

    private Map<CurrencyPair, Quote> currentBids = new ConcurrentHashMap<>();
    private Map<CurrencyPair, Quote> currentAsks = new ConcurrentHashMap<>();
    private final Timer timer = new Timer();

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public void subscribe(Subscription subscription) {
        assert getName().equals(subscription.source);
        currentBids.put(subscription.instrument, new Quote(Quote.Action.PUT, UUID.randomUUID().toString(),
                subscription, Quote.Side.BID, subscription.requestId, null,
                new BigDecimal("1000000"), new BigDecimal("1.3342")));
        currentAsks.put(subscription.instrument, new Quote(Quote.Action.PUT, UUID.randomUUID().toString(),
                subscription, Quote.Side.BID, subscription.requestId, null,
                new BigDecimal("1000000"), new BigDecimal("1.3343")));
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        assert getName().equals(subscription.source);
        currentBids.remove(subscription.instrument);
        currentAsks.remove(subscription.instrument);
    }

    @Override
    public void trade(Order order) {
        Quote quote = getQuote(order);
        if (quote != null && quote.price != null) {
            Trade trade = new Trade(UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    order.instrument,
                    order.side,
                    order.amount,
                    quote.price
            );
        }
    }

    private Quote getQuote(Order order) {
        return order.side == Side.BUY ? currentAsks.get(order.instrument) : currentBids.get(order.instrument);
    }
}
