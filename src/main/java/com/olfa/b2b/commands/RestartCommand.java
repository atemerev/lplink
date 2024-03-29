package com.olfa.b2b.commands;

import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;

public class RestartCommand implements Command {

    private final LpManager liquidityManager;
    private final Shell shell;
    private final String lpName;

    public RestartCommand(LpManager liquidityManager, Shell shell, String[] tokens) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @Override public void run() {
        shell.log("Restart command is not yet implemented.");
    }
}
