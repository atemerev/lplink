package com.olfa.b2b.lp.impl;

import com.olfa.b2b.domain.*;
import com.olfa.b2b.exception.LifecycleException;
import com.olfa.b2b.lp.AbstractLiquidityProvider;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MockLiquidityProvider extends AbstractLiquidityProvider {

    private Map<CurrencyPair, Quote> currentBids = new ConcurrentHashMap<>();
    private Map<CurrencyPair, Quote> currentAsks = new ConcurrentHashMap<>();
    private Map<CurrencyPair, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Timer timer = new Timer(true);
    private final Random random = new Random();
    private final CountDownLatch latch = new CountDownLatch(1);

    public MockLiquidityProvider() {
        super();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (CurrencyPair currencyPair : subscriptions.keySet()) {
                    Subscription subscription = subscriptions.get(currencyPair);
                    BigDecimal price1 = new BigDecimal(random.nextDouble(), MathContext.DECIMAL32);
                    BigDecimal price2 = new BigDecimal(random.nextDouble(), MathContext.DECIMAL32);
                    BigDecimal bid = price1.compareTo(price2) < 0 ? price1 : price2;
                    @SuppressWarnings("NumberEquality") BigDecimal ask = bid == price1 ? price2 : price1;
                    final Quote bidQuote = new Quote(Quote.Action.PUT, UUID.randomUUID().toString(),
                            subscription, Quote.Side.BID, subscription.requestId, "b1",
                            new BigDecimal("1000000"), bid);
                    final Quote askQuote = new Quote(Quote.Action.PUT, UUID.randomUUID().toString(),
                            subscription, Quote.Side.ASK, subscription.requestId, "a1",
                            new BigDecimal("1000000"), ask);
                    currentBids.put(subscription.instrument, bidQuote);
                    currentAsks.put(subscription.instrument, askQuote);
                    fireQuote(bidQuote);
                    fireQuote(askQuote);
                }
                if (latch.getCount() != 0) {
                    latch.countDown();
                }
            }
        }, 1000, 1000);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new LifecycleException("Interrupted");
        }
    }

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public void subscribe(Subscription subscription) {
        assert getName().equals(subscription.source);
        subscriptions.put(subscription.instrument, subscription);
        currentBids.put(subscription.instrument, null);
        currentAsks.put(subscription.instrument, null);
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        assert getName().equals(subscription.source);
        subscriptions.remove(subscription.instrument);
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
            ExecutionReport report = new ExecutionReport(
                    getName(),
                    order,
                    ExecutionReport.ExecutionStatus.FILLED,
                    trade,
                    "Mock execution OK"
            );
            fireExecutionReport(report);
        }
    }

    private Quote getQuote(Order order) {
        return order.side == Side.BUY ? currentAsks.get(order.instrument) : currentBids.get(order.instrument);
    }

    @Override
    protected void finalize() throws Throwable {
        timer.cancel();
        super.finalize();
    }
}
