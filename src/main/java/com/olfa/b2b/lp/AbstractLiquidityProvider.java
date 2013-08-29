package com.olfa.b2b.lp;

import com.olfa.b2b.domain.ExecutionReport;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.ExecutionReportListener;
import com.olfa.b2b.events.LpStatusListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.events.StatusEvent;
import quickfix.MessageCracker;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractLiquidityProvider implements LiquidityProvider {

    private final Queue<LpStatusListener> statusListeners = new ConcurrentLinkedQueue<>();
    private final Queue<MarketDataListener> marketDataListeners = new ConcurrentLinkedQueue<>();
    private final Queue<ExecutionReportListener> tradeListeners = new ConcurrentLinkedQueue<>();

    public void addStatusListener(LpStatusListener listener) {
        statusListeners.add(listener);
    }

    public void addMarketDataListener(MarketDataListener listener) {
        marketDataListeners.add(listener);
    }

    public void addTradeListener(ExecutionReportListener listener) {
        tradeListeners.add(listener);
    }

    protected void fireStatusEvent(StatusEvent event) {
        for (LpStatusListener listener : statusListeners) {
            listener.onStatusEvent(event);
        }
    }

    protected void fireQuote(Quote quote) {
        for (MarketDataListener listener : marketDataListeners) {
            listener.onQuote(quote);
        }
    }

    protected void fireExecutionReport(ExecutionReport report) {
        for (ExecutionReportListener listener : tradeListeners) {
            listener.onExecutionReport(report);
        }
    }
}
