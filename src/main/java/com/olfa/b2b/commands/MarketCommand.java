package com.olfa.b2b.commands;

import com.olfa.b2b.domain.Feed;
import com.olfa.b2b.events.Status;
import com.olfa.b2b.lp.SubscriptionMonitor;
import com.olfa.b2b.shell.Shell;

import java.util.Set;

public class MarketCommand implements Command {

    private final SubscriptionMonitor monitor;
    private final Shell shell;

    public MarketCommand(SubscriptionMonitor monitor, Shell shell, String[] tokens) {
        this.monitor = monitor;
        this.shell = shell;
    }

    @Override public void run() {
        Set<Feed> allFeeds = monitor.feeds;
        for (Feed feed : allFeeds) {
            Status<Feed> status = monitor.getStatus(feed);
            shell.log(status.toString());
        }
    }
}
