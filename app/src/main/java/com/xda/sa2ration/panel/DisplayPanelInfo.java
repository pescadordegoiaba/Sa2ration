package com.xda.sa2ration.panel;

import com.xda.sa2ration.diagnostics.DetectionEvidence;
import java.util.ArrayList;import java.util.Collections;import java.util.List;

public final class DisplayPanelInfo {
    public PanelTechnology detectedTechnology=PanelTechnology.UNKNOWN;
    public PanelTechnology effectiveTechnology=PanelTechnology.UNKNOWN;
    public int confidence;
    public String panelName="";
    public String manufacturer="";
    public String probableBackend="generic-aosp";
    public boolean manuallyOverridden;
    public boolean manualConflict;
    public boolean hardwareSupportConfirmed;
    public boolean hardwareSupportInferred;
    private final List<DetectionEvidence> evidence=new ArrayList<>();
    private final List<String> consultedSources=new ArrayList<>();
    public void addEvidence(String source,String detail,int weight){evidence.add(new DetectionEvidence(source,detail,weight));}
    public void addSource(String source){consultedSources.add(source);}
    public List<DetectionEvidence> evidence(){return Collections.unmodifiableList(evidence);}
    public List<String> consultedSources(){return Collections.unmodifiableList(consultedSources);}
    public boolean isOledFamily(){return effectiveTechnology==PanelTechnology.OLED||effectiveTechnology==PanelTechnology.POLED||effectiveTechnology==PanelTechnology.AMOLED||effectiveTechnology==PanelTechnology.SUPER_AMOLED||effectiveTechnology==PanelTechnology.DYNAMIC_AMOLED||effectiveTechnology==PanelTechnology.LTPO_OLED;}
    public boolean isLcdFamily(){return effectiveTechnology==PanelTechnology.LCD||effectiveTechnology==PanelTechnology.TFT_LCD||effectiveTechnology==PanelTechnology.IPS_LCD||effectiveTechnology==PanelTechnology.PLS_LCD||effectiveTechnology==PanelTechnology.LTPS_LCD||effectiveTechnology==PanelTechnology.LTPO_LCD||effectiveTechnology==PanelTechnology.MINI_LED;}
}
