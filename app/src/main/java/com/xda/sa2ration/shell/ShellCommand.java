package com.xda.sa2ration.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShellCommand {
    private final List<String> commands;
    private final long timeoutMs;
    private final boolean redactInLogs;

    private ShellCommand(Builder builder) {
        if (builder.commands.isEmpty()) throw new IllegalArgumentException("At least one command is required");
        this.commands = Collections.unmodifiableList(new ArrayList<>(builder.commands));
        this.timeoutMs = builder.timeoutMs;
        this.redactInLogs = builder.redactInLogs;
    }

    public List<String> commands() { return commands; }
    public long timeoutMs() { return timeoutMs; }
    public boolean redactInLogs() { return redactInLogs; }
    public String script() { return String.join("\n", commands); }
    public String logLabel() { return redactInLogs ? "<redacted>" : script(); }

    public static Builder builder(String command) { return new Builder(command); }

    public static final class Builder {
        private final List<String> commands = new ArrayList<>();
        private long timeoutMs = 10_000;
        private boolean redactInLogs;

        private Builder(String command) { add(command); }
        public Builder add(String command) {
            if (command == null || command.trim().isEmpty() || command.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("Invalid shell command");
            }
            commands.add(command);
            return this;
        }
        public Builder timeoutMs(long timeoutMs) {
            if (timeoutMs < 100 || timeoutMs > 120_000) throw new IllegalArgumentException("Invalid timeout");
            this.timeoutMs = timeoutMs;
            return this;
        }
        public Builder redactInLogs(boolean value) { this.redactInLogs = value; return this; }
        public ShellCommand build() { return new ShellCommand(this); }
    }
}
