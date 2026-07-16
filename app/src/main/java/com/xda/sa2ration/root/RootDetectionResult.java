package com.xda.sa2ration.root;

import com.xda.sa2ration.diagnostics.DetectionEvidence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RootDetectionResult {
    public RootImplementation implementation=RootImplementation.NONE;
    public RootArchitecture architecture=RootArchitecture.NONE;
    public boolean functionalRoot;
    public String versionName="";
    public Long versionCode;
    public int confidence;
    public int exitCode=-1;
    public String stdout="";
    public String stderr="";
    public boolean timedOut;
    public long durationMs;
    public String binaryPath="";
    private final List<DetectionEvidence> evidence=new ArrayList<>();
    private final List<String> warnings=new ArrayList<>();
    public void addEvidence(String source,String detail,int weight){evidence.add(new DetectionEvidence(source,detail,weight));}
    public void addWarning(String warning){warnings.add(warning);}
    public List<DetectionEvidence> evidence(){return Collections.unmodifiableList(evidence);}
    public List<String> warnings(){return Collections.unmodifiableList(warnings);}
}
