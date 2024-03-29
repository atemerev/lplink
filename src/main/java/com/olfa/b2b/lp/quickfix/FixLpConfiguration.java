package com.olfa.b2b.lp.quickfix;

import com.olfa.b2b.exception.ConfigurationException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FixLpConfiguration {

    private static final String DEFAULT_CONFIG_PATH = "lp/fix-default.conf";

    public final @NotNull Config defaultConfig;
    public final Config rawConfig;
    public final String name;
    public final Map<String, SessionID> sessionIDs;
    public final SessionSettings sessionSettings;
//    public final Set<Subscription> subscriptions;

    @SuppressWarnings("NullableProblems")
    public FixLpConfiguration(@NotNull String name, @NotNull Config rawConfig, @NotNull Config defaultConfig) throws ConfigurationException {
        this.rawConfig = rawConfig;
        this.defaultConfig = defaultConfig;
        this.name = name;
        this.sessionSettings = new SessionSettings();
        Config defaultSession = rawConfig.getConfig("sessions.default");
        populateDefaultSession(defaultSession);
        this.sessionIDs = populateSessions(defaultSession.getString("version"), rawConfig);
//        this.subscriptions = mkFeeds(rawConfig);
    }

    public FixLpConfiguration(@NotNull String name, @NotNull Config rawConfig) {
        this(name, rawConfig, ConfigFactory.load(DEFAULT_CONFIG_PATH));
    }

    public boolean isSessionManaged(String name) {
        final String path = "sessions." + name + ".managed";
        final String defaultPath = "sessions.default.managed";
        return rawConfig.hasPath(path) ? rawConfig.getBoolean(path) :
                rawConfig.hasPath(defaultPath) && rawConfig.getBoolean(defaultPath);
    }

    public @Nullable String getString(String sessionName, String parameter) {
        final String path = "sessions." + sessionName + "." + parameter;
        return rawConfig.getString(path);
    }

    public boolean isSessionManaged(SessionID sid) {
        for (Map.Entry<String, SessionID> entry : sessionIDs.entrySet()) {
            if (sid.equals(entry.getValue())) {
                return isSessionManaged(entry.getKey());
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void populateDefaultSession(Config defaultSession) {
        Map unwrapped = defaultConfig.root().unwrapped();
        sessionSettings.set(unwrapped);
        sessionSettings.setString("ConnectionType", "initiator");
        sessionSettings.setString("DataDictionary", "lp/" + name + "/" + defaultSession.getString("dictionary"));
        // todo JDBC support
        sessionSettings.setString("LogFactory", "file");
        sessionSettings.setString("FileLogPath", defaultSession.getString("log"));
        sessionSettings.setString("MessageStoreFactory", "memory");
        sessionSettings.setBool("UseDataDictionary", true);
        sessionSettings.setString("BeginString", defaultSession.getString("version"));
        sessionSettings.setLong("ReconnectInterval", 2000);
        sessionSettings.setLong("LogonTimeout", 5);
        sessionSettings.setBool("SocketUseSSL", defaultSession.getBoolean("ssl"));
        sessionSettings.setLong("HeartBtInt", defaultSession.getInt("heartbeat"));
        if (defaultSession.hasPath("keystore")) {
            sessionSettings.setString("SocketKeyStore", defaultSession.getString("keystore"));
        }
        if (defaultSession.hasPath("keystore-password")) {
            sessionSettings.setString("SocketKeyStorePassword", defaultSession.getString("keystore-password"));
        }
        for (String key: defaultSession.root().keySet()) {
            if (key.substring(0, 1).equals(key.substring(0, 1).toUpperCase())) {
                sessionSettings.setString(key, defaultSession.getString(key));
            }
        }
    }

    private Map<String, SessionID> populateSessions(String beginString, Config rawConfig) {
        Map<String, SessionID> sessionMap = new HashMap<>();
        for (String sessionName : rawConfig.getConfig("sessions").root().keySet()) {
            if (!"default".equals(sessionName)) {
                Config sessionConfig = rawConfig.getConfig("sessions." + sessionName);
                SessionID sid = parseSessionId(beginString, sessionConfig.getString("session"));
                sessionMap.put(sessionName, sid);
                String[] tokens = sessionConfig.getString("endpoint").split(":");
                sessionSettings.setString(sid, "SocketConnectHost", tokens[0]);
                sessionSettings.setString(sid, "SocketConnectPort", tokens[1]);
                if (sessionConfig.hasPath("account")) {
                    sessionSettings.setString(sid, "Account", sessionConfig.getString("account"));
                }
                for (String key: sessionConfig.root().keySet()) {
                    if (key.substring(0, 1).equals(key.substring(0, 1).toUpperCase())) {
                        sessionSettings.setString(key, sessionConfig.getString(key));
                    }
                }
            }
        }
        return Collections.unmodifiableMap(sessionMap);
    }

    private SessionID parseSessionId(String beginString, String sessionString) {
        // todo support sub-ids
        String[] tokens = sessionString.split("\\s*\\->\\s*");
        return new SessionID(beginString, tokens[0], tokens[1]);
    }


/*
    private Set<Subscription> mkFeeds(Config conf) {
        Set<Subscription> subscriptions = new HashSet<>();
        for (String symbol : conf.getStringList("instruments")) {
            CurrencyPair instrument = new CurrencyPair(symbol);
            for (long amount : conf.getLongList("bands"))
                subscriptions.add(new Subscription(name, instrument, new BigDecimal(amount), null));
        }
        return Collections.unmodifiableSet(subscriptions);
    }
*/
}
