package com.olfa.b2b.lp;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.MessageListenerDelegate;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.olfa.b2b.domain.*;
import com.olfa.b2b.events.*;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.RejectedException;
import com.olfa.b2b.lp.quickfix.FixLpConfiguration;
import com.olfa.b2b.util.ListeningPromise;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quickfix.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class FixLiquidityProvider extends MessageCracker implements LiquidityProvider, Application, MessageListener {

    public static final String QUOTE_SESSION = "quote";
    public static final String TRADE_SESSION = "trade";

    private final MessageListener listener = new DispatchListener(this);
    private final MessageListenerDelegate notifier = new MessageListenerDelegate();
    private final ConcurrentMap<SessionID, Boolean> sessionStatus = new ConcurrentHashMap<>();
    private ListeningPromise<Online<? extends LiquidityProvider>> connectionPromise;
    private ListeningPromise<Offline<? extends LiquidityProvider>> disconnectionPromise;
    protected ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();

    private Initiator initiator;

    protected final FixLpConfiguration configuration;

    protected FixLiquidityProvider(String name, @NotNull Config conf, @Nullable MessageListener listener) throws ConfigurationException {
        super();
        if (listener != null) {
            notifier.listeners().add(listener);
        }
        try {
            this.configuration = new FixLpConfiguration(name, conf, null);
            for (SessionID sid : configuration.sessionIDs.values()) {
                sessionStatus.put(sid, false);
            }
            this.initiator = createInitiator();
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    protected FixLiquidityProvider(String name, @NotNull Config conf) {
        this(name, conf, null);
    }

    // These methods to be called by FIX message crackers

    @SuppressWarnings("unchecked")
    protected void onQuote(Quote quote) {
        // todo change to MarketDataListener
        notifier.processMessage(quote);
    }

    protected void onExecutionResponse(ExecutionReport report) {
        notifier.processMessage(report);
    }

    // Rest of implementation...

    void connect() {
        try {
            if (this.initiator == null) {
                this.initiator = createInitiator();
            }
            this.connectionPromise = new ListeningPromise<>();
            initiator.start();
        } catch (ConfigError e) {
            throw new ConfigurationException(e);
        }
    }

    void disconnect() {
        this.disconnectionPromise = new ListeningPromise<>();
        // todo watch subscriptions, and unsubscribe if necessary
        initiator.stop();
        initiator = null;
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
        notifier.processMessage(new Diagnostic(getName(), "Session created: " + sessionID));
        Session.lookupSession(sessionID).addStateListener(new SessionStateListener() {
            @Override
            public void onConnect() {
                notifier.processMessage(new Diagnostic(getName(), "Session connected: " + sessionID));
            }

            @Override
            public void onDisconnect() {
                notifier.processMessage(new Diagnostic(getName(), "Session disconnected: " + sessionID));
            }

            @Override
            public void onLogon() {
                notifier.processMessage(new Diagnostic(getName(), "Session logon: " + sessionID));
            }

            @Override
            public void onLogout() {
                notifier.processMessage(new Diagnostic(getName(), "Session logout: " + sessionID));
            }

            @Override
            public void onReset() {
                notifier.processMessage(new Diagnostic(getName(), "Session reset: " + sessionID));
            }

            @Override
            public void onRefresh() {
                notifier.processMessage(new Diagnostic(getName(), "Session refreshed: " + sessionID));
            }

            @Override
            public void onMissedHeartBeat() {
                notifier.processMessage(new Diagnostic(getName(), "Missed heartbeat: " + sessionID));
            }

            @Override
            public void onHeartBeatTimeout() {
                notifier.processMessage(new Diagnostic(getName(), "Heartbeat timeout: " + sessionID));
            }
        });
    }

    @Override
    public void onLogon(SessionID sessionID) {
        if (!configuration.isSessionManaged(sessionID)) {
            processMessage(new Online<>(sessionID));
        }
    }

    @Override
    public void onLogout(SessionID sessionID) {
        processMessage(new Offline<>(sessionID, "Session disconnected"));
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

    @Override
    public void processMessage(Object o) {
        listener.processMessage(o);
    }

    @Listener
    public void $(Online<SessionID> online) {
        sessionStatus.put(online.getSubject(), true);
        boolean allOnline = true;
        for (SessionID sessionID : sessionStatus.keySet()) {
            if (!sessionStatus.get(sessionID)) {
                allOnline = false;
                break;
            }
        }
        if (allOnline) {
            final Online<FixLiquidityProvider> status = new Online<>(this);
            notifier.processMessage(status);
            connectionPromise.processMessage(status);
        }
    }

    @Listener
    public void $(Offline<SessionID> offline) {
        sessionStatus.put(offline.getSubject(), true);
        final Offline<FixLiquidityProvider> status = new Offline<>(this, offline.getReason());
        notifier.processMessage(status);
        disconnectionPromise.processMessage(status);
    }

    @Listener
    public void $(Reject<Subscription> reject) {
        notifier.processMessage(new Offline<>(reject.request, reject.reason));
    }

    @Override
    public String toString() {
        return configuration.name.toUpperCase();
    }

    @Override
    public void addStatusListener(StatusListener<? extends LiquidityProvider> listener) {
        // todo implement
    }

    @Override
    public void addMarketDataListener(MarketDataListener listener) {
        // todo implement
    }

    @Override
    public void addTradeListener(ExecutionReportListener listener) {
        // todo implement
    }

    private SocketInitiator createInitiator() throws ConfigError {
        SessionSettings settings = configuration.sessionSettings;
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        return new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }
}
