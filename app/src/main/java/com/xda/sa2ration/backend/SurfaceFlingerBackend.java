package com.xda.sa2ration.backend;

import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;

import java.util.Locale;

public final class SurfaceFlingerBackend implements DisplayBackend {
    public static final String SATURATION_PROPERTY = "persist.sys.sf.color_saturation";
    public static final String NATIVE_MODE_PROPERTY = "persist.sys.sf.native_mode";
    private final RootShellExecutor executor;
    private final BackendCapabilities capabilities = BackendCapabilities.genericSurfaceFlinger();

    public SurfaceFlingerBackend(RootShellExecutor executor) {
        this.executor = executor;
    }

    @Override public String id() { return "surfaceflinger-legacy"; }
    @Override public BackendCapabilities capabilities() { return capabilities; }

    @Override
    public ShellResult apply(DisplayConfiguration configuration, Matrix4 matrix) {
        boolean enabled = configuration != null && configuration.masterEnabled;
        double saturation = enabled && configuration.globalSaturationEnabled
                ? configuration.globalSaturation : 1.0;
        Matrix4 effectiveMatrix = enabled ? matrix : Matrix4.identity();
        int nativeMode = configuration != null && configuration.colorManagementEnabled ? 0 : 1;
        String saturationValue = String.format(Locale.US, "%.8f", saturation);
        ShellCommand command = ShellCommand.builder("setprop " + SATURATION_PROPERTY + " " + saturationValue)
                .add("service call SurfaceFlinger 1022 f " + saturationValue)
                .add("service call SurfaceFlinger 1015 i32 1" + effectiveMatrix.toSurfaceFlingerArguments())
                .add("service call SurfaceFlinger 1023 i32 " + nativeMode)
                .timeoutMs(15_000)
                .build();
        return executor.executeBlocking(command);
    }

    @Override
    public ShellResult resetToNeutral() {
        DisplayConfiguration neutral = DisplayConfiguration.neutral();
        neutral.masterEnabled = false;
        return apply(neutral, Matrix4.identity());
    }
}
