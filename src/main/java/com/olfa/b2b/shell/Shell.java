package com.olfa.b2b.shell;

import com.olfa.b2b.commands.Command;
import com.olfa.b2b.commands.CommandParser;
import com.olfa.b2b.lp.SubscriptionMonitor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shell {

    private static final String PROMPT = "> ";

    public Shell() {
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void start() {
        final Config b2bConfig = ConfigFactory.load("lp/b2b.conf");
        // todo fix
        final SubscriptionMonitor liquidityManager = new SubscriptionMonitor(null, 0, 0);
        final CommandParser parser = new CommandParser(liquidityManager, this);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        log("Olfa LP Manager 2013-04-11-001 is online.\n");
        while (true) {
            try {
                System.out.print(PROMPT);
                String input = in.readLine();
                if (input != null) {
                    Command command = parser.parseCommand(input);
                    if (command != null) {
                        try {
                            command.run(); // Same thread for the moment.
                        } catch (Exception e) {
                            log(String.format("Couldn't execute command: %s", e.getMessage()));
                        }
                    } else {
                        log(String.format("Unrecognized command: %s", input));
                    }
                }
            } catch (IOException e) {
                log(String.format("I/O error: unable to read your input -- %s", e.getMessage()));
            }
        }
    }

    public void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) throws Exception {
        Shell shell = new Shell();
        shell.start();
    }
}
