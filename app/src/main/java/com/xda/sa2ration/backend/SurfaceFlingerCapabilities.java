package com.xda.sa2ration.backend;

import java.util.Locale;

import com.xda.sa2ration.shell.RootShellExecutor;import com.xda.sa2ration.shell.ShellCommand;import com.xda.sa2ration.shell.ShellResult;

public final class SurfaceFlingerCapabilities {
    public final boolean serviceAvailable;public final SurfaceFlingerTransactions transactions;public final BackendCapabilities capabilities;public final String diagnostic;
    private SurfaceFlingerCapabilities(boolean available,SurfaceFlingerTransactions transactions,BackendCapabilities capabilities,String diagnostic){this.serviceAvailable=available;this.transactions=transactions;this.capabilities=capabilities;this.diagnostic=diagnostic;}
    public static SurfaceFlingerCapabilities detect(RootShellExecutor executor){
        SurfaceFlingerTransactions tx=new SurfaceFlingerTransactionResolver().resolve();
        ShellResult check=executor.executeBlocking(ShellCommand.builder("service check SurfaceFlinger; dumpsys SurfaceFlinger --display-id 2>/dev/null | head -n 80").timeoutMs(8_000).build());
        boolean available=check.stdout.contains("found")||check.stdout.toLowerCase(Locale.ROOT).contains("display");
        BackendCapabilities c=BackendCapabilities.genericSurfaceFlinger();
        if(!available){c.set("linearColorMatrix",CapabilityStatus.FAILED).set("globalSaturation",CapabilityStatus.FAILED).set("colorModes",CapabilityStatus.FAILED);}
        else{c.set("linearColorMatrix",CapabilityStatus.UNTESTED).set("globalSaturation",CapabilityStatus.UNTESTED).set("colorModes",CapabilityStatus.UNTESTED).set("daltonizer",CapabilityStatus.UNTESTED);}
        return new SurfaceFlingerCapabilities(available,tx,c,check.stdout+check.stderr);
    }
}
