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
    private final SurfaceFlingerTransactions transactions = new SurfaceFlingerTransactionResolver().resolve();

    public SurfaceFlingerBackend(RootShellExecutor executor) {
        this.executor = executor;
    }

    @Override public String id() { return "surfaceflinger-legacy"; }
    @Override public BackendCapabilities capabilities() { return capabilities; }

    @Override
    public ShellResult apply(DisplayConfiguration configuration, Matrix4 matrix) {
        DisplayConfiguration safe=configuration==null?DisplayConfiguration.neutral():configuration.copy();
        safe.sanitize();
        boolean enabled = safe.masterEnabled;
        double saturation = enabled && safe.globalSaturationEnabled ? safe.globalSaturation : 1.0;
        Matrix4 effectiveMatrix = enabled ? matrix : Matrix4.identity();
        if(effectiveMatrix==null||!effectiveMatrix.isSurfaceFlingerAffine())effectiveMatrix=Matrix4.identity();
        int nativeMode = safe.colorManagementEnabled ? 0 : 1;
        String saturationValue = String.format(Locale.US, "%.8f", saturation);
        String apply="service call SurfaceFlinger "+transactions.globalSaturation+" f "+saturationValue
                +" && service call SurfaceFlinger "+transactions.colorMatrix+" i32 1"+effectiveMatrix.toSurfaceFlingerArguments()
                +" && service call SurfaceFlinger "+transactions.colorManagement+" i32 "+nativeMode
                +" && setprop "+SATURATION_PROPERTY+" "+saturationValue;
        String neutral="service call SurfaceFlinger "+transactions.globalSaturation+" f 1.0 >/dev/null 2>&1; "
                +"service call SurfaceFlinger "+transactions.colorMatrix+" i32 0 >/dev/null 2>&1; "
                +"service call SurfaceFlinger "+transactions.colorManagement+" i32 0 >/dev/null 2>&1; "
                +"setprop "+SATURATION_PROPERTY+" 1.0 >/dev/null 2>&1";
        ShellCommand command = ShellCommand.builder("if "+apply+"; then exit 0; fi")
                .add(neutral)
                .add("exit 1")
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
