package com.olfa.b2b.lp;

import com.miriamlaurel.pms.Listener;
import com.miriamlaurel.pms.listeners.MessageListener;
import com.miriamlaurel.pms.listeners.MessageListenerDelegate;
import com.miriamlaurel.pms.listeners.dispatch.DispatchListener;
import com.miriamlaurel.prometheus.MementoPromise;
import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.domain.ExecutionReport;
import com.olfa.b2b.domain.Feed;
import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.domain.Reject;
import com.olfa.b2b.domain.Trade;
import com.olfa.b2b.domain.TradePromise;
import com.olfa.b2b.events.Diagnostic;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Online;
import com.olfa.b2b.events.Status;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.RejectedException;
import com.olfa.b2b.lp.quickfix.FixLpConfiguration;
import com.olfa.b2b.util.ListeningPromise;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quickfix.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class FixLiquidityProvider extends MessageCracker implements LiquidityProvider, Application, MessageListener {

    public static final String QUOTE_SESSION = "quote";
    public static final String TRADE_SESSION = "trade";

    private final MessageListener listener = new DispatchListener(this);
    private final MessageListenerDelegate notifier = new MessageListenerDelegate();
    protected final ConcurrentMap<String, TradePromise> tradePromises = new ConcurrentHashMap<>();
    private final ConcurrentMap<SessionID, Boolean> sessionStatus = new ConcurrentHashMap<>();
    private volatile Status<? extends LiquidityProvider> status;
    private ListeningPromise<Status<? extends LiquidityProvider>> connectionPromise;
    private ListeningPromise<Status<? extends LiquidityProvider>> disconnectionPromise;

    private Initiator initiator;

    protected final FixLpConfiguration configuration;

    protected FixLiquidityProvider(String name, @NotNull Config conf, @Nullable MessageListener listener) throws ConfigurationException {
        super();
        this.status = new Offline<>(this, "Not yet started");
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

    // These methods are to be overridden for specific LP implementations

    protected abstract void subscribe();

    protected abstract void unsubscribe();

    protected abstract void sendOrder(Order order);

    // These methods to be called by FIX message crackers

    @SuppressWarnings("unchecked")
    protected void onQuote(Quote quote) {
        if (getStatus().isOnline()) {
            Online<? extends FixLiquidityProvider> online = (Online<? extends FixLiquidityProvider>) getStatus();
            online.touch();
            notifier.processMessage(quote);
        }
    }

    protected void onExecutionResponse(ExecutionReport report) {
        TradePromise promise = tradePromises.get(report.order.id);
        if (promise != null) {
            promise.processMessage(report);
            if (promise.isDone()) {
                tradePromises.remove(report.order.id);
            }
        } else {
            notifier.processMessage(new Diagnostic(getName(), String.format("Execution report for unknown order ID: %s", report.order.id)));
        }
    }

    // Rest of implementation...

    @Override public Promise<Status<? extends LiquidityProvider>> connect() {
        try {
            if (this.initiator == null) {
                this.initiator = createInitiator();
            }
            this.connectionPromise = new ListeningPromise<>();
            initiator.start();
            return connectionPromise;
        } catch (ConfigError e) {
            throw new ConfigurationException(e);
        }
    }

    @Override public Promise<Status<? extends LiquidityProvider>> disconnect() {
        this.disconnectionPromise = new ListeningPromise<>();
        unsubscribe();
        initiator.stop();
        initiator = null;
        return disconnectionPromise;
    }

    @Override public Status<? extends LiquidityProvider> getStatus() {
        return status;
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


    @Override public String getName() {
        return configuration.name;
    }

    @Override public MementoPromise<Trade> trade(Order order) {
        sendOrder(order);
        TradePromise promise = new TradePromise(order);
        tradePromises.put(order.id, promise);
        return promise;
    }

    @Override public Set<Feed> getAvailableFeeds() {
        return configuration.feeds;
    }

    @Override public List<MessageListener> listeners() {
        return notifier.listeners();
    }

    // QuickFIX Application interface that can be overridden if necessary

    @Override public void onCreate(final SessionID sessionID) {
        notifier.processMessage(new Diagnostic(getName(), "Session created: " + sessionID));
        Session.lookupSession(sessionID).addStateListener(new SessionStateListener() {
            @Override public void onConnect() {
                notifier.processMessage(new Diagnostic(getName(), "Session connected: " + sessionID));
            }

            @Override public void onDisconnect() {
                notifier.processMessage(new Diagnostic(getName(), "Session disconnected: " + sessionID));
            }

            @Override public void onLogon() {
                notifier.processMessage(new Diagnostic(getName(), "Session logon: " + sessionID));
            }

            @Override public void onLogout() {
                notifier.processMessage(new Diagnostic(getName(), "Session logout: " + sessionID));
            }

            @Override public void onReset() {
                notifier.processMessage(new Diagnostic(getName(), "Session reset: " + sessionID));
            }

            @Override public void onRefresh() {
                notifier.processMessage(new Diagnostic(getName(), "Session refreshed: " + sessionID));
            }

            @Override public void onMissedHeartBeat() {
                notifier.processMessage(new Diagnostic(getName(), "Missed heartbeat: " + sessionID));
            }

            @Override public void onHeartBeatTimeout() {
                notifier.processMessage(new Diagnostic(getName(), "Heartbeat timeout: " + sessionID));
            }
        });
    }

    @Override public void onLogon(SessionID sessionID) {
        if (!configuration.isSessionManaged(sessionID)) {
            processMessage(new Online<>(sessionID));
        }
    }

    @Override public void onLogout(SessionID sessionID) {
        processMessage(new Offline<>(sessionID, "Session disconnected"));
    }

    @Override public void toAdmin(Message message, SessionID sessionID) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override public void toApp(Message message, SessionID sessionID) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    @Override public void processMessage(Object o) {
        listener.processMessage(o);
    }

    @Listener public void $(Online<SessionID> online) {
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
            this.status = status;
            notifier.processMessage(status);
            connectionPromise.processMessage(status);
            subscribe();
        }
    }

    @Listener public void $(Offline<SessionID> offline) {
        sessionStatus.put(offline.getSubject(), true);
        for (Feed feed : getAvailableFeeds()) {
            notifier.processMessage(new Offline<>(feed, String.format("%s is disconnected", getName().toUpperCase())));
        }
        final Offline<FixLiquidityProvider> status = new Offline<>(this, offline.getReason());
        this.status = status;
        notifier.processMessage(status);
        disconnectionPromise.processMessage(status);
    }

    @Listener public void $(Reject<Feed> reject) {
        notifier.processMessage(new Offline<>(reject.request, reject.reason));
    }

    @Override public String toString() {
        return configuration.name.toUpperCase();
    }

    private SocketInitiator createInitiator() throws ConfigError {
        SessionSettings settings = configuration.sessionSettings;
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        return new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }
}
