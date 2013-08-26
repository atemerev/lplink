package com.olfa.b2b.commands;

import com.olfa.b2b.LiquidityManager;
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
            // todo implement
        }
    }
}

