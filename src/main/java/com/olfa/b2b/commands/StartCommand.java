package com.olfa.b2b.commands;

import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;

public class StartCommand implements Command {

    private final LpManager liquidityManager;
    private final Shell shell;
    private final String lpName;

    public StartCommand(LpManager liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @SuppressWarnings("unchecked")
    @Override public void run() {
        if (lpName != null) {
            liquidityManager.connect(lpName);
        }
    }
}

