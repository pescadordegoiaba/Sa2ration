package com.xda.sa2ration.root.backend;
import com.xda.sa2ration.root.RootDetectionResult;
import com.xda.sa2ration.shell.RootShellExecutor;
import java.io.File;
public class ModuleRootBackend extends GenericSuBackend {
    public ModuleRootBackend(RootShellExecutor executor,RootDetectionResult detection){super(executor,detection);}
    @Override public boolean supportsModules(){return true;}@Override public File modulesDirectory(){return new File("/data/adb/modules");}
    @Override public boolean supportsBootScripts(){return true;}@Override public boolean supportsServiceScripts(){return true;}
    @Override public String limitations(){return"Diretório e scripts devem ser confirmados antes de qualquer instalação.";}
}
