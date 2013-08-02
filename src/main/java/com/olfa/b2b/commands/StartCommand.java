package com.olfa.b2b.commands;

import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.LiquidityManager;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.events.Status;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.shell.Shell;

import java.util.concurrent.TimeoutException;

public class StartCommand implements Command {

    private final LiquidityManager liquidityManager;
    private final Shell shell;
    private final String lpName;

    public StartCommand(LiquidityManager liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @SuppressWarnings("unchecked")
    @Override public void run() {
        if (lpName != null) {
            final Promise<Status<? extends LiquidityProvider>> promise = liquidityManager.start(lpName);
            try {
                Status<? extends LiquidityProvider> status = promise.get(5000);
                if (status.isOnline()) {
                    shell.log(String.format("Liquidity provider %s is online.", lpName.toUpperCase()));
                } else {
                    Offline<? extends LiquidityProvider> offline = (Offline<? extends LiquidityProvider>) status;
                    shell.log(String.format("Liquidity provider %s couldn't start: %s.", lpName.toUpperCase(), offline.getReason()));
                }
            } catch (TimeoutException e) {
                shell.log(String.format("Couldn't start %s due to timeout.", lpName.toUpperCase()));
            }
        } else {
            shell.log("Please specify liquidity provider name to start.");
        }
    }
}

