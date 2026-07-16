package com.xda.sa2ration.recovery;

import com.xda.sa2ration.backend.DisplayBackend;
import com.xda.sa2ration.domain.ColorTransformPipeline;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.shell.ShellResult;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Owns stable/current state and the 15-second dangerous-change rollback window. */
public final class DisplayApplyCoordinator implements AutoCloseable {
    public static final int ROLLBACK_SECONDS = 15;
    private final DisplayBackend backend;
    private final ColorTransformPipeline pipeline;
    private final ConfigurationRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "sa2ration-safety"));
    private ScheduledFuture<?> rollbackFuture;
    private ScheduledFuture<?> countdownFuture;
    private DisplayConfiguration pending;
    private Listener pendingListener;
    private int secondsRemaining;

    public interface Listener {
        void onApplied(ShellResult result, boolean needsConfirmation);
        void onCountdown(int secondsRemaining);
        void onReverted(ShellResult result, String reason);
    }

    public DisplayApplyCoordinator(DisplayBackend backend, ColorTransformPipeline pipeline,
                                   ConfigurationRepository repository) {
        this.backend = backend;
        this.pipeline = pipeline;
        this.repository = repository;
    }

    public synchronized void apply(DisplayConfiguration configuration, boolean useSafety, Listener listener) {
        cancelTimers();
        DisplayConfiguration candidate = configuration.copy();
        Matrix4 matrix;
        try { matrix = pipeline.compose(candidate); }
        catch (RuntimeException error) {
            listener.onApplied(new ShellResult(-1, "", error.getMessage(), false, 0, "matrix-validation"), false);
            return;
        }
        ShellResult result = backend.apply(candidate, matrix);
        repository.saveCurrent(candidate);
        boolean temporary = result.isSuccess() && useSafety && isDangerous(candidate, matrix);
        if (temporary) {
            pending = candidate;
            pendingListener = listener;
            secondsRemaining = ROLLBACK_SECONDS;
            listener.onApplied(result, true);
            listener.onCountdown(secondsRemaining);
            countdownFuture = scheduler.scheduleWithFixedDelay(() -> tick(listener), 1, 1, TimeUnit.SECONDS);
            rollbackFuture = scheduler.schedule(() -> revert("Tempo de confirmação esgotado"),
                    ROLLBACK_SECONDS, TimeUnit.SECONDS);
        } else {
            if (result.isSuccess()) repository.confirmStable(candidate);
            listener.onApplied(result, false);
        }
    }

    public synchronized boolean confirm() {
        if (pending == null) return false;
        repository.confirmStable(pending);
        clearPending();
        return true;
    }

    public synchronized boolean revert(String reason) {
        if (pending == null) return false;
        Listener listener = pendingListener;
        DisplayConfiguration stable = repository.load().stable.copy();
        ShellResult result = backend.apply(stable, pipeline.compose(stable));
        repository.saveCurrent(stable);
        clearPending();
        if (listener != null) listener.onReverted(result, reason);
        return true;
    }

    public synchronized ShellResult resetAtomically() {
        cancelTimers();
        DisplayConfiguration neutral = DisplayConfiguration.neutral();
        ShellResult result = backend.apply(neutral, Matrix4.identity());
        if (result.isSuccess()) repository.confirmStable(neutral);
        return result;
    }

    public ShellResult preview(DisplayConfiguration configuration) {
        return backend.apply(configuration, pipeline.compose(configuration));
    }

    public static boolean isDangerous(DisplayConfiguration c, Matrix4 matrix) {
        if (!c.masterEnabled) return false;
        if (Math.abs(c.globalSaturation) > 3 || Math.abs(c.globalContrast) > 3
                || Math.abs(c.digitalBrightnessGain) > 2 || Math.abs(c.digitalBrightnessOffset) > .35
                || Math.abs(c.blackLevel) > .35 || Math.abs(c.whiteLevel) > 2
                || Math.abs(c.redGain) > 3 || Math.abs(c.greenGain) > 3 || Math.abs(c.blueGain) > 3
                || Math.abs(c.redOffset) > .5 || Math.abs(c.greenOffset) > .5 || Math.abs(c.blueOffset) > .5
                || c.inversionEnabled || c.customMatrixEnabled) return true;
        for (double value : matrix.toArray()) if (Math.abs(value) > 4) return true;
        return false;
    }

    private synchronized void tick(Listener listener) {
        secondsRemaining--;
        if (secondsRemaining > 0 && pending != null) listener.onCountdown(secondsRemaining);
    }

    private void clearPending() {
        cancelTimers();
        pending = null;
        pendingListener = null;
    }

    private void cancelTimers() {
        if (rollbackFuture != null) rollbackFuture.cancel(false);
        if (countdownFuture != null) countdownFuture.cancel(false);
        rollbackFuture = null;
        countdownFuture = null;
    }

    @Override public synchronized void close() {
        cancelTimers();
        scheduler.shutdownNow();
    }
}
