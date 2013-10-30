package com.olfa.b2b.commands;

import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;
import org.jetbrains.annotations.Nullable;

public class CommandParser {

    private final LpManager liquidityManager;
    private final Shell shell;

    public CommandParser(LpManager liquidityManager, Shell shell) {
        this.liquidityManager = liquidityManager;
        this.shell = shell;
    }

    public @Nullable Command parseCommand(String line) {
        if (line == null) {
            return null;
        }
        String[] tokens = line.trim().split(" ");
        if (tokens.length == 0) {
            return null;
        } else {
            switch(tokens[0]) {
                // todo fix commands
                case "start":
                    return new StartCommand(liquidityManager, shell, tokens);
                case "stop":
                    return new StopCommand(liquidityManager, shell, tokens);
                case "restart":
                    return new RestartCommand(liquidityManager, shell, tokens);
                case "market":
//                    return new MarketCommand(liquidityManager.subscriptionMonitor, shell, tokens);
                default:
                    return null;
            }
        }
    }
}
