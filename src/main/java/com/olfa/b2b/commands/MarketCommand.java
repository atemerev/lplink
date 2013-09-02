package com.olfa.b2b.commands;

import com.olfa.b2b.domain.Subscription;
import com.olfa.b2b.lp.LpManager;
import com.olfa.b2b.shell.Shell;

import java.util.Set;

public class MarketCommand implements Command {

    private final LpManager monitor;
    private final Shell shell;

    public MarketCommand(LpManager monitor, Shell shell, String[] tokens) {
        this.monitor = monitor;
        this.shell = shell;
    }

    @Override public void run() {
        Set<Subscription> allSubscriptions = monitor.subscriptions;
        for (Subscription subscription : allSubscriptions) {
        }
    }
}
