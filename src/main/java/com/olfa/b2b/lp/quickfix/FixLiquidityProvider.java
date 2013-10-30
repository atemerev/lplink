package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.domain.Order;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.events.StatusEvent;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.LifecycleException;
import com.olfa.b2b.exception.RejectedException;
import com.olfa.b2b.lp.AbstractLiquidityProvider;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

public abstract class FixLiquidityProvider extends AbstractLiquidityProvider implements Application {

    public static final String QUOTE_SESSION = "quote";
    public static final String TRADE_SESSION = "trade";

    private static final Logger log = LoggerFactory.getLogger(FixLiquidityProvider.class);

    private final CountDownLatch startupLatch = new CountDownLatch(1);
    private final Initiator initiator;
    private final MessageCracker cracker;
    protected final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();
    protected final ConcurrentMap<SessionID, Boolean> sessionStatus = new ConcurrentHashMap<>();
    protected final FixLpConfiguration configuration;

    protected FixLiquidityProvider(String name, @NotNull Config conf) throws ConfigurationException {
        super();
        try {
            this.configuration = new FixLpConfiguration(name, conf);
            for (SessionID sid : configuration.sessionIDs.values()) {
                sessionStatus.put(sid, false);
            }
            this.initiator = createInitiator();
            this.cracker = new MessageCracker(this);
            initiator.start();
            startupLatch.await();
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        } catch (InterruptedException e) {
            throw new LifecycleException("Interrupted");
        }
    }

    public void disconnect() {
        initiator.stop(true);
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
    public void subscribe(Subscription subscription) {
        subscriptions.put(subscription.requestId, subscription);
        doSubscribe(subscription);
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        subscriptions.remove(subscription.requestId);
        doUnsubscribe(subscription);
    }

    @Override
    public void trade(Order order) {
        orders.put(order.id, order);
        doTrade(order);
    }

    public abstract void doSubscribe(Subscription subscription);

    public abstract void doUnsubscribe(Subscription subscription);

    public abstract void doTrade(Order order);

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
        disconnect();
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
        cracker.crack(message, sessionID);
    }

    private void onSessionOnline(SessionID sid) {
        log.info(String.format("%s FIX session online: %s", getName(), sid.toString()));
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
            log.info(String.format("%s is connected", getName()));
        }
    }

    @Override
    public String toString() {
        return configuration.name.toUpperCase();
    }

    private SocketInitiator createInitiator() throws ConfigError {
        SessionSettings settings = configuration.sessionSettings;
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        return new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }
}
