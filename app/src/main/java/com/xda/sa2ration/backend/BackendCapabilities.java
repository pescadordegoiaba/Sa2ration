package com.xda.sa2ration.backend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BackendCapabilities {
    private final Map<String, CapabilityStatus> values = new LinkedHashMap<>();

    public BackendCapabilities set(String capability, CapabilityStatus status) {
        values.put(capability, status);
        return this;
    }

    public CapabilityStatus get(String capability) {
        return values.getOrDefault(capability, CapabilityStatus.UNKNOWN);
    }

    public boolean isSupported(String capability) {
        return get(capability) == CapabilityStatus.SUPPORTED;
    }

    public boolean supportsLinearColorMatrix(){return isSupported("linearColorMatrix");}
    public boolean supportsGlobalSaturation(){return isSupported("globalSaturation");}
    public boolean supportsGamma(){return isSupported("gamma");}
    public boolean supportsRgbGamma(){return isSupported("rgbGamma");}
    public boolean supportsOneDimensionalLut(){return isSupported("lut1d");}
    public boolean supportsThreeDimensionalLut(){return isSupported("lut3d");}
    public boolean supportsRefreshRateControl(){return isSupported("refreshRate");}
    public boolean supportsResolutionControl(){return isSupported("resolution");}
    public boolean supportsBrightnessControl(){return isSupported("brightness");}
    public boolean supportsHbm(){return isSupported("hbm");}
    public boolean supportsDcDimming(){return isSupported("dcDimming");}
    public boolean supportsColorModes(){return isSupported("colorModes");}
    public boolean supportsDaltonizer(){return isSupported("daltonizer");}
    public boolean supportsHdrControls(){return isSupported("hdrControls");}

    public Map<String, CapabilityStatus> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public static BackendCapabilities genericSurfaceFlinger() {
        return new BackendCapabilities()
                .set("linearColorMatrix", CapabilityStatus.UNTESTED)
                .set("globalSaturation", CapabilityStatus.UNTESTED)
                .set("colorModes", CapabilityStatus.UNTESTED)
                .set("daltonizer", CapabilityStatus.UNTESTED)
                .set("gamma", CapabilityStatus.REQUIRES_MODULE)
                .set("rgbGamma", CapabilityStatus.REQUIRES_MODULE)
                .set("lut1d", CapabilityStatus.REQUIRES_MODULE)
                .set("lut3d", CapabilityStatus.REQUIRES_MODULE)
                .set("refreshRate", CapabilityStatus.UNKNOWN)
                .set("resolution", CapabilityStatus.UNKNOWN)
                .set("brightness", CapabilityStatus.UNKNOWN)
                .set("hbm", CapabilityStatus.UNKNOWN)
                .set("dcDimming", CapabilityStatus.UNKNOWN)
                .set("hdrControls", CapabilityStatus.UNKNOWN);
    }
}
