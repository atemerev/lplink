package com.olfa.b2b.lp;

import com.olfa.b2b.domain.ExecutionReport;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.ExecutionReportListener;
import com.olfa.b2b.events.LpStatusListener;
import com.olfa.b2b.events.MarketDataListener;
import com.olfa.b2b.events.StatusEvent;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.LifecycleException;
import com.olfa.b2b.exception.RejectedException;
import com.olfa.b2b.lp.quickfix.FixLpConfiguration;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import quickfix.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

public abstract class FixLiquidityProvider extends MessageCracker implements LiquidityProvider, Application {

    public static final String QUOTE_SESSION = "quote";
    public static final String TRADE_SESSION = "trade";

    private final CountDownLatch startupLatch = new CountDownLatch(1);
    private final Initiator initiator;
    protected final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();
    protected final ConcurrentMap<SessionID, Boolean> sessionStatus = new ConcurrentHashMap<>();
    protected final FixLpConfiguration configuration;
    private final Queue<LpStatusListener> statusListeners = new ConcurrentLinkedQueue<>();
    private final Queue<MarketDataListener> marketDataListeners = new ConcurrentLinkedQueue<>();
    private final Queue<ExecutionReportListener> tradeListeners = new ConcurrentLinkedQueue<>();

    protected FixLiquidityProvider(String name, @NotNull Config conf) throws ConfigurationException {
        super();
        try {
            this.configuration = new FixLpConfiguration(name, conf, null);
            for (SessionID sid : configuration.sessionIDs.values()) {
                sessionStatus.put(sid, false);
            }
            this.initiator = createInitiator();
            initiator.start();
            startupLatch.await();
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        } catch (InterruptedException e) {
            throw new LifecycleException("Interrupted");
        }
    }

    void disconnect() {
        initiator.stop();
        fireStatusEvent(new StatusEvent(getName(),
                String.format("LP %s has been stopped", getName()), StatusEvent.Type.DISCONNECTED));
    }

    protected void sendTo(String sessionName, Message message) throws RejectedException {
        SessionID sid = configuration.sessionIDs.get(sessionName);
        if (sid != null) {
            sendTo(sid, message);
        } else {
            throw new RejectedException(String.format("Session not found: %s", sessionName));
        }
    }

    protected void sendTo(SessionID sid, Message message) throws RejectedException {
        try {
            Session.sendToTarget(message, sid);
        } catch (SessionNotFound e) {
            throw new RejectedException(String.format("Session not found (%s): %s", e.getMessage(), sid));
        }
    }

    @Override
    public String getName() {
        return configuration.name;
    }

    @Override
    public void trade(Order order) {
        orders.put(order.id, order);
        // override this method to perform the actual trade
    }

    // QuickFIX Application interface that can be overridden if necessary

    @Override
    public void onCreate(final SessionID sessionID) {
        Session.lookupSession(sessionID).addStateListener(new SessionStateListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onDisconnect() {
                disconnect();
            }

            @Override
            public void onLogon() {
            }

            @Override
            public void onLogout() {
                disconnect();
            }

            @Override
            public void onReset() {
            }

            @Override
            public void onRefresh() {
            }

            @Override
            public void onMissedHeartBeat() {
            }

            @Override
            public void onHeartBeatTimeout() {
            }
        });
    }

    @Override
    public void onLogon(SessionID sessionID) {
        onSessionOnline(sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    private void onSessionOnline(SessionID sid) {
        sessionStatus.put(sid, true);
        boolean allOnline = true;
        for (SessionID sessionID : sessionStatus.keySet()) {
            if (!sessionStatus.get(sessionID)) {
                allOnline = false;
                break;
            }
        }
        if (allOnline) {
            startupLatch.countDown();
        }
    }

    @Override
    public String toString() {
        return configuration.name.toUpperCase();
    }

    @Override
    public void addStatusListener(LpStatusListener listener) {
        statusListeners.add(listener);
    }

    @Override
    public void addMarketDataListener(MarketDataListener listener) {
        marketDataListeners.add(listener);
    }

    @Override
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

    private SocketInitiator createInitiator() throws ConfigError {
        SessionSettings settings = configuration.sessionSettings;
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        return new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }
}
