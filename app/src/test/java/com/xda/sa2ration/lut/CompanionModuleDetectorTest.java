package com.xda.sa2ration.lut;

import com.xda.sa2ration.backend.CapabilityStatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CompanionModuleDetectorTest {
    @Test public void parsesPassiveModuleWithEnabledAdapter() {
        CompanionModuleStatus status=CompanionModuleDetector.parse(
                "module.installed=true\nmodule.version=1.1.0\nmodule.passive=true\n"
                        +"module.bootService=false\nadapter.enabled=true\nadapter.id=test-oled\n"
                        +"gamma=supported\nlut1d=experimental\nlut3d=unsupported\n");
        assertTrue(status.installed);
        assertTrue(status.passive);
        assertFalse(status.bootService);
        assertTrue(status.hasAdapter());
        assertEquals(CapabilityStatus.SUPPORTED,status.gamma);
        assertEquals(CapabilityStatus.EXPERIMENTAL,status.lut1d);
        assertEquals(CapabilityStatus.UNSUPPORTED,status.lut3d);
    }

    @Test public void installedModuleWithoutEnabledAdapterRequiresAdapter() {
        CompanionModuleStatus status=CompanionModuleDetector.parse(
                "module.installed=true\nmodule.version=1.1.0\nmodule.passive=true\n"
                        +"module.bootService=false\nadapter.enabled=false\nadapter.id=none\n");
        assertTrue(status.installed);
        assertFalse(status.hasAdapter());
        assertEquals(CapabilityStatus.REQUIRES_ADAPTER,status.gamma);
    }

    @Test public void missingModuleRequiresModuleAndAdapter() {
        CompanionModuleStatus status=CompanionModuleDetector.parse("");
        assertFalse(status.installed);
        assertEquals(CapabilityStatus.REQUIRES_MODULE,status.gamma);
    }
}
