package com.olfa.b2b.commands;

import com.olfa.b2b.LiquidityManager;
import com.olfa.b2b.events.Status;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.shell.Shell;

import java.util.Set;

public class StatusCommand implements Command {

    private final LiquidityManager liquidityManager;
    private final Shell shell;

    public StatusCommand(LiquidityManager liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
    }

    @Override public void run() {
        Set<String> keys = liquidityManager.liquidityProviders.keySet();
        for (String key : keys) {
            LiquidityProvider lp = liquidityManager.liquidityProviders.get(key);
            Status<? extends LiquidityProvider> status = lp.getStatus();
            shell.log(status.toString());
        }
    }
}
