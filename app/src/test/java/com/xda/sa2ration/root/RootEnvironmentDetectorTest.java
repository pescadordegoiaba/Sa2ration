package com.xda.sa2ration.root;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RootEnvironmentDetectorTest {
    @Test public void nonFunctionalRootIsNone(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(false,"magisk");assertEquals(RootImplementation.NONE,r.implementation);assertFalse(r.functionalRoot);}
    @Test public void classifiesMagisk(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"Magisk 28.1 /data/adb/magisk");assertEquals(RootImplementation.MAGISK,r.implementation);assertEquals(RootArchitecture.USERSPACE_ROOT,r.architecture);}
    @Test public void classifiesKernelSu(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"KernelSU ksud /data/adb/ksu");assertEquals(RootImplementation.KERNELSU,r.implementation);assertEquals(RootArchitecture.KERNEL_ROOT,r.architecture);}
    @Test public void classifiesSukiSuBeforeKernelSu(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"SukiSU Ultra KernelSU compatible ksud");assertEquals(RootImplementation.SUKISU_ULTRA,r.implementation);}
    @Test public void classifiesReSukiSuBeforeSukiSu(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"ReSukiSU SukiSU KernelSU");assertEquals(RootImplementation.RESUKISU,r.implementation);}
    @Test public void classifiesAPatch(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"APatch apd /data/adb/ap");assertEquals(RootImplementation.APATCH,r.implementation);assertEquals(RootArchitecture.HYBRID,r.architecture);}
    @Test public void unknownSuRemainsFunctional(){RootDetectionResult r=RootEnvironmentDetector.classifyProbe(true,"custom super user");assertEquals(RootImplementation.UNKNOWN_SU,r.implementation);assertTrue(r.functionalRoot);}
}
