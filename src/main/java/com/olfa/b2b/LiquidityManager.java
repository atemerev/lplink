package com.olfa.b2b;

import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Online;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.ValidationException;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.lp.SubscriptionMonitor;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class LiquidityManager {

    public final Map<String, LiquidityProvider> liquidityProviders;
    public final SubscriptionMonitor subscriptionMonitor;

    public LiquidityManager(@NotNull Config config) throws ConfigurationException {
        this.liquidityProviders = createProviders(config);
        Set<Subscription> allSubscriptions = new HashSet<>();
        this.subscriptionMonitor = new SubscriptionMonitor(Collections.unmodifiableSet(allSubscriptions), getTickPeriod(config), getTimeout(config));
    }

    @SuppressWarnings("unchecked")
    private Map<String, LiquidityProvider> createProviders(Config config) throws ConfigurationException {
        try {
            Map<String, LiquidityProvider> lpMap = new HashMap<>();
            final Config providersConf = config.getConfig("b2b.providers");
            for (String lpName : providersConf.root().keySet()) {
                Config lpConfig = providersConf.getConfig(lpName);
                Class<? extends LiquidityProvider> lpClass = (Class<? extends LiquidityProvider>) Class.forName(lpConfig.getString("implementation"));
                LiquidityProvider lp = lpClass.getConstructor(Config.class).newInstance(lpConfig);
                lpMap.put(lp.getName(), lp);
            }
            return Collections.unmodifiableMap(lpMap);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("LP implementation class not found", e);
        } catch (InvocationTargetException | InstantiationException e) {
            throw new ConfigurationException("Error while instantiating LP implementation", e);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor accepting Config instance not found in LP implementation", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("No public constructor found in LP implementation", e);
        }
    }

    private long getTickPeriod(Config config) {
        return config.getLong("b2b.subscriptions.tick-period");
    }

    private long getTimeout(Config config) {
        return config.getLong("b2b.subscriptions.timeout");
    }

    public Promise<Online<? extends LiquidityProvider>> start(String lpName) throws ValidationException {
        LiquidityProvider lp = liquidityProviders.get(lpName);
        if (lp != null) {
            if (!lp.getStatus().isOnline()) {
                return lp.connect();
            } else {
                throw new ValidationException(String.format("LP %s is already online", lpName));
            }
        } else {
            throw new ValidationException(String.format("LP %s not found", lpName));
        }
    }

    public Promise<Online<Subscription>> subscribe(Subscription subscription) throws ValidationException {
        LiquidityProvider lp = liquidityProviders.get(subscription.source);
        if (lp != null) {
            if (lp.getStatus().isOnline()) {
                return lp.subscribe(subscription);
            } else {
                throw new ValidationException(String.format("LP %s is offline", lp.getName()));
            }
        } else {
            throw new ValidationException(String.format("LP %s not found", subscription.source));
        }
    }

    public Promise<Offline<Subscription>> unsubscribe(Subscription subscription) throws ValidationException {
        LiquidityProvider lp = liquidityProviders.get(subscription.source);
        if (lp != null) {
            if (lp.getStatus().isOnline()) {
                return lp.unsubscribe(subscription);
            } else {
                throw new ValidationException(String.format("LP %s is offline", lp.getName()));
            }
        } else {
            throw new ValidationException(String.format("LP %s not found", subscription.source));
        }
    }

    public Promise<Offline<? extends LiquidityProvider>> stop(String lpName) {
        LiquidityProvider lp = liquidityProviders.get(lpName);
        if (lp != null) {
            if (lp.getStatus().isOnline()) {
                return lp.disconnect();
            } else {
                throw new ValidationException(String.format("LP %s is already offline", lpName));
            }
        } else {
            throw new ValidationException(String.format("LP not found: %s", lpName));
        }
    }
}
