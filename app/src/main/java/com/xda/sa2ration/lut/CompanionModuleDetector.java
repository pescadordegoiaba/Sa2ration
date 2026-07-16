package com.xda.sa2ration.lut;

import com.xda.sa2ration.backend.CapabilityStatus;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CompanionModuleDetector {
    public static final String MODULE_PATH="/data/adb/modules/sa2ration_companion";
    public static final String CLI=MODULE_PATH+"/bin/sa2rationctl";
    private final RootShellExecutor executor;
    public CompanionModuleDetector(RootShellExecutor executor){this.executor=executor;}

    public CompanionModuleStatus detect(){
        ShellResult result=executor.executeBlocking(ShellCommand.builder(
                "if [ -x "+CLI+" ]; then "+CLI+" probe; else exit 20; fi").timeoutMs(8_000).build());
        CompanionModuleStatus status=parse(result.stdout);
        status.diagnostic=result.stderr;
        if(!result.isSuccess())status.installed=false;
        return status;
    }

    public static CompanionModuleStatus parse(String output){
        CompanionModuleStatus status=new CompanionModuleStatus();Map<String,String>values=new LinkedHashMap<>();
        if(output!=null)for(String line:output.split("\\R")){int split=line.indexOf('=');if(split>0)values.put(line.substring(0,split).trim(),line.substring(split+1).trim());}
        status.installed="true".equals(values.get("module.installed"));status.version=values.getOrDefault("module.version","");status.adapterId=values.getOrDefault("adapter.id","none");
        status.gamma=map(values.get("gamma"),status.installed);status.rgbGamma=map(values.get("rgbGamma"),status.installed);status.lut1d=map(values.get("lut1d"),status.installed);status.lut3d=map(values.get("lut3d"),status.installed);status.curves=map(values.get("curves"),status.installed);return status;
    }

    private static CapabilityStatus map(String value,boolean installed){if(!installed)return CapabilityStatus.REQUIRES_MODULE;if("supported".equals(value))return CapabilityStatus.SUPPORTED;if("experimental".equals(value))return CapabilityStatus.EXPERIMENTAL;return CapabilityStatus.UNSUPPORTED;}
}
