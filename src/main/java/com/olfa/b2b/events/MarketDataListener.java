package com.olfa.b2b.events;

import com.olfa.b2b.domain.Quote;

public interface MarketDataListener {
    void onQuote(Quote quote);
}
