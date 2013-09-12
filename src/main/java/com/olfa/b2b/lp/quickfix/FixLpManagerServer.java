package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.domain.CurrencyPair;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.lp.LpManager;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NetworkStatusRequest;
import quickfix.fix44.NetworkStatusResponse;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FixLpManagerServer extends MessageCracker implements Application {

    private static final int FIX_DESIRED_STATUS_VALUE = 11200;
    private static final int FIX_NO_SUBSCRIPTIONS_GROUP = 11201;
    private static final int FIX_TIER = 11202;

    private final LpManager lpManager;
    private final Acceptor acceptor;

    public FixLpManagerServer(LpManager lpManager) throws ConfigurationException {
        this.lpManager = lpManager;
        this.acceptor = createAcceptor();
        try {
            acceptor.start();
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
    }

    @Override
    public void onLogon(SessionID sessionId) {
    }

    @Override
    public void onLogout(SessionID sessionId) {
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionId);
    }

    public void onMessage(NetworkStatusRequest message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        final String requestId = message.getNetworkRequestID().getValue();
        final String responseId = UUID.randomUUID().toString();
        final Map<String, LiquidityProvider> lps = lpManager.liquidityProviders;
        NetworkStatusResponse response = new NetworkStatusResponse();
        response.set(new NetworkStatusResponseType(NetworkStatusResponseType.FULL));
        response.set(new NetworkRequestID(requestId));
        response.set(new NetworkResponseID(responseId));
        for (String lpName : lps.keySet()) {
            Group group1 = new Group(NoCompIDs.FIELD, RefCompID.FIELD, new int[]{RefCompID.FIELD, StatusValue.FIELD, FIX_DESIRED_STATUS_VALUE, StatusText.FIELD, FIX_NO_SUBSCRIPTIONS_GROUP});
            group1.setString(RefCompID.FIELD, lpName);
            LiquidityProvider lp = lps.get(lpName);
            if (lp == null) {
                group1.setInt(StatusValue.FIELD, StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP);
                // todo set correct desired status, add correct text message
                group1.setInt(FIX_DESIRED_STATUS_VALUE, StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP);
                group1.setString(StatusText.FIELD, "LP is disconnected");
            } else {
                group1.setInt(StatusValue.FIELD, StatusValue.CONNECTED);
                // todo set correct desired status
                group1.setInt(FIX_DESIRED_STATUS_VALUE, StatusValue.CONNECTED);
            }
            Group group2 = new Group(FIX_NO_SUBSCRIPTIONS_GROUP, Symbol.FIELD, new int[]{Symbol.FIELD, Quantity.FIELD,
                    FIX_TIER, StatusValue.FIELD, FIX_DESIRED_STATUS_VALUE});
            for (Subscription subscription : lpManager.subscriptions) {
                if (lpName.equals(subscription.source)) {
                    group2.setString(Symbol.FIELD, subscription.instrument.toString());
                    if (subscription.amount != null) {
                        group2.setDecimal(Quantity.FIELD, subscription.amount);
                    }
                    if (subscription.classifier != null) {
                        group2.setString(FIX_TIER, subscription.classifier);
                    }
                    int subscriptionStatus = lpManager.isOnline(subscription) ? StatusValue.CONNECTED : StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP;
                    group2.setInt(StatusValue.FIELD, subscriptionStatus);
                    // todo set correct desired status
                    group2.setInt(FIX_DESIRED_STATUS_VALUE, subscriptionStatus);
                    group1.addGroup(group2);
                }
            }
            response.addGroup(group1);
        }
        try {
            Session.sendToTarget(response, sessionID);
        } catch (SessionNotFound sessionNotFound) {
            throw new ConfigurationException(sessionNotFound);
        }
    }

    private Acceptor createAcceptor() throws ConfigurationException {
        try {
            SessionSettings settings = loadSessionSettings();
            MessageStoreFactory storeFactory = new MemoryStoreFactory();
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            return new SocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    private SessionSettings loadSessionSettings() throws ConfigurationException {
        try {
            return new SessionSettings(FixLpManagerServer.class.getResourceAsStream("/server/fix-session.ini"));
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    public static void main(String[] args) {
        Set<Subscription> subs = new HashSet<>();
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), new BigDecimal("1000000"), null));
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), new BigDecimal("3000000"), null));
        subs.add(new Subscription("rbs", new CurrencyPair("EUR/USD"), new BigDecimal("5000000"), null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), new BigDecimal("1000000"), null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), new BigDecimal("3000000"), null));
        subs.add(new Subscription("rbs", new CurrencyPair("GBP/USD"), new BigDecimal("5000000"), null));
        subs.add(new Subscription("bnp", new CurrencyPair("USD/JPY"), new BigDecimal("1000000"), "BLUE"));
        subs.add(new Subscription("bnp", new CurrencyPair("USD/JPY"), new BigDecimal("3000000"), "BLUE"));
        LpManager lpManager = new LpManager(subs, 1000, 30000);
        FixLpManagerServer server = new FixLpManagerServer(lpManager);
        System.out.println("LP manager server started");
    }
}
