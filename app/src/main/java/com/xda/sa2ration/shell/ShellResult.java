package com.xda.sa2ration.shell;

public final class ShellResult {
    public final int exitCode;
    public final String stdout;
    public final String stderr;
    public final boolean timedOut;
    public final long durationMs;
    public final String command;

    public ShellResult(int exitCode, String stdout, String stderr, boolean timedOut,
                       long durationMs, String command) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.timedOut = timedOut;
        this.durationMs = durationMs;
        this.command = command == null ? "" : command;
    }

    public boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }
}
