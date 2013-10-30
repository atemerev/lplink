package com.olfa.b2b.commands;

import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;

public class StopCommand implements Command {

    private final LpManager lpManager;
    private final Shell shell;
    private final String lpName;

    public StopCommand(LpManager lpManager, Shell shell, String[] tokens) {
        this.lpManager = lpManager;
        this.shell = shell;
        this.lpName = tokens.length > 1 ? tokens[1] : null;
    }

    @Override public void run() {
        if (lpName != null) {
            lpManager.disconnect(lpName);
            shell.log("Disconnected: " + lpName);
        } else {
            shell.log("Please specify liquidity provider name to stop.");
        }
    }
}
