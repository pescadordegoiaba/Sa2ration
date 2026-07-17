package com.xda.sa2ration.backend;

import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import com.xda.sa2ration.shell.ShellResult;

public interface DisplayBackend {
    String id();
    BackendCapabilities capabilities();
    ShellResult apply(DisplayConfiguration configuration, Matrix4 matrix);
    ShellResult resetToNeutral();
}
