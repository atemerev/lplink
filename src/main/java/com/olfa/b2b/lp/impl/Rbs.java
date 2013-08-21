package com.olfa.b2b.lp.impl;

import com.miriamlaurel.pms.listeners.MessageListener;
import com.olfa.b2b.domain.*;
import com.olfa.b2b.domain.ExecutionReport;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.domain.Reject;
import com.olfa.b2b.events.Diagnostic;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Online;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.lp.FixLiquidityProvider;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.*;
import quickfix.field.Side;
import quickfix.fix44.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Rbs extends FixLiquidityProvider {

    private static final String APPLICATION_PING_REQUEST = "U1";
    private static final String APPLICATION_PING_RESPONSE = "U2";

    private volatile Map<String, Subscription> subscriptions = Collections.unmodifiableMap(new HashMap<String, Subscription>());

    public Rbs(@NotNull Config conf, @Nullable MessageListener listener) throws ConfigurationException {
        super("rbs", conf, listener);
    }

    public Rbs(Config conf) throws ConfigurationException {
        super("rbs", conf);
    }

    public void subscribe(Subscription subscription) {
        QuoteRequest message = new QuoteRequest();
        message.set(new QuoteReqID(subscription.requestId));
        message.setString(Symbol.FIELD, subscription.instrument.toString());
        QuoteRequest.NoRelatedSym group = new QuoteRequest.NoRelatedSym();
        if (subscription.amount == null) {
            throw new IllegalArgumentException("RBS doesn't support blanket subscriptions");
        }
        group.setString(OrderQty.FIELD, subscription.amount.toPlainString());
        group.set(new SettlType(SettlType.REGULAR));
        message.addGroup(group);
        sendTo(QUOTE_SESSION, message);
    }

    public void unsubscribe(Subscription subscription) {
        QuoteCancel message = new QuoteCancel();
        message.set(new QuoteReqID(subscription.requestId));
        message.set(new QuoteCancelType(QuoteCancelType.CANCEL_FOR_SYMBOL));
        sendTo(QUOTE_SESSION, message);
    }

    @Override
    public void trade(Order order) {
        NewOrderSingle message = new NewOrderSingle();
        message.set(new ClOrdID(order.id));
        String account = configuration.getString(TRADE_SESSION, "account");
        assert account != null;
        message.set(new Account(account));
        message.set(new Symbol(order.instrument.toString()));
        message.set(new Side(order.side == com.olfa.b2b.domain.Side.BUY ? Side.BUY : Side.SELL));
        message.set(new OrderQty(order.amount.doubleValue()));
        if (order.quote != null) {
            message.set(new OrdType(OrdType.PREVIOUSLY_QUOTED));
            message.setString(QuoteReqID.FIELD, order.quote.quoteReqId);
            message.set(new QuoteID(order.quote.lpQuoteId));
        } else {
            message.set(new OrdType(OrdType.LIMIT));
        }
        message.set(new Price(order.price.doubleValue()));
        message.set(new TransactTime(new Date()));
        message.set(new Currency(order.instrument.primary.toString()));
        sendTo(TRADE_SESSION, message);
    }

    public void onMessage(TradingSessionStatus message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        int status = message.getTradSesStatus().getValue();
        switch (status) {
            case TradSesStatus.OPEN:
                processMessage(new Online<>(sessionID));
                break;
            case TradSesStatus.CLOSED:
                processMessage(new Offline<>(sessionID, String.format("LP %s: session %s is offline",
                        getName(), sessionID.toString())));
        }
    }

    public void onMessage(QuoteRequestReject message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String requestId = message.getQuoteReqID().getValue();
        int code = message.getQuoteRequestRejectReason().getValue();
        String reason = message.isSetText() ? message.getText().getValue() : String.format("Request rejected: %d", code);
        Subscription subscription = this.subscriptions.get(requestId);
        Map<String, Subscription> newSubscriptions = new HashMap<>(subscriptions);
        newSubscriptions.remove(requestId);
        this.subscriptions = newSubscriptions;
        processMessage(new Reject<>(subscription, reason));
    }

    public void onMessage(quickfix.fix44.Quote message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String requestId = message.getQuoteReqID().getValue();
        Subscription subscription = subscriptions.get(requestId);
        if (subscription != null) {
            String quoteId = message.getQuoteID().getValue();
            final double bidPx = message.getBidPx().getValue();
            final double offerPx = message.getOfferPx().getValue();
            boolean tradable = QuoteCondition.OPEN_ACTIVE.equals(message.getString(QuoteCondition.FIELD));
            if (tradable && bidPx > 0) {
                onQuote(new Quote(Quote.Action.PUT, quoteId, subscription, Quote.Side.BID, requestId, null, bidPx));
            } else {
                onQuote(new Quote(Quote.Action.REMOVE, quoteId, subscription, Quote.Side.BID, requestId));
            }
            if (tradable && offerPx > 0) {
                onQuote(new Quote(Quote.Action.PUT, quoteId, subscription, Quote.Side.ASK, requestId, null, offerPx));
            } else {
                onQuote(new Quote(Quote.Action.REMOVE, quoteId, subscription, Quote.Side.ASK, requestId));
            }
        }
    }

    public void onMessage(quickfix.fix44.ExecutionReport message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String lpOrderId = message.getOrderID().getValue();
        String clOrderId = message.getClOrdID().getValue();
        if (orders.containsKey(clOrderId)) {
            Order order = orders.get(clOrderId);
            char ordStatus = message.getOrdStatus().getValue();
            switch (ordStatus) {
                case OrdStatus.NEW:
                    onExecutionResponse(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.NEW, null, null));
                    break;
                case OrdStatus.FILLED:
                    long timestamp = message.getTransactTime().getValue().getTime();
                    BigDecimal price = BigDecimal.valueOf(message.getPrice().getValue());
                    Trade trade = new Trade(lpOrderId, timestamp, order.instrument, order.side, order.amount, price);
                    onExecutionResponse(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.FILLED, trade, null));
                    orders.remove(clOrderId);
                    break;
                case OrdStatus.REJECTED:
                    String text = message.isSetText() ? message.getText().getValue() : null;
                    onExecutionResponse(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.REJECTED, null, text));
                    orders.remove(clOrderId);
            }
        } else {
            processMessage(new Diagnostic(getName(), String.format("Execution report for unknown order received: %s", clOrderId)));
        }
    }

    @Override
    public void onMessage(Message message, SessionID sid) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String msgType = message.getHeader().getString(MsgType.FIELD);
        if (APPLICATION_PING_REQUEST.equals(msgType)) {
            long time = message.getUtcTimeStamp(TransactTime.FIELD).getTime();
            String reqId = message.getString(TestReqID.FIELD);
            Message pong = new quickfix.fix44.Message();
            pong.getHeader().setString(MsgType.FIELD, APPLICATION_PING_RESPONSE);
            pong.setString(TestReqID.FIELD, reqId);
            pong.setUtcTimeStamp(TransactTime.FIELD, new Date(time));
            sendTo(sid, pong);
        } else {
            super.onMessage(message, sid);
        }
    }
}
