package com.xda.sa2ration.root;

import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RootEnvironmentDetector {
    private final RootShellExecutor executor;
    public RootEnvironmentDetector(RootShellExecutor executor){this.executor=executor;}

    public RootDetectionResult detect(){
        RootDetectionResult result=new RootDetectionResult();
        ShellResult id=executor.executeBlocking(ShellCommand.builder("id -u").timeoutMs(7_000).build());
        result.exitCode=id.exitCode;result.stdout=id.stdout;result.stderr=id.stderr;result.timedOut=id.timedOut;result.durationMs=id.durationMs;
        result.functionalRoot=id.isSuccess()&&"0".equals(id.stdout.trim());
        if(!result.functionalRoot){result.implementation=RootImplementation.NONE;result.architecture=RootArchitecture.NONE;result.confidence=id.timedOut?35:80;result.addWarning(id.timedOut?"Teste su expirou":"id -u não retornou exatamente 0");return result;}
        result.addEvidence("id -u","UID 0 confirmado",45);
        String probe="echo __SU_PATH__; command -v su 2>/dev/null; echo __SU_V__; su -v 2>&1; echo __SU_CAPV__; su -V 2>&1; "
                +"echo __MAGISK__; command -v magisk 2>/dev/null; magisk -v 2>&1; magisk -V 2>&1; magisk --path 2>&1; "
                +"echo __KSU__; command -v ksud 2>/dev/null; ksud -V 2>&1; ksud --version 2>&1; "
                +"echo __APATCH__; command -v apd 2>/dev/null; apd --version 2>&1; "
                +"echo __PROPS__; getprop | grep -iE 'magisk|kernelsu|ksu|sukisu|resukisu|apatch' 2>/dev/null | head -n 80; "
                +"echo __FILES__; for p in /data/adb/magisk /data/adb/ksu /data/adb/ap /data/adb/modules /proc/sys/kernel/ksu; do [ -e \"$p\" ] && echo \"$p\"; done; "
                +"echo __KERNEL__; uname -a";
        ShellResult details=executor.executeBlocking(ShellCommand.builder(probe).timeoutMs(15_000).build());
        String text=(details.stdout+'\n'+details.stderr).toLowerCase(Locale.US);
        result.binaryPath=section(details.stdout,"__SU_PATH__","__SU_V__").trim();
        RootDetectionResult classification=classifyProbe(true,text);
        result.implementation=classification.implementation;result.architecture=classification.architecture;result.confidence=classification.confidence;
        for(com.xda.sa2ration.diagnostics.DetectionEvidence e:classification.evidence())result.addEvidence(e.source,e.detail,e.weight);
        String suVersion=section(details.stdout,"__SU_V__","__SU_CAPV__").trim();
        String implementationVersion=result.implementation==RootImplementation.MAGISK?section(details.stdout,"__MAGISK__","__KSU__"):suVersion;
        result.versionName=firstNonEmptyLine(implementationVersion);
        Matcher number=Pattern.compile("(?:version|v)?\\s*(\\d{3,})",Pattern.CASE_INSENSITIVE).matcher(implementationVersion);
        if(number.find())try{result.versionCode=Long.parseLong(number.group(1));}catch(NumberFormatException ignored){}
        if(!details.isSuccess())result.addWarning("Coleta complementar retornou exit "+details.exitCode);
        return result;
    }

    public static RootDetectionResult classifyProbe(boolean functionalRoot,String probeText){
        RootDetectionResult result=new RootDetectionResult();result.functionalRoot=functionalRoot;
        if(!functionalRoot){result.implementation=RootImplementation.NONE;result.architecture=RootArchitecture.NONE;result.confidence=80;return result;}
        String text=probeText==null?"":probeText.toLowerCase(Locale.US);
        if(text.contains("resukisu")){assignStatic(result,RootImplementation.RESUKISU,RootArchitecture.KERNEL_ROOT,94,"ReSukiSU identificado");}
        else if(text.contains("sukisu ultra")||text.contains("sukisu_ultra")||text.contains("sukisu")){assignStatic(result,RootImplementation.SUKISU_ULTRA,RootArchitecture.KERNEL_ROOT,94,"SukiSU identificado");}
        else if(text.contains("apatch")||text.contains("/data/adb/ap")||text.contains(" apd")){assignStatic(result,RootImplementation.APATCH,RootArchitecture.HYBRID,90,"APatch identificado");}
        else if(text.contains("magisk")){assignStatic(result,RootImplementation.MAGISK,RootArchitecture.USERSPACE_ROOT,92,"Magisk identificado");}
        else if(text.contains("kernelsu")||text.contains("/data/adb/ksu")||text.contains("ksud")){assignStatic(result,RootImplementation.KERNELSU,RootArchitecture.KERNEL_ROOT,88,"KernelSU identificado");}
        else assignStatic(result,RootImplementation.UNKNOWN_SU,RootArchitecture.UNKNOWN,55,"su funcional sem assinatura conhecida");
        return result;
    }

    private void assign(RootDetectionResult r,RootImplementation implementation,RootArchitecture architecture,int confidence,String evidence){r.implementation=implementation;r.architecture=architecture;r.confidence=confidence;r.addEvidence("probe root",evidence,confidence-45);}
    private static void assignStatic(RootDetectionResult r,RootImplementation implementation,RootArchitecture architecture,int confidence,String evidence){r.implementation=implementation;r.architecture=architecture;r.confidence=confidence;r.addEvidence("probe root",evidence,confidence-45);}
    private String section(String text,String start,String end){int from=text.indexOf(start);if(from<0)return"";from+=start.length();int to=text.indexOf(end,from);return to<0?text.substring(from):text.substring(from,to);}
    private String firstNonEmptyLine(String value){for(String line:value.split("\\R"))if(!line.trim().isEmpty())return line.trim();return"";}
}
