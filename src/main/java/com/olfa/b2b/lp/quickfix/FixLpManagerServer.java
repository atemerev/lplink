package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.fix.FixGroup;
import com.olfa.b2b.fix.FixMessageListener;
import com.olfa.b2b.fix.FixSpan;
import com.olfa.b2b.fix.FixTag;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.lp.LpManager;
import quickfix.field.*;
import quickfix.fix44.NetworkStatusRequest;
import quickfix.fix44.NetworkStatusResponse;

import java.util.*;

public class FixLpManagerServer implements FixMessageListener {

    private static final int FIX_DESIRED_STATUS_VALUE = 11200;
    private static final int FIX_NO_SUBSCRIPTIONS_GROUP = 11201;
    private static final int FIX_TIER = 11202;

    private final LpManager lpManager;

    public FixLpManagerServer(LpManager lpManager) throws ConfigurationException {
        this.lpManager = lpManager;
    }

    @Override
    public List<FixSpan> onFixMessage(FixSpan message) {
        final String type = message.getTag(MsgType.FIELD).getValue();
        if (NetworkStatusRequest.MSGTYPE.equals(type)) {
            final String requestId = message.getTag(NetworkRequestID.FIELD).getValue();
            final String responseId = UUID.randomUUID().toString();
            final List<FixSpan> lpSpans = new ArrayList<>();
            final Set<String> lpNames = lpManager.getLpNames();
            for (String lpName : lpNames) {
                LiquidityProvider lp = lpManager.getLp(lpName);
                final int status = lp == null ? StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP : StatusValue.CONNECTED;
                final Set<Subscription> subscriptions = lpManager.getSubscriptions();
                final List<FixSpan> subSpans = new ArrayList<>();
                for (Subscription subscription : subscriptions) {
                    if (lpName.equals(subscription.source)) {
                        int subscriptionStatus = lpManager.isOnline(subscription) ?
                                StatusValue.CONNECTED : StatusValue.NOT_CONNECTED_DOWN_EXPECTED_UP;
                        subSpans.add(new FixSpan(
                                new FixTag(Symbol.FIELD, subscription.instrument.toString()),
                                subscription.amount != null ?
                                        new FixTag(Quantity.FIELD, subscription.amount.toPlainString()) : null,
                                subscription.classifier != null ? new FixTag(FIX_TIER, subscription.classifier) : null,
                                new FixTag(StatusValue.FIELD, subscriptionStatus),
                                // todo set correct desired status
                                new FixTag(FIX_DESIRED_STATUS_VALUE, subscriptionStatus)
                        ));
                    }

                }
                lpSpans.add(new FixSpan(
                        new FixTag(RefCompID.FIELD, lpName),
                        new FixTag(StatusValue.FIELD, status),
                        // todo set correct desired status
                        new FixTag(FIX_DESIRED_STATUS_VALUE, status),
                        new FixGroup(FIX_NO_SUBSCRIPTIONS_GROUP, subSpans)
                ));
            }
            FixSpan response = new FixSpan(
                    new FixTag(MsgType.FIELD, NetworkStatusResponse.MSGTYPE),
                    new FixTag(NetworkStatusResponseType.FIELD, NetworkStatusResponseType.FULL),
                    new FixTag(NetworkRequestID.FIELD, requestId),
                    new FixTag(NetworkResponseID.FIELD, responseId),
                    new FixGroup(NoCompIDs.FIELD, lpSpans)
            );
            ArrayList<FixSpan> result = new ArrayList<>();
            result.add(response);
            return Collections.unmodifiableList(result);
        } else {
            //noinspection unchecked
            return Collections.EMPTY_LIST;
        }
    }
}
