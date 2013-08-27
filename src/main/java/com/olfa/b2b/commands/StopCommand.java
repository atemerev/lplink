package com.olfa.b2b.commands;

import com.olfa.b2b.lp.SubscriptionMonitor;
import com.olfa.b2b.shell.Shell;

public class StopCommand implements Command {

    private final SubscriptionMonitor liquidityManager;
    private final Shell shell;
    private final String lpName;

    public StopCommand(SubscriptionMonitor liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @Override public void run() {
        if (lpName != null) {
            // todo implement
        } else {
            shell.log("Please specify liquidity provider name to stop.");
        }
    }
}
