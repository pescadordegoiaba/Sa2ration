package com.xda.sa2ration.shell;

import android.util.Log;
import android.os.Build;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Serialized root executor. All blocking work is performed outside the main thread. */
public final class RootShellExecutor implements AutoCloseable {
    private static final String TAG = "Sa2rationRoot";
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "sa2ration-root"));
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> new Thread(r, "sa2ration-stream"));

    public Future<ShellResult> execute(ShellCommand command) {
        return commandExecutor.submit(() -> executeBlocking(command));
    }

    public ShellResult executeBlocking(ShellCommand command) {
        long started = System.nanoTime();
        Process process = null;
        boolean timedOut = false;
        int exitCode = -1;
        String stdout = "";
        String stderr = "";
        try {
            process = new ProcessBuilder("su", "-c", command.script()).start();
            Future<String> stdoutFuture = streamExecutor.submit(readStream(process.getInputStream()));
            Future<String> stderrFuture = streamExecutor.submit(readStream(process.getErrorStream()));
            if (!waitFor(process, command.timeoutMs())) {
                timedOut = true;
                process.destroy();
                if (!waitFor(process, 250) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) process.destroyForcibly();
            } else {
                exitCode = process.exitValue();
            }
            stdout = getQuietly(stdoutFuture);
            stderr = getQuietly(stderrFuture);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (process != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) process.destroyForcibly();
                else process.destroy();
            }
            stderr = "Interrupted";
        } catch (IOException error) {
            stderr = error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        ShellResult result = new ShellResult(exitCode, stdout, stderr, timedOut, durationMs,
                command.logLabel());
        Log.i(TAG, "command=" + result.command + " exit=" + result.exitCode
                + " timeout=" + result.timedOut + " durationMs=" + result.durationMs);
        return result;
    }

    private boolean waitFor(Process process, long timeoutMs) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline) {
            try { process.exitValue(); return true; }
            catch (IllegalThreadStateException running) { Thread.sleep(20); }
        }
        try { process.exitValue(); return true; }
        catch (IllegalThreadStateException running) { return false; }
    }

    private Callable<String> readStream(InputStream stream) {
        return () -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append('\n');
                    output.append(line);
                }
            }
            return output.toString();
        };
    }

    private String getQuietly(Future<String> future) {
        try { return future.get(1, TimeUnit.SECONDS); }
        catch (Exception ignored) { future.cancel(true); return ""; }
    }

    @Override
    public void close() {
        commandExecutor.shutdownNow();
        streamExecutor.shutdownNow();
    }
}
