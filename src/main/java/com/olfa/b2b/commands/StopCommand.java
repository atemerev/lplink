package com.olfa.b2b.commands;

import com.miriamlaurel.prometheus.Promise;
import com.olfa.b2b.LiquidityManager;
import com.olfa.b2b.events.Offline;
import com.olfa.b2b.lp.LiquidityProvider;
import com.olfa.b2b.shell.Shell;

public class StopCommand implements Command {

    private final LiquidityManager liquidityManager;
    private final Shell shell;
    private final String lpName;

    public StopCommand(LiquidityManager liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @Override public void run() {
        if (lpName != null) {
            final Promise<Offline<? extends LiquidityProvider>> promise = liquidityManager.stop(lpName);
            promise.append(new Runnable() {
                @Override public void run() {
                    shell.log(String.format("Liquidity provider %s is offline.", lpName.toUpperCase()));
                }
            });
        } else {
            shell.log("Please specify liquidity provider name to stop.");
        }
    }
}
