package com.xda.sa2ration.root.backend;

import com.xda.sa2ration.root.RootArchitecture;
import com.xda.sa2ration.root.RootDetectionResult;
import com.xda.sa2ration.root.RootImplementation;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;
import java.io.File;

public class GenericSuBackend implements RootBackend {
    protected final RootShellExecutor executor;protected final RootDetectionResult detection;
    public GenericSuBackend(RootShellExecutor executor,RootDetectionResult detection){this.executor=executor;this.detection=detection;}
    @Override public RootImplementation implementation(){return detection.implementation;}
    @Override public RootArchitecture architecture(){return detection.architecture;}
    @Override public boolean isFunctional(){return detection.functionalRoot;}
    @Override public boolean supportsModules(){return false;}
    @Override public File modulesDirectory(){return null;}
    @Override public boolean supportsBootScripts(){return false;}
    @Override public boolean supportsServiceScripts(){return false;}
    @Override public String version(){return detection.versionName;}
    @Override public String limitations(){return"Executor su genérico; recursos de módulo não confirmados.";}
    @Override public ShellResult execute(ShellCommand command){return executor.executeBlocking(command);}
}
