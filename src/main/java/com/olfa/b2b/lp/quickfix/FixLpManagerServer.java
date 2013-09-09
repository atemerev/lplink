package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.lp.LpManager;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix50.NetworkCounterpartySystemStatusRequest;
import quickfix.fix50.NetworkCounterpartySystemStatusResponse;

import java.util.Map;
import java.util.UUID;

public class FixLpManagerServer extends MessageCracker implements Application {

    private static final int FIX_DESIRED_STATUS_VALUE = 11200;
    private static final int FIX_NO_SUBSCRIPTIONS_GROUP = 11201;
    private static final int FIX_TIER = 11202;

    private final LpManager lpManager;

    public FixLpManagerServer(LpManager lpManager) {
        this.lpManager = lpManager;
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

    protected void onMessage(NetworkCounterpartySystemStatusRequest message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        final String requestId = message.getNetworkRequestID().getValue();
        final String responseId = UUID.randomUUID().toString();
        final Map<String, LiquidityProvider> lps = lpManager.liquidityProviders;
        NetworkCounterpartySystemStatusResponse response = new NetworkCounterpartySystemStatusResponse();
        response.set(new NetworkStatusResponseType(NetworkStatusResponseType.FULL));
        response.set(new NetworkRequestID(requestId));
        response.set(new NetworkResponseID(responseId));
        NetworkCounterpartySystemStatusResponse.NoCompIDs group1 = new NetworkCounterpartySystemStatusResponse.NoCompIDs();
        for (String lpName : lps.keySet()) {
            group1.set(new RefCompID(lpName));
            LiquidityProvider lp = lps.get(lpName);
            if (lp == null) {
                group1.set(new StatusValue(StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP));
                // todo set correct desired status, add correct text message
                group1.setInt(FIX_DESIRED_STATUS_VALUE, StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP);
                group1.set(new StatusText("LP is disconnected"));
            } else {
                group1.set(new StatusValue(StatusValue.CONNECTED));
                // todo set correct desired status
                group1.setInt(FIX_DESIRED_STATUS_VALUE, StatusValue.CONNECTED);
            }
            Group group2 = new Group(FIX_NO_SUBSCRIPTIONS_GROUP, Symbol.FIELD, new int[]{Symbol.FIELD, Quantity.FIELD,
                    FIX_TIER, StatusValue.FIELD, FIX_DESIRED_STATUS_VALUE});
            for (Subscription subscription : lpManager.subscriptions) {
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
            response.addGroup(group1);
        }
    }
}
