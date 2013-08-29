package com.olfa.b2b.test;

import com.olfa.b2b.domain.CurrencyPair;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.lp.impl.MockLiquidityProvider;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

import java.util.concurrent.CountDownLatch;

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
            latch.await();
        } catch (InterruptedException e) {
            fail();
        }
    }
}
