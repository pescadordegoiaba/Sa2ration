package com.xda.sa2ration.lut;

import com.xda.sa2ration.backend.CapabilityStatus;

public final class CompanionModuleStatus {
    public boolean installed;
    public String version="";
    public String adapterId="none";
    public String diagnostic="";
    public CapabilityStatus gamma=CapabilityStatus.REQUIRES_MODULE;
    public CapabilityStatus rgbGamma=CapabilityStatus.REQUIRES_MODULE;
    public CapabilityStatus lut1d=CapabilityStatus.REQUIRES_MODULE;
    public CapabilityStatus lut3d=CapabilityStatus.REQUIRES_MODULE;
    public CapabilityStatus curves=CapabilityStatus.REQUIRES_MODULE;
    public boolean hasAdapter(){return installed&&adapterId!=null&&!adapterId.isEmpty()&&!"none".equals(adapterId);}
}
