package com.xda.sa2ration.profile;

import com.xda.sa2ration.domain.DisplayConfiguration;
import java.util.ArrayList;import java.util.List;import java.util.UUID;

public final class DisplayProfile {
    public static final int FORMAT_VERSION=1;
    public int formatVersion=FORMAT_VERSION;
    public String id=UUID.randomUUID().toString();
    public String name="Personalizado";
    public DisplayConfiguration configuration=DisplayConfiguration.neutral();
    public String panelTechnology="ANY";
    public String backend="AUTO";
    public String colorMode="AUTO";
    public Double physicalBrightness;
    public Float refreshRate;
    public List<String> automationIds=new ArrayList<>();
    public long createdAtEpochMs=System.currentTimeMillis();
    public boolean builtIn;

    public DisplayProfile copy(){DisplayProfile p=new DisplayProfile();p.formatVersion=formatVersion;p.id=id;p.name=name;p.configuration=configuration.copy();p.panelTechnology=panelTechnology;p.backend=backend;p.colorMode=colorMode;p.physicalBrightness=physicalBrightness;p.refreshRate=refreshRate;p.automationIds=new ArrayList<>(automationIds);p.createdAtEpochMs=createdAtEpochMs;p.builtIn=builtIn;return p;}
}
