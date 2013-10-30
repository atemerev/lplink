package com.olfa.b2b.test;

import com.olfa.b2b.domain.CurrencyPair;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.domain.Subscription;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

import java.util.Random;

public class DecimalPerformanceTest {

    private static final Subscription subscription = new Subscription("mock", new CurrencyPair("EUR/USD"));
    private static final Random random = new Random();

    @Test
    public void testQuoteCreationPerformance() {
        int count = 0;
        long start = System.currentTimeMillis();
        while (true) {
            new Quote(Quote.Action.PUT, "test", subscription, Quote.Side.BID, null, null, random.nextDouble());
            count++;
            if (count % 1000 == 0 && System.currentTimeMillis() - start >= 1000) {
                break;
            }
        }
        System.out.println(count);
        assertTrue("More than 1M quotes per second should be created", count > 1000000);
    }
}
