package com.olfa.b2b;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.exception.ConfigurationException;
import com.olfa.b2b.exception.LifecycleException;
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

    public LiquidityManager(@NotNull Config config) throws ConfigurationException, LifecycleException {
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

    public void subscribe(Subscription subscription) throws ValidationException {
        LiquidityProvider lp = liquidityProviders.get(subscription.source);
        lp.subscribe(subscription);
    }

    public void unsubscribe(Subscription subscription) throws ValidationException {
        LiquidityProvider lp = liquidityProviders.get(subscription.source);
        lp.unsubscribe(subscription);
    }
}
