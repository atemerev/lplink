package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.exception.ConfigurationException;
import quickfix.*;
import quickfix.field.NetworkRequestID;
import quickfix.field.NetworkRequestType;
import quickfix.fix44.NetworkStatusRequest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class FixLpManagerClient extends MessageCracker implements Application {

    private final Initiator initiator;

    public FixLpManagerClient() {
        try {
            this.initiator = createInitiator();
            initiator.start();
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    private Initiator createInitiator() throws ConfigError {
        SessionSettings settings = loadSessionSettings();
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        return new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
    }

    private SessionSettings loadSessionSettings() throws ConfigurationException {
        try {
            return new SessionSettings(FixLpManagerServer.class.getResourceAsStream("/server/fix-client.ini"));
        } catch (ConfigError configError) {
            throw new ConfigurationException(configError);
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
    }

    @Override
    public void onLogon(SessionID sessionId) {
        try {
            System.out.println("Logon: " + sessionId.toString());
            NetworkStatusRequest req = new NetworkStatusRequest();
            req.set(new NetworkRequestType(NetworkRequestType.SNAPSHOT));
            req.set(new NetworkRequestID(UUID.randomUUID().toString()));
            Session.sendToTarget(req, sessionId);
        } catch (SessionNotFound sessionNotFound) {
            throw new ConfigurationException(sessionNotFound);
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Logout: " + sessionId.toString());
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
        System.out.println("<< " + message);
    }

    public static void main(String[] args) throws Exception {
        new FixLpManagerClient();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
