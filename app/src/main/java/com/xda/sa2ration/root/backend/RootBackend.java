package com.xda.sa2ration.root.backend;

import com.xda.sa2ration.root.RootArchitecture;
import com.xda.sa2ration.root.RootImplementation;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;
import java.io.File;

public interface RootBackend {
    RootImplementation implementation();
    RootArchitecture architecture();
    boolean isFunctional();
    boolean supportsModules();
    File modulesDirectory();
    boolean supportsBootScripts();
    boolean supportsServiceScripts();
    String version();
    String limitations();
    ShellResult execute(ShellCommand command);
}
