package com.xda.sa2ration.manufacturer;
import com.xda.sa2ration.backend.BackendCapabilities;
public interface ManufacturerBackend {String id();boolean matches(String signals);BackendCapabilities capabilities();default boolean permitsHardwareWrites(){return false;}}
