package com.olfa.b2b.lp.impl;

import com.olfa.b2b.domain.*;
import com.olfa.b2b.domain.ExecutionReport;
import com.olfa.b2b.domain.Quote;
import com.olfa.b2b.events.StatusEvent;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.lp.FixLiquidityProvider;
import com.typesafe.config.Config;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.*;
import quickfix.field.Side;
import quickfix.fix44.*;

import java.math.BigDecimal;
import java.util.Date;

public class Rbs extends FixLiquidityProvider {

    private static final String APPLICATION_PING_REQUEST = "U1";
    private static final String APPLICATION_PING_RESPONSE = "U2";

    public Rbs(Config conf) throws ConfigurationException {
        super("rbs", conf);
    }

    public void doSubscribe(Subscription subscription) {
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

    public void doUnsubscribe(Subscription subscription) {
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
                fireStatusEvent(new StatusEvent(getName(), "Trading session is now open", StatusEvent.Type.TRADING_SESSION_ONLINE));
                break;
            case TradSesStatus.CLOSED:
                fireStatusEvent(new StatusEvent(getName(), "Trading session is now closed", StatusEvent.Type.TRADING_SESSION_OFFLINE));
                break;
            default:
                // todo: log
        }
    }

    public void onMessage(QuoteRequestReject message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        String requestId = message.getQuoteReqID().getValue();
        int code = message.getQuoteRequestRejectReason().getValue();
        String reason = message.isSetText() ? message.getText().getValue() : String.format("Request %s rejected: %d", requestId, code);
        fireStatusEvent(new StatusEvent(
                getName(),
                reason,
                StatusEvent.Type.SUBSCRIPTION_CANCELLED));
    }

    public void onMessage(quickfix.fix44.Quote message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        final String requestId = message.getQuoteReqID().getValue();
        final String quoteId = message.getQuoteID().getValue();
        final Subscription subscription = subscriptions.get(requestId);
        if (subscription != null) {
            final double bidPx = message.getBidPx().getValue();
            final double offerPx = message.getOfferPx().getValue();
            boolean tradable = QuoteCondition.OPEN_ACTIVE.equals(message.getString(QuoteCondition.FIELD));
            if (tradable && bidPx > 0) {
                fireQuote(new Quote(Quote.Action.PUT, quoteId, subscription, Quote.Side.BID, requestId, null, bidPx));
            } else {
                fireQuote(new Quote(Quote.Action.REMOVE, quoteId, subscription, Quote.Side.BID, requestId));
            }
            if (tradable && offerPx > 0) {
                fireQuote(new Quote(Quote.Action.PUT, quoteId, subscription, Quote.Side.ASK, requestId, null, offerPx));
            } else {
                fireQuote(new Quote(Quote.Action.REMOVE, quoteId, subscription, Quote.Side.ASK, requestId));
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
                    fireExecutionReport(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.NEW, null, null));
                    break;
                case OrdStatus.FILLED:
                    long timestamp = message.getTransactTime().getValue().getTime();
                    BigDecimal price = BigDecimal.valueOf(message.getPrice().getValue());
                    Trade trade = new Trade(lpOrderId, timestamp, order.instrument, order.side, order.amount, price);
                    fireExecutionReport(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.FILLED, trade, null));
                    orders.remove(clOrderId);
                    break;
                case OrdStatus.REJECTED:
                    String text = message.isSetText() ? message.getText().getValue() : null;
                    fireExecutionReport(new ExecutionReport(lpOrderId, order, ExecutionReport.ExecutionStatus.REJECTED, null, text));
                    orders.remove(clOrderId);
            }
        } else {
            // todo log execution report for unknown order
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
