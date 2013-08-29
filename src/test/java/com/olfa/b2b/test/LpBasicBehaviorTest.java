package com.olfa.b2b.test;

import com.olfa.b2b.domain.*;
import com.olfa.b2b.events.ExecutionReportListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.lp.impl.MockLiquidityProvider;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LpBasicBehaviorTest {

    @Test
    public void testSubscription() {
        MockLiquidityProvider mockLp = new MockLiquidityProvider();
        final Subscription subscription = new Subscription(
                "mock",
                new CurrencyPair("EUR/USD")
        );
        mockLp.subscribe(subscription);
        final CountDownLatch latch = new CountDownLatch(2);
        mockLp.addMarketDataListener(new MarketDataListener() {
            @Override
            public void onQuote(Quote quote) {
                assertNotNull(quote.price);
                assertEquals(quote.subscription, subscription);
                latch.countDown();
            }
        });
        try {
            latch.await(1500, TimeUnit.MILLISECONDS);
            assertEquals(0, latch.getCount());
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void testTrade() {
        MockLiquidityProvider mockLp = new MockLiquidityProvider();
        final Subscription subscription = new Subscription(
                "mock",
                new CurrencyPair("EUR/USD")
        );
        mockLp.subscribe(subscription);
        final CountDownLatch quoteLatch = new CountDownLatch(2);
        mockLp.addMarketDataListener(new MarketDataListener() {
            @Override
            public void onQuote(Quote quote) {
                assertNotNull(quote.price);
                assertEquals(quote.subscription, subscription);
                quoteLatch.countDown();
            }
        });
        try {
            quoteLatch.await(1500, TimeUnit.MILLISECONDS);
            assertEquals(0, quoteLatch.getCount());
        } catch (InterruptedException e) {
            fail();
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final Order order = new Order(UUID.randomUUID().toString(),
                mockLp.getName(),
                null,
                new CurrencyPair("EUR/USD"),
                Side.BUY,
                new BigDecimal("1000000"),
                new BigDecimal("1.3314")
        );
        mockLp.addTradeListener(new ExecutionReportListener() {
            @Override
            public void onExecutionReport(ExecutionReport report) {
                if (report.status == ExecutionReport.ExecutionStatus.FILLED) {
                    assertNotNull(report.trade);
                    assertNotNull(report.trade.price);
                    assertEquals(report.trade.amount, order.amount);
                    latch.countDown();
                } else {
                    fail();
                }
            }
        });
        mockLp.trade(order);
        try {
            latch.await(1500 ,TimeUnit.MILLISECONDS);
            assertEquals(0, latch.getCount());
        } catch (InterruptedException e) {
            fail();
        }
    }
}
