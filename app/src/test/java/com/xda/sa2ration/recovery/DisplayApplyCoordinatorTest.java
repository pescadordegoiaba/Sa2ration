package com.xda.sa2ration.recovery;

import com.xda.sa2ration.domain.DefaultTransformStages;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DisplayApplyCoordinatorTest {
    @Test public void neutralIsNotDangerous(){DisplayConfiguration c=DisplayConfiguration.neutral();assertFalse(DisplayApplyCoordinator.isDangerous(c,Matrix4.identity()));}
    @Test public void extremeContrastRequiresRollback(){DisplayConfiguration c=DisplayConfiguration.neutral();c.globalContrast=10;assertTrue(DisplayApplyCoordinator.isDangerous(c,DefaultTransformStages.createPipeline().compose(c)));}
    @Test public void inversionRequiresRollback(){DisplayConfiguration c=DisplayConfiguration.neutral();c.inversionEnabled=true;assertTrue(DisplayApplyCoordinator.isDangerous(c,DefaultTransformStages.createPipeline().compose(c)));}
    @Test public void disabledMasterDoesNotRequireRollback(){DisplayConfiguration c=DisplayConfiguration.neutral();c.masterEnabled=false;c.globalContrast=100;assertFalse(DisplayApplyCoordinator.isDangerous(c,Matrix4.identity()));}
    @Test public void disabledStageDoesNotRequireRollbackForPreservedExtremeValue(){DisplayConfiguration c=DisplayConfiguration.neutral();c.globalContrastEnabled=false;c.globalContrast=100;c.rgbGainEnabled=false;c.redGain=100;assertFalse(DisplayApplyCoordinator.isDangerous(c,Matrix4.identity()));}
    @Test public void extremeTemperatureRequiresRollbackOnlyWhenEnabled(){DisplayConfiguration c=DisplayConfiguration.neutral();c.temperatureKelvin=1000;assertFalse(DisplayApplyCoordinator.isDangerous(c,Matrix4.identity()));c.temperatureEnabled=true;assertTrue(DisplayApplyCoordinator.isDangerous(c,DefaultTransformStages.createPipeline().compose(c)));}
}
