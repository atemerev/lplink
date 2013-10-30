package com.olfa.b2b.shell;

import com.olfa.b2b.commands.Command;
import com.olfa.b2b.commands.CommandParser;
import com.olfa.b2b.lp.LpManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shell {

    private static final String PROMPT = "> ";
    private final LpManager lpManager;

    public Shell(LpManager lpManager) {
        this.lpManager = lpManager;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void start() {
        final CommandParser parser = new CommandParser(lpManager, this);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        log("Olfa LP Manager 2013-10-30 is online.\n");
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
                Thread.sleep(100);
            } catch (IOException e) {
                log(String.format("I/O error: unable to read your input -- %s", e.getMessage()));
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    public void log(String message) {
        System.out.println(message);
    }
}
